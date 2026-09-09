package com.adricarrera.marosatechnicaltest.domain;

import java.math.BigDecimal;

/*
 * BigDecimal is used for monetary calculations to avoid
 * floating-point precision issues.
 */
public record InvoiceLine(
    String description,
    BigDecimal amount,
    BigDecimal vatRate
) {}