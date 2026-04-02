package com.samhcoco.ai.tools.claude.invoicetool.runner;

import com.samhcoco.ai.tools.claude.invoicetool.service.ClaudeInvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeInvoiceServiceRunner implements CommandLineRunner {

    private final ClaudeInvoiceService invoiceService;

    @Override
    public void run(String... args) throws Exception {
        log.info("🚀 PROCESSING INVOICES...");
        invoiceService.processAllInvoices();
        log.info("✅ INVOICE PROCESSING COMPLETE");
    }
}
