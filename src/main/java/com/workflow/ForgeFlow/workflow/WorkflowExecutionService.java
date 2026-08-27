package com.workflow.ForgeFlow.workflow;

import com.workflow.ForgeFlow.workflow.api.ExecuteWorkflowRequest;
import com.workflow.ForgeFlow.workflow.messaging.WorkflowJobMessage;
import com.workflow.ForgeFlow.workflow.messaging.WorkflowJobProducer;
import com.workflow.ForgeFlow.workflow.persistence.JobEntity;
import com.workflow.ForgeFlow.workflow.persistence.JobRepository;
import com.workflow.ForgeFlow.workflow.persistence.WorkflowEntity;
import com.workflow.ForgeFlow.workflow.persistence.WorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class WorkflowExecutionService {

    private final WorkflowJobProducer producer;
    private final WorkflowRepository workflowRepository;
    private final JobRepository jobRepository;

    public WorkflowExecutionService(
            WorkflowJobProducer producer,
            WorkflowRepository workflowRepository,
            JobRepository jobRepository) {
        this.producer = producer;
        this.workflowRepository = workflowRepository;
        this.jobRepository = jobRepository;
    }

    @Transactional
    public JobRecord enqueue(ExecuteWorkflowRequest request) {
        Instant now = Instant.now();
        String payload = request.payload() == null ? "" : request.payload();
        String workflowName = request.workflowName();

        WorkflowEntity workflow = workflowRepository.findByName(workflowName)
                .orElseGet(() -> workflowRepository.saveAndFlush(new WorkflowEntity(workflowName, now)));

        JobEntity job = new JobEntity(
                UUID.randomUUID().toString(),
                workflow.getId(),
                workflow.getName(),
                payload,
                JobStatus.QUEUED,
                now
        );
        jobRepository.saveAndFlush(job);

        String jobId = job.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                producer.publish(new WorkflowJobMessage(jobId, workflowName, payload, now));
            }
        });

        return job.toRecord();
    }

    @Transactional(readOnly = true)
    public Optional<JobRecord> getJob(String jobId) {
        return jobRepository.findById(jobId).map(entity -> entity.toRecord());
    }
}
