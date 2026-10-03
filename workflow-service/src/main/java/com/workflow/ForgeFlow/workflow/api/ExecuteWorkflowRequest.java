package com.workflow.ForgeFlow.workflow.api;

import jakarta.validation.constraints.NotBlank;

public record ExecuteWorkflowRequest(
        @NotBlank String workflowName,
        String payload
) {
}
