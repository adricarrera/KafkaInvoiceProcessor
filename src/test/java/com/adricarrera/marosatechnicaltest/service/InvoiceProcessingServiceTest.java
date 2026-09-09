package com.adricarrera.marosatechnicaltest.service;

import com.adricarrera.marosatechnicaltest.domain.InvalidInvoice;
import com.adricarrera.marosatechnicaltest.domain.Invoice;
import com.adricarrera.marosatechnicaltest.domain.InvoiceLine;
import com.adricarrera.marosatechnicaltest.exception.InvalidInvoiceException;
import com.adricarrera.marosatechnicaltest.producer.InvoiceProducer;
import com.adricarrera.marosatechnicaltest.repository.InvalidInvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class InvoiceProcessingServiceTest {

  @Mock
  private InvoiceValidationService validationService;

  @Mock
  private InvoiceTransformationService transformationService;

  @Mock
  private InvoiceProducer invoiceProducer;

  @Mock
  private InvalidInvoiceRepository invalidInvoiceRepository;

  @InjectMocks
  private InvoiceProcessingService processingService;

  @Test
  void shouldProcessValidInvoice() {
    Invoice invoice = new Invoice(
        "INV-TEST-001",
        "DE987654321",
        "DE",
        LocalDate.of(2024, 1, 2),
        List.of(
            new InvoiceLine(
                "Monitor",
                new BigDecimal("500"),
                new BigDecimal("0.19")
            )
        )
    );

    when(transformationService.transform(invoice))
        .thenReturn("<Invoice>test</Invoice>");

    processingService.process(invoice);

    verify(validationService).validate(invoice);
    verify(transformationService).transform(invoice);
    verify(invoiceProducer)
        .send("INV-TEST-001", "<Invoice>test</Invoice>");

    verifyNoInteractions(invalidInvoiceRepository);
  }

  @Test
  void shouldSaveInvalidInvoice() {

    Invoice invoice = new Invoice(
        "INV-INVALID-001",
        "INVALID",
        "DE",
        LocalDate.of(2024, 1, 2),
        List.of(
            new InvoiceLine(
                "Monitor",
                BigDecimal.ZERO,
                new BigDecimal("0.19")
            )
        )
    );

    doThrow(new InvalidInvoiceException(
        "Invoice line amount must be greater than zero"
    )).when(validationService).validate(invoice);

    processingService.process(invoice);

    verify(validationService).validate(invoice);

    verify(invalidInvoiceRepository)
        .save(any(InvalidInvoice.class));

    verifyNoInteractions(transformationService);
    verifyNoInteractions(invoiceProducer);
  }
}