package com.workflow.ForgeFlow.workflow.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WorkflowJobProducerTest {

    @Test
    void publishSendsJsonMessageToExchange() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        WorkflowJobProducer producer = new WorkflowJobProducer(
                rabbitTemplate,
                "workflow.exchange",
                "workflow.job"
        );
        WorkflowJobMessage message = new WorkflowJobMessage("job-1", "welcome-email", "user=42", Instant.parse("2026-08-25T00:00:00Z"));

        producer.publish(message);

        verify(rabbitTemplate).convertAndSend("workflow.exchange", "workflow.job", message);
    }
}
