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
    private final WorkflowNameCache workflowNameCache;

    public WorkflowExecutionService(
            WorkflowJobProducer producer,
            WorkflowRepository workflowRepository,
            JobRepository jobRepository,
            WorkflowNameCache workflowNameCache) {
        this.producer = producer;
        this.workflowRepository = workflowRepository;
        this.jobRepository = jobRepository;
        this.workflowNameCache = workflowNameCache;
    }

    @Transactional
    public JobRecord enqueue(ExecuteWorkflowRequest request) {
        Instant now = Instant.now();
        String payload = request.payload() == null ? "" : request.payload();
        String workflowName = request.workflowName();

        ResolvedWorkflow resolved = resolveWorkflow(workflowName, now);
        if (resolved.loadedFromDatabase()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    workflowNameCache.remember(workflowName, resolved.workflowId());
                }
            });
        }

        JobEntity job = new JobEntity(
                UUID.randomUUID().toString(),
                resolved.workflowId(),
                resolved.workflowName(),
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

    private ResolvedWorkflow resolveWorkflow(String workflowName, Instant now) {
        Optional<Long> cachedId = workflowNameCache.findId(workflowName);
        if (cachedId.isPresent()) {
            return new ResolvedWorkflow(cachedId.get(), workflowName, false);
        }
        WorkflowEntity loaded = loadOrCreate(workflowName, now);
        return new ResolvedWorkflow(loaded.getId(), loaded.getName(), true);
    }

    private record ResolvedWorkflow(Long workflowId, String workflowName, boolean loadedFromDatabase) {
    }

    private WorkflowEntity loadOrCreate(String workflowName, Instant now) {
        workflowNameCache.forget(workflowName);
        return workflowRepository.findByName(workflowName)
                .orElseGet(() -> workflowRepository.saveAndFlush(new WorkflowEntity(workflowName, now)));
    }
}
