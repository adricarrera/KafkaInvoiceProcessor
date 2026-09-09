package com.adricarrera.marosatechnicaltest.domain;

import java.time.LocalDate;
import java.util.List;

public record Invoice(
    String invoiceId,
    String customerId,
    String country,
    LocalDate issueDate,
    List<InvoiceLine> lines
) {}
