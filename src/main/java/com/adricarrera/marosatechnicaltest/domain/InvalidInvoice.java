package com.adricarrera.marosatechnicaltest.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class InvalidInvoice {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String invoiceId;
  private String reason;

  public InvalidInvoice() {
  }

  public InvalidInvoice(String invoiceId, String reason) {
    this.invoiceId = invoiceId;
    this.reason = reason;
  }

  public Long getId() {
    return id;
  }

  public String getInvoiceId() {
    return invoiceId;
  }

  public String getReason() {
    return reason;
  }
}