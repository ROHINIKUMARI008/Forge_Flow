package com.workflow.ForgeFlow.workflow.messaging;

import java.time.Instant;

public record WorkflowJobMessage(
        String jobId,
        String workflowName,
        String payload,
        Instant enqueuedAt
) {
}
