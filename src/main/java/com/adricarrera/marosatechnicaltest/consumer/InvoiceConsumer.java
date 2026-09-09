package com.adricarrera.marosatechnicaltest.consumer;

import com.adricarrera.marosatechnicaltest.domain.Invoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.adricarrera.marosatechnicaltest.service.InvoiceProcessingService;

/**
 * Kafka adapter responsible for consuming incoming invoice events.
 *
 * The consumer intentionally contains no business logic.
 * Processing is delegated to InvoiceProcessingService.
 */

@Component
public class InvoiceConsumer {

  private static final Logger log =
      LoggerFactory.getLogger(InvoiceConsumer.class);
  private final InvoiceProcessingService invoiceProcessingService;

  public InvoiceConsumer(InvoiceProcessingService invoiceProcessingService) {
    this.invoiceProcessingService = invoiceProcessingService;
  }

  @KafkaListener(
      topics = "${app.kafka.incoming-topic}",
      groupId = "${spring.kafka.consumer.group-id}",
      containerFactory = "kafkaListenerContainerFactory"
  )
  public void consume(Invoice invoice) {
    if (invoice == null) {
      log.warn("Received null invoice");
      return;
    }
    log.info("Received invoice: {}", invoice.invoiceId());
    invoiceProcessingService.process(invoice);
  }
}
