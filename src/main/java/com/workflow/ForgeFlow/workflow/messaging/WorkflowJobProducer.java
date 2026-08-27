package com.workflow.ForgeFlow.workflow.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WorkflowJobProducer {

    private static final Logger log = LoggerFactory.getLogger(WorkflowJobProducer.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public WorkflowJobProducer(
            RabbitTemplate rabbitTemplate,
            @Value("${forgeflow.rabbit.exchange}") String exchange,
            @Value("${forgeflow.rabbit.routing-key}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void publish(WorkflowJobMessage message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        log.info("Published job {} to exchange {} key {}", message.jobId(), exchange, routingKey);
    }
}
