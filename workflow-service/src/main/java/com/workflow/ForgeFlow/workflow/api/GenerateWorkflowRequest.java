package com.workflow.ForgeFlow.workflow.api;

import jakarta.validation.constraints.NotBlank;

public record GenerateWorkflowRequest(@NotBlank String prompt) {
}
