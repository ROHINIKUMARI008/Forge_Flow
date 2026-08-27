package com.workflow.ForgeFlow.workflow.persistence;

import com.workflow.ForgeFlow.workflow.JobRecord;
import com.workflow.ForgeFlow.workflow.JobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "job")
public class JobEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "workflow_id", nullable = false)
    private Long workflowId;

    @Column(name = "workflow_name", nullable = false)
    private String workflowName;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobStatus status;

    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(name = "enqueued_at", nullable = false)
    private Instant enqueuedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    protected JobEntity() {
    }

    public JobEntity(
            String id,
            Long workflowId,
            String workflowName,
            String payload,
            JobStatus status,
            Instant enqueuedAt) {
        this.id = id;
        this.workflowId = workflowId;
        this.workflowName = workflowName;
        this.payload = payload;
        this.status = status;
        this.enqueuedAt = enqueuedAt;
        this.updatedAt = enqueuedAt;
        this.attemptCount = 0;
    }

    public String getId() {
        return id;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public JobStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public boolean alreadyCompleted() {
        return status == JobStatus.COMPLETED;
    }

    public boolean exhaustedAttempts(int maxAttempts) {
        return status == JobStatus.FAILED && attemptCount >= maxAttempts;
    }

    public void registerFailedAttempt(String message) {
        this.attemptCount = this.attemptCount + 1;
        this.error = message;
        this.updatedAt = Instant.now();
    }

    public void markRunning() {
        this.status = JobStatus.RUNNING;
        this.error = null;
        this.updatedAt = Instant.now();
    }

    public void markCompleted() {
        this.status = JobStatus.COMPLETED;
        this.error = null;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String message) {
        this.status = JobStatus.FAILED;
        this.error = message;
        this.updatedAt = Instant.now();
    }

    public JobRecord toRecord() {
        return new JobRecord(id, workflowName, payload, status, enqueuedAt, updatedAt, error, attemptCount);
    }
}
