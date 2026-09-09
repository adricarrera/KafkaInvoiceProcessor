package com.adricarrera.marosatechnicaltest.validation;

import java.util.Map;
import java.util.regex.Pattern;

public final class CustomerIdValidationRules {

  private CustomerIdValidationRules() {
  }

  /*
   * Customer ID format by country.
   *
   * Keeping these rules outside the validation service makes it easier
   * to add or modify country-specific formats without changing the
   * invoice validation flow.
   */
  public static final Map<String, Pattern> PATTERNS = Map.of(
      "ES", Pattern.compile("^ES[0-9]{8}[A-Z]$"),
      "DE", Pattern.compile("^DE\\d{9}$"),
      "FR", Pattern.compile("^FR[A-HJ-NP-Z0-9]{2}\\d{9}$")
  );
}