package com.workflow.ForgeFlow.workflow;

import java.time.Instant;

public record JobRecord(
        String jobId,
        String workflowName,
        String payload,
        JobStatus status,
        Instant enqueuedAt,
        Instant updatedAt,
        String error,
        int attemptCount
) {
    public JobRecord withStatus(JobStatus next) {
        return new JobRecord(jobId, workflowName, payload, next, enqueuedAt, Instant.now(), null, attemptCount);
    }

    public JobRecord failed(String message) {
        return new JobRecord(jobId, workflowName, payload, JobStatus.FAILED, enqueuedAt, Instant.now(), message, attemptCount);
    }
}
