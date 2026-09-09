package com.adricarrera.marosatechnicaltest.service;

import com.adricarrera.marosatechnicaltest.domain.Invoice;
import com.adricarrera.marosatechnicaltest.domain.InvoiceLine;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Converts a validated invoice into the simplified XML format
 * required by the technical test.
 */

@Service
public class InvoiceTransformationService {

  public String transform(Invoice invoice) {

    BigDecimal total = calculateTotal(invoice);

    return """
            <Invoice>
                <Id>%s</Id>
                <Country>%s</Country>
                <Total>%s</Total>
            </Invoice>
            """.formatted(
        invoice.invoiceId(),
        invoice.country(),
        total
    );
  }

  // Each line total is calculated as:
  // amount + (amount * VAT rate)
  private BigDecimal calculateTotal(Invoice invoice) {
    return invoice.lines().stream()
        .map(this::calculateLineTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal calculateLineTotal(InvoiceLine line) {
    BigDecimal vatMultiplier =
        BigDecimal.ONE.add(line.vatRate());

    return line.amount().multiply(vatMultiplier);
  }
}