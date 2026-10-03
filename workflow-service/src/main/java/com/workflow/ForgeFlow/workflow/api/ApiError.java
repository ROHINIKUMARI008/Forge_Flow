package com.workflow.ForgeFlow.workflow.api;

public record ApiError(
        int status,
        String error,
        String message,
        String path
) {
}
