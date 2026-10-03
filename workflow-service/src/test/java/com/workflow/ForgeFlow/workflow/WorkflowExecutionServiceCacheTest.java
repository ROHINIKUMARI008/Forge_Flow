package com.workflow.ForgeFlow.workflow;

import com.workflow.ForgeFlow.workflow.api.ExecuteWorkflowRequest;
import com.workflow.ForgeFlow.workflow.messaging.WorkflowJobProducer;
import com.workflow.ForgeFlow.workflow.persistence.JobRepository;
import com.workflow.ForgeFlow.workflow.persistence.WorkflowEntity;
import com.workflow.ForgeFlow.workflow.persistence.WorkflowRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowExecutionServiceCacheTest {

    @Test
    void cacheHitSkipsTheWorkflowSelect() {
        WorkflowNameCache cache = mock(WorkflowNameCache.class);
        WorkflowRepository workflowRepository = mock(WorkflowRepository.class);
        when(cache.findId("welcome-email")).thenReturn(Optional.of(5L));

        WorkflowExecutionService service = new WorkflowExecutionService(
                mock(WorkflowJobProducer.class),
                workflowRepository,
                mock(JobRepository.class),
                cache);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.enqueue(new ExecuteWorkflowRequest("welcome-email", "user=42"));
            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(workflowRepository, never()).findByName(any());
        verify(cache, never()).remember(any(), any());
    }

    @Test
    void cacheMissStoresTheIdAfterCommit() throws Exception {
        WorkflowNameCache cache = mock(WorkflowNameCache.class);
        WorkflowRepository workflowRepository = mock(WorkflowRepository.class);
        when(cache.findId("welcome-email")).thenReturn(Optional.empty());
        WorkflowEntity entity = new WorkflowEntity("welcome-email", Instant.parse("2026-10-03T00:00:00Z"));
        setId(entity, 5L);
        when(workflowRepository.findByName("welcome-email")).thenReturn(Optional.of(entity));

        WorkflowExecutionService service = new WorkflowExecutionService(
                mock(WorkflowJobProducer.class),
                workflowRepository,
                mock(JobRepository.class),
                cache);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.enqueue(new ExecuteWorkflowRequest("welcome-email", "user=42"));
            verify(cache, never()).remember(any(), any());
            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(workflowRepository).findByName("welcome-email");
        verify(cache).remember("welcome-email", 5L);
    }

    private static void setId(WorkflowEntity entity, Long id) throws Exception {
        Field field = WorkflowEntity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }
}
