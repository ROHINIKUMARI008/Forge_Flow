package com.workflow.ForgeFlow.workflow;

public record ExecutionResult(JobRecord job, boolean replayed) {
}
