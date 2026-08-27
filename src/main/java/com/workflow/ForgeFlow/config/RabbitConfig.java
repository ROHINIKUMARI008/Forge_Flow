package com.workflow.ForgeFlow.config;

import com.workflow.ForgeFlow.workflow.messaging.WorkflowJobMessage;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange workflowExchange(@Value("${forgeflow.rabbit.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue workflowQueue(@Value("${forgeflow.rabbit.queue}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    public Binding workflowBinding(
            Queue workflowQueue,
            TopicExchange workflowExchange,
            @Value("${forgeflow.rabbit.routing-key}") String routingKey) {
        return BindingBuilder.bind(workflowQueue).to(workflowExchange).with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        // DefaultClassMapper matches the exact Java package of the payload class, not a parent package.
        String payloadPackage = "com.workflow.ForgeFlow.workflow.messaging";
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(payloadPackage);
        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setTrustedPackages(payloadPackage);
        classMapper.setDefaultType(WorkflowJobMessage.class);
        converter.setClassMapper(classMapper);
        return converter;
    }
}
