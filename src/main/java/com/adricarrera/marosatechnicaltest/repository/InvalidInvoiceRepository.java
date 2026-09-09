package com.adricarrera.marosatechnicaltest.repository;


import com.adricarrera.marosatechnicaltest.domain.InvalidInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvalidInvoiceRepository extends JpaRepository<InvalidInvoice, Long> {
}
