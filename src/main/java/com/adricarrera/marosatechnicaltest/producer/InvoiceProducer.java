package com.adricarrera.marosatechnicaltest.producer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes validated invoice XML to the output Kafka topic.
 *
 * The invoice ID is used as the Kafka message key so that events
 * for the same invoice are consistently associated with the same key.
 * This can also help downstream consumers implement idempotent processing.
 */

@Service
public class InvoiceProducer {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final String validatedTopic;

  public InvoiceProducer(
      KafkaTemplate<String, String> kafkaTemplate,
      @Value("${app.kafka.validated-topic}") String validatedTopic) {
    this.kafkaTemplate = kafkaTemplate;
    this.validatedTopic = validatedTopic;
  }

  public void send(String invoiceId, String xml) {
    kafkaTemplate
        .send(validatedTopic, invoiceId, xml)
        .join();
  }
}