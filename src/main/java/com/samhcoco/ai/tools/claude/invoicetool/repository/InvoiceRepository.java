package com.samhcoco.ai.tools.claude.invoicetool.repository;

import com.samhcoco.ai.tools.claude.invoicetool.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByContentHash(String contentHash);
}
