package com.adricarrera.marosatechnicaltest.service;

import com.adricarrera.marosatechnicaltest.domain.InvalidInvoice;
import com.adricarrera.marosatechnicaltest.domain.Invoice;
import com.adricarrera.marosatechnicaltest.exception.InvalidInvoiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.adricarrera.marosatechnicaltest.producer.InvoiceProducer;
import com.adricarrera.marosatechnicaltest.repository.InvalidInvoiceRepository;


/**
 * Orchestrates the invoice processing flow:
 * <p>
 * 1. Validate the incoming invoice.
 * 2. Transform it into the required XML representation.
 * 3. Publish the validated invoice to Kafka.
 * 4. Persist business validation errors in H2.
 * <p>
 * Technical/infrastructure exceptions are not swallowed here and are
 * handled by Kafka's error handling mechanism.
 */

@Service
public class InvoiceProcessingService {

  private static final Logger log =
      LoggerFactory.getLogger(InvoiceProcessingService.class);

  private final InvoiceValidationService invoiceValidationService;

  private final InvoiceTransformationService transformationService;

  private final InvoiceProducer invoiceProducer;

  private final InvalidInvoiceRepository invalidInvoiceRepository;

  public InvoiceProcessingService(
      InvoiceValidationService invoiceValidationService,
      InvoiceTransformationService transformationService,
      InvoiceProducer invoiceProducer, InvalidInvoiceRepository invalidInvoiceRepository) {

    this.invoiceValidationService = invoiceValidationService;
    this.transformationService = transformationService;
    this.invoiceProducer = invoiceProducer;
    this.invalidInvoiceRepository = invalidInvoiceRepository;
  }

  public void process(Invoice invoice) {
    try {
      invoiceValidationService.validate(invoice);

      invoiceProducer.send(invoice.invoiceId(), transformationService.transform(invoice));

      log.info("Invoice {} validated and published", invoice.invoiceId());

    } catch (InvalidInvoiceException e) {

      log.warn(
          "Invoice {} rejected: {}",
          invoice.invoiceId(),
          e.getMessage()
      );

      invalidInvoiceRepository.save(
          new InvalidInvoice(
              invoice.invoiceId(),
              e.getMessage()
          )
      );
    }
  }
}
