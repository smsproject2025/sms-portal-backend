package com.smsportal.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String SMS_EXCHANGE = "sms.exchange";
    public static final String SMS_QUEUE = "sms.queue";
    public static final String SMS_ROUTING_KEY = "sms.send";
    public static final String SMS_DLQ = "sms.dead-letter-queue";
    public static final String SMS_DL_EXCHANGE = "sms.dead-letter-exchange";

    // WhatsApp queues
    public static final String WA_QUEUE = "wa.queue";
    public static final String WA_ROUTING_KEY = "wa.send";

    @Bean
    public DirectExchange smsExchange() {
        return new DirectExchange(SMS_EXCHANGE);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(SMS_DL_EXCHANGE);
    }

    @Bean
    public Queue smsQueue() {
        return QueueBuilder.durable(SMS_QUEUE)
                .withArgument("x-dead-letter-exchange", SMS_DL_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", SMS_DLQ)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(SMS_DLQ).build();
    }

    @Bean
    public Queue waQueue() {
        return QueueBuilder.durable(WA_QUEUE)
                .withArgument("x-dead-letter-exchange", SMS_DL_EXCHANGE)
                .build();
    }

    @Bean
    public Binding waBinding(Queue waQueue, DirectExchange smsExchange) {
        return BindingBuilder.bind(waQueue).to(smsExchange).with(WA_ROUTING_KEY);
    }

    @Bean
    public Binding smsBinding(Queue smsQueue, DirectExchange smsExchange) {
        return BindingBuilder.bind(smsQueue).to(smsExchange).with(SMS_ROUTING_KEY);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(SMS_DLQ);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
