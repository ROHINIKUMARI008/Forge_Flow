package com.workflow.ForgeFlow.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class WorkflowNameCache {

    private static final Logger log = LoggerFactory.getLogger(WorkflowNameCache.class);

    private final StringRedisTemplate redis;
    private final Duration ttl;

    public WorkflowNameCache(
            StringRedisTemplate redis,
            @Value("${forgeflow.cache.workflow-ttl:10m}") Duration ttl) {
        this.redis = redis;
        this.ttl = ttl;
    }

    public Optional<Long> findId(String workflowName) {
        String cached = redis.opsForValue().get(key(workflowName));
        if (cached == null || cached.isBlank()) {
            log.info("Workflow cache miss for {}", workflowName);
            return Optional.empty();
        }
        try {
            log.info("Workflow cache hit for {} -> {}", workflowName, cached);
            return Optional.of(Long.valueOf(cached));
        } catch (NumberFormatException ex) {
            forget(workflowName);
            log.info("Workflow cache miss for {} (bad cached value)", workflowName);
            return Optional.empty();
        }
    }

    public void remember(String workflowName, Long workflowId) {
        redis.opsForValue().set(key(workflowName), workflowId.toString(), ttl);
        log.info("Workflow cache stored {} -> {} for {}", workflowName, workflowId, ttl);
    }

    public void forget(String workflowName) {
        redis.delete(key(workflowName));
    }

    static String key(String workflowName) {
        return "forgeflow:workflow:name:" + workflowName;
    }
}
