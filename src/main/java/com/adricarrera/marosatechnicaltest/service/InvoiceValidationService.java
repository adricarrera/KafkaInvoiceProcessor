package com.adricarrera.marosatechnicaltest.service;

import com.adricarrera.marosatechnicaltest.domain.Invoice;
import com.adricarrera.marosatechnicaltest.exception.InvalidInvoiceException;
import com.adricarrera.marosatechnicaltest.validation.CustomerIdValidationRules;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validates incoming invoices before they are processed.
 * Country-specific customer ID rules are kept separately in
 * CustomerIdValidationRules to make them easier to maintain and extend.
 */

@Service
public class InvoiceValidationService {

  public void validate(Invoice invoice) {
    this.validateCountry(invoice);
    this.validateCustomerId(invoice);
    this.validateLines(invoice);
  }

  /**
   * Validates that the country is a valid ISO code supported by the service.
   * The service currently supports ES, DE and FR because these are the
   * countries for which customer ID validation rules are implemented.
   */
  private void validateCountry(Invoice invoice) {

    String country = invoice.country();

    if (!Arrays.asList(Locale.getISOCountries()).contains(country)) {
      throw new InvalidInvoiceException("Invalid country code");
    }

    if (!CustomerIdValidationRules.PATTERNS.containsKey(country)) {
      throw new InvalidInvoiceException(
          "Country not supported: " + country
      );
    }
  }

  // Validates the customer ID using the format defined for the invoice country.
  private void validateCustomerId(Invoice invoice) {
    Pattern pattern =
        CustomerIdValidationRules.PATTERNS.get(invoice.country());

    if (pattern == null
        || invoice.customerId() == null
        || !pattern.matcher(invoice.customerId()).matches()) {

      throw new InvalidInvoiceException(
          "Invalid customer ID for country: " + invoice.country()
      );
    }
  }

  // Validates that the invoice contains valid line amounts and VAT rates.
  private void validateLines(Invoice invoice) {

    if (invoice.lines() == null || invoice.lines().isEmpty()) {
      throw new InvalidInvoiceException(
          "Invoice must contain at least one line"
      );
    }

    invoice.lines().forEach(line -> {

      if (line.amount() == null
          || line.amount().compareTo(BigDecimal.ZERO) <= 0) {

        throw new InvalidInvoiceException(
            "Invoice line amount must be greater than zero"
        );
      }

      if (line.vatRate() == null
          || line.vatRate().compareTo(BigDecimal.ZERO) < 0) {

        throw new InvalidInvoiceException(
            "Invoice VAT rate cannot be negative"
        );
      }

      if (line.vatRate().compareTo(BigDecimal.ONE) > 0) {
        throw new InvalidInvoiceException(
            "Invoice VAT rate cannot be greater than 1"
        );
      }
    });
  }
}