package com.cms.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  public static final String EMAIL_QUEUE = "email-queue";
  public static final String EMAIL_EXCHANGE = "email-exchange";
  public static final String EMAIL_ROUTING_KEY = "email-routing-key";
  public static final String EMAIL_DLQ = "email-dead-letter-queue";
  public static final String EMAIL_DLX = "email-dead-letter-exchange";

  @Bean
  public Queue emailQueue() {
    return QueueBuilder.durable(EMAIL_QUEUE)
        .withArgument("x-dead-letter-exchange", EMAIL_DLX)
        .withArgument("x-dead-letter-routing-key", EMAIL_ROUTING_KEY)
        .build();
  }

  @Bean
  public Queue emailDeadLetterQueue() {
    return new Queue(EMAIL_DLQ, true);
  }

  @Bean
  public DirectExchange emailExchange() {
    return new DirectExchange(EMAIL_EXCHANGE);
  }

  @Bean
  public DirectExchange emailDeadLetterExchange() {
    return new DirectExchange(EMAIL_DLX);
  }

  @Bean
  public Binding emailBinding(Queue emailQueue, DirectExchange emailExchange) {
    return BindingBuilder.bind(emailQueue).to(emailExchange).with(EMAIL_ROUTING_KEY);
  }

  @Bean
  public Binding emailDlqBinding(Queue emailDeadLetterQueue, DirectExchange emailDeadLetterExchange) {
    return BindingBuilder.bind(emailDeadLetterQueue).to(emailDeadLetterExchange).with(EMAIL_ROUTING_KEY);
  }

  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
      MessageConverter jsonMessageConverter) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(jsonMessageConverter);
    return rabbitTemplate;
  }
}
