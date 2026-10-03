package com.workflow.ForgeFlow.workflow;

import com.workflow.ForgeFlow.workflow.api.ExecuteWorkflowRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdempotentExecutionServiceTest {

    @Test
    void sameKeyReturnsTheOriginalJob() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("forgeflow:idempotency:welcome-42")).thenReturn("job-1");

        WorkflowExecutionService executionService = mock(WorkflowExecutionService.class);
        JobRecord existing = new JobRecord(
                "job-1", "welcome-email", "user=42", JobStatus.QUEUED,
                Instant.parse("2026-10-03T00:00:00Z"), Instant.parse("2026-10-03T00:00:00Z"), null, 0);
        when(executionService.getJob("job-1")).thenReturn(Optional.of(existing));

        IdempotentExecutionService service = new IdempotentExecutionService(redis, executionService, 24);

        ExecutionResult result = service.execute(new ExecuteWorkflowRequest("welcome-email", "user=42"), "welcome-42");

        assertTrue(result.replayed());
        assertEquals("job-1", result.job().jobId());
        verify(executionService, never()).enqueue(any());
    }

    @Test
    void firstKeyStoresTheNewJobId() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("forgeflow:idempotency:welcome-42")).thenReturn(null);
        when(values.setIfAbsent(eq("forgeflow:idempotency:welcome-42"), eq("PENDING"), any(Duration.class)))
                .thenReturn(true);

        WorkflowExecutionService executionService = mock(WorkflowExecutionService.class);
        JobRecord created = new JobRecord(
                "job-2", "welcome-email", "user=42", JobStatus.QUEUED,
                Instant.parse("2026-10-03T00:00:00Z"), Instant.parse("2026-10-03T00:00:00Z"), null, 0);
        when(executionService.enqueue(any())).thenReturn(created);

        IdempotentExecutionService service = new IdempotentExecutionService(redis, executionService, 24);

        ExecutionResult result = service.execute(new ExecuteWorkflowRequest("welcome-email", "user=42"), "welcome-42");

        assertFalse(result.replayed());
        assertEquals("job-2", result.job().jobId());
        verify(values).set(eq("forgeflow:idempotency:welcome-42"), eq("job-2"), eq(Duration.ofHours(24)));
    }
}
