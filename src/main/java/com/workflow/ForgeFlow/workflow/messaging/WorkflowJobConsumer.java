package com.workflow.ForgeFlow.workflow.messaging;

import com.workflow.ForgeFlow.workflow.JobRetryRecorder;
import com.workflow.ForgeFlow.workflow.persistence.JobEntity;
import com.workflow.ForgeFlow.workflow.persistence.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WorkflowJobConsumer {

    private static final Logger log = LoggerFactory.getLogger(WorkflowJobConsumer.class);

    private final JobRepository jobRepository;
    private final JobRetryRecorder jobRetryRecorder;
    private final long processingMs;
    private final int maxAttempts;

    public WorkflowJobConsumer(
            JobRepository jobRepository,
            JobRetryRecorder jobRetryRecorder,
            @Value("${forgeflow.worker.processing-ms}") long processingMs,
            @Value("${forgeflow.worker.max-attempts}") int maxAttempts) {
        this.jobRepository = jobRepository;
        this.jobRetryRecorder = jobRetryRecorder;
        this.processingMs = processingMs;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    @RabbitListener(queues = "${forgeflow.rabbit.queue}")
    public void handle(WorkflowJobMessage message) {
        log.info("Worker picked up job {} ({})", message.jobId(), message.workflowName());
        JobEntity job = jobRepository.findById(message.jobId())
                .orElseThrow(() -> new IllegalStateException("Job not found: " + message.jobId()));

        if (job.alreadyCompleted()) {
            log.info("Skipping job {} — already COMPLETED (idempotent)", job.getId());
            return;
        }
        if (job.exhaustedAttempts(maxAttempts)) {
            log.info("Skipping job {} — already FAILED after {} attempts", job.getId(), job.getAttemptCount());
            return;
        }

        job.markRunning();
        jobRepository.save(job);

        try {
            if (message.payload() != null && message.payload().contains("FORCE_FAIL")) {
                throw new IllegalStateException("Forced failure for retry demo");
            }
            Thread.sleep(processingMs);
            job.markCompleted();
            jobRepository.save(job);
            log.info("Worker finished job {}", message.jobId());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            retryOrFail(job.getId(), "Worker interrupted");
        } catch (RuntimeException ex) {
            retryOrFail(job.getId(), ex.getMessage() == null ? "Worker error" : ex.getMessage());
        }
    }

    private void retryOrFail(String jobId, String error) {
        boolean shouldRequeue = jobRetryRecorder.recordFailureAndShouldRequeue(jobId, error, maxAttempts);
        if (shouldRequeue) {
            log.warn("Job {} failed (will retry): {}", jobId, error);
            throw new IllegalStateException("Retry job " + jobId + ": " + error);
        }
        log.error("Job {} FAILED after {} attempts: {}", jobId, maxAttempts, error);
    }
}
