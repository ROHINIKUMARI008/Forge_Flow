package com.workflow.ForgeFlow.workflow;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowNameCacheTest {

    @Test
    void hitReturnsTheStoredId() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("forgeflow:workflow:name:welcome-email")).thenReturn("5");

        WorkflowNameCache cache = new WorkflowNameCache(redis, Duration.ofMinutes(10));

        assertEquals(Optional.of(5L), cache.findId("welcome-email"));
    }

    @Test
    void rememberWritesTheIdWithTtl() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);

        WorkflowNameCache cache = new WorkflowNameCache(redis, Duration.ofMinutes(10));
        cache.remember("welcome-email", 5L);

        verify(values).set("forgeflow:workflow:name:welcome-email", "5", Duration.ofMinutes(10));
        assertTrue(cache.findId("missing").isEmpty());
    }
}
