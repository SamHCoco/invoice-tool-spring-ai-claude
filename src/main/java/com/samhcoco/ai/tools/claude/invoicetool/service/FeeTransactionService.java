package com.samhcoco.ai.tools.claude.invoicetool.service;

import com.samhcoco.ai.tools.claude.invoicetool.model.FeeTransaction;
import com.samhcoco.ai.tools.claude.invoicetool.model.Invoice;
import com.samhcoco.ai.tools.claude.invoicetool.repository.FeeTransactionRepository;
import com.samhcoco.ai.tools.claude.invoicetool.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeTransactionService {

    private final FeeTransactionRepository feeTransactionRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional
    public List<FeeTransaction> saveInvoiceWithTransactions(Invoice invoice, List<FeeTransaction> transactions) {
        Invoice savedInvoice = invoiceRepository.save(invoice);
        transactions.forEach(t -> t.setInvoice(savedInvoice));
        log.info("Persisting invoice {} with {} fee transaction(s)", savedInvoice.getContentHash(), transactions.size());
        return feeTransactionRepository.saveAll(transactions);
    }
}
