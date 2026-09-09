package com.adricarrera.marosatechnicaltest.service;

import com.adricarrera.marosatechnicaltest.domain.Invoice;
import com.adricarrera.marosatechnicaltest.domain.InvoiceLine;
import org.junit.jupiter.api.Test;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoiceTransformationServiceTest {

  private final InvoiceTransformationService transformationService =
      new InvoiceTransformationService();

  @Test
  void shouldCalculateTotalWithVat() {

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

    String xml = transformationService.transform(invoice);

    assertTrue(xml.contains("<Id>INV-TEST-001</Id>"));
    assertTrue(xml.contains("<Country>DE</Country>"));
    assertTrue(xml.contains("<Total>595.00</Total>"));
  }

  @Test
  void shouldCalculateTotalForMultipleLines() {

    Invoice invoice = new Invoice(
        "INV-TEST-002",
        "DE987654321",
        "DE",
        LocalDate.of(2024, 1, 2),
        List.of(
            new InvoiceLine(
                "Monitor",
                new BigDecimal("500"),
                new BigDecimal("0.19")
            ),
            new InvoiceLine(
                "Keyboard",
                new BigDecimal("100"),
                new BigDecimal("0.19")
            )
        )
    );

    String xml = transformationService.transform(invoice);

    assertTrue(xml.contains("<Total>714.00</Total>"));
  }
}