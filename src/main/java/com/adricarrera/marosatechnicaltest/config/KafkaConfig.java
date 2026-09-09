package com.adricarrera.marosatechnicaltest.config;

import com.adricarrera.marosatechnicaltest.domain.Invoice;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

  @Bean
  public ConsumerFactory<String, Invoice> consumerFactory(
      KafkaProperties kafkaProperties) {

    JacksonJsonDeserializer<Invoice> jsonDeserializer =
        new JacksonJsonDeserializer<>(Invoice.class);

    /*
     * Wraps the JSON deserializer to prevent malformed messages from
     * continuously blocking the Kafka consumer.
     *
     * Deserialization errors are delegated to Kafka's error handler.
     */

    ErrorHandlingDeserializer<Invoice> deserializer =
        new ErrorHandlingDeserializer<>(jsonDeserializer);

    return new DefaultKafkaConsumerFactory<>(
        kafkaProperties.buildConsumerProperties(),
        new StringDeserializer(),
        deserializer
    );
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Invoice>
  kafkaListenerContainerFactory(
      ConsumerFactory<String, Invoice> consumerFactory,
      DefaultErrorHandler errorHandler) {

    var factory =
        new ConcurrentKafkaListenerContainerFactory<String, Invoice>();

    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(errorHandler);

    return factory;
  }

  @Bean
  public DefaultErrorHandler kafkaErrorHandler() {

    /*
     * Retries failed Kafka records twice with a 1-second delay
     * before the record is considered exhausted.
     */
    return new DefaultErrorHandler(
        new FixedBackOff(1000L, 2L)
    );
  }
}