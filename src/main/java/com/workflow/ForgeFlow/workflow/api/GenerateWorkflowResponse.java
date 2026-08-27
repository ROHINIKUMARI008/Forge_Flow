package com.workflow.ForgeFlow.workflow.api;

import com.workflow.ForgeFlow.workflow.JobStatus;

public record GenerateWorkflowResponse(
        String jobId,
        JobStatus status,
        String workflowName,
        String payload,
        String message
) {
}
