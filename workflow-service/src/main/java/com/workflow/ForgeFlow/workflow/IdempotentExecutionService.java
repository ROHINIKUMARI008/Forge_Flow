package com.workflow.ForgeFlow.workflow;

import com.workflow.ForgeFlow.workflow.api.ExecuteWorkflowRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class IdempotentExecutionService {

    static final String PENDING = "PENDING";

    private final StringRedisTemplate redis;
    private final WorkflowExecutionService workflowExecutionService;
    private final Duration ttl;

    public IdempotentExecutionService(
            StringRedisTemplate redis,
            WorkflowExecutionService workflowExecutionService,
            @Value("${forgeflow.idempotency.ttl-hours:24}") long ttlHours) {
        this.redis = redis;
        this.workflowExecutionService = workflowExecutionService;
        this.ttl = Duration.ofHours(ttlHours);
    }

    public ExecutionResult execute(ExecuteWorkflowRequest request, String idempotencyKey) {
        String key = normalize(idempotencyKey);
        if (key == null) {
            return new ExecutionResult(workflowExecutionService.enqueue(request), false);
        }

        String redisKey = "forgeflow:idempotency:" + key;
        String existing = redis.opsForValue().get(redisKey);
        if (isJobId(existing)) {
            return replay(existing);
        }

        Boolean claimed = redis.opsForValue().setIfAbsent(redisKey, PENDING, Duration.ofMinutes(2));
        if (!Boolean.TRUE.equals(claimed)) {
            existing = redis.opsForValue().get(redisKey);
            if (isJobId(existing)) {
                return replay(existing);
            }
            throw new IllegalStateException("Duplicate execute is still being saved. Retry this Idempotency-Key.");
        }

        try {
            JobRecord job = workflowExecutionService.enqueue(request);
            redis.opsForValue().set(redisKey, job.jobId(), ttl);
            return new ExecutionResult(job, false);
        } catch (RuntimeException ex) {
            redis.delete(redisKey);
            throw ex;
        }
    }

    private ExecutionResult replay(String jobId) {
        JobRecord job = workflowExecutionService.getJob(jobId)
                .orElseThrow(() -> new IllegalStateException("Idempotency key points at a missing job: " + jobId));
        return new ExecutionResult(job, true);
    }

    private static boolean isJobId(String value) {
        return value != null && !value.isBlank() && !PENDING.equals(value);
    }

    private static String normalize(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        String key = idempotencyKey.trim();
        return key.isEmpty() ? null : key;
    }
}
