package com.workflow.ForgeFlow.workflow.api;

import com.workflow.ForgeFlow.workflow.JobStatus;

public record ExecuteWorkflowResponse(
        String jobId,
        JobStatus status,
        String message,
        boolean replayed
) {
}
