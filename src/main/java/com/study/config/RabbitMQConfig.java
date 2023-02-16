package com.study.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    @Bean
    public Queue secKillQueue() {
        return new Queue("secKillQueue");
    }

    @Bean
    public FanoutExchange secKillExchange() {
        return new FanoutExchange("secKillExchange");
    }

    @Bean
    public Binding secKillBinding() {
        return new Binding("secKillQueue", Binding.DestinationType.QUEUE, "secKillExchange", "", null);
    }
}
