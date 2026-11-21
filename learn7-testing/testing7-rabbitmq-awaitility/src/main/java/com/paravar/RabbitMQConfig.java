package com.paravar;

import tools.jackson.databind.json.JsonMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE_NAME = "ProductsExchange";
    public static final String ROUTING_KEY = "product.price";
    public static final String QUEUE_NAME = "product-price-changes";

    @Bean
    TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    Queue productPriceQueue() {
        return new Queue(QUEUE_NAME, false);
    }

    @Bean
    Binding productPriceQueueBinding(Queue productPriceQueue, TopicExchange exchange) {
        return BindingBuilder.bind(productPriceQueue)
                .to(exchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory, JsonMapper jsonMapper) {
        final var rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(producerJackson2MessageConverter(jsonMapper));
        return rabbitTemplate;
    }

    @Bean
    public JacksonJsonMessageConverter producerJackson2MessageConverter(
            JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }
}