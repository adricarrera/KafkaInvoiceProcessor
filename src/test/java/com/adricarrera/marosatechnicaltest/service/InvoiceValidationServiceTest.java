package com.adricarrera.marosatechnicaltest.service;

import com.adricarrera.marosatechnicaltest.domain.Invoice;
import com.adricarrera.marosatechnicaltest.domain.InvoiceLine;
import com.adricarrera.marosatechnicaltest.exception.InvalidInvoiceException;
import org.junit.jupiter.api.Test;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceValidationServiceTest {

  private final InvoiceValidationService validationService =
      new InvoiceValidationService();

  @Test
  void shouldAcceptValidInvoice() {

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

    assertDoesNotThrow(() -> validationService.validate(invoice));
  }

  @Test
  void shouldRejectInvalidCountry() {

    Invoice invoice = new Invoice(
        "INV-TEST-002",
        "DE987654321",
        "XX",
        LocalDate.of(2024, 1, 2),
        List.of(
            new InvoiceLine(
                "Monitor",
                new BigDecimal("500"),
                new BigDecimal("0.19")
            )
        )
    );

    InvalidInvoiceException e =
        assertThrows(
            InvalidInvoiceException.class,
            () -> validationService.validate(invoice)
        );
    assertEquals("Invalid country code", e.getMessage());
  }

  @Test
  void shouldRejectInvalidCustomerId() {

    Invoice invoice = new Invoice(
        "INV-TEST-003",
        "INVALID",
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

    InvalidInvoiceException e =
        assertThrows(
            InvalidInvoiceException.class,
            () -> validationService.validate(invoice)
        );
    assertEquals("Invalid customer ID for country: " + invoice.country(), e.getMessage());
  }

  @Test
  void shouldRejectZeroAmount() {

    Invoice invoice = new Invoice(
        "INV-TEST-004",
        "DE987654321",
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

    InvalidInvoiceException e =
        assertThrows(
            InvalidInvoiceException.class,
            () -> validationService.validate(invoice)
        );
    assertEquals("Invoice line amount must be greater than zero", e.getMessage());
  }


  @Test
  void shouldRejectNullLines() {
    Invoice invoice = new Invoice(
        "INV-TEST-007",
        "DE987654321",
        "DE",
        LocalDate.of(2024, 1, 2),
        null
    );

    assertThrows(
        InvalidInvoiceException.class,
        () -> validationService.validate(invoice)
    );
  }

  @Test
  void shouldRejectEmptyLines() {
    Invoice invoice = new Invoice(
        "INV-TEST-008",
        "DE987654321",
        "DE",
        LocalDate.of(2024, 1, 2),
        List.of()
    );

    assertThrows(
        InvalidInvoiceException.class,
        () -> validationService.validate(invoice)
    );
  }

  @Test
  void shouldRejectNullAmount() {
    Invoice invoice = new Invoice(
        "INV-TEST-009",
        "DE987654321",
        "DE",
        LocalDate.of(2024, 1, 2),
        List.of(
            new InvoiceLine(
                "Monitor",
                null,
                new BigDecimal("0.19")
            )
        )
    );

    assertThrows(
        InvalidInvoiceException.class,
        () -> validationService.validate(invoice)
    );
  }

  @Test
  void shouldRejectNullVatRate() {
    Invoice invoice = new Invoice(
        "INV-TEST-010",
        "DE987654321",
        "DE",
        LocalDate.of(2024, 1, 2),
        List.of(
            new InvoiceLine(
                "Monitor",
                new BigDecimal("500"),
                null
            )
        )
    );

    assertThrows(
        InvalidInvoiceException.class,
        () -> validationService.validate(invoice)
    );
  }

  @Test
  void shouldRejectNegativeVatRate() {
    Invoice invoice = new Invoice(
        "INV-TEST-011",
        "DE987654321",
        "DE",
        LocalDate.of(2024, 1, 2),
        List.of(
            new InvoiceLine(
                "Monitor",
                new BigDecimal("500"),
                new BigDecimal("-0.19")
            )
        )
    );

    assertThrows(
        InvalidInvoiceException.class,
        () -> validationService.validate(invoice)
    );
  }
}