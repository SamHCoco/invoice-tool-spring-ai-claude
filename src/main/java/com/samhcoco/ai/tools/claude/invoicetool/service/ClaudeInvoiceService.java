package com.samhcoco.ai.tools.claude.invoicetool.service;

import com.samhcoco.ai.tools.claude.invoicetool.config.InvoiceDirectories;
import com.samhcoco.ai.tools.claude.invoicetool.model.FeeTransaction;
import com.samhcoco.ai.tools.claude.invoicetool.model.Invoice;
import com.samhcoco.ai.tools.claude.invoicetool.repository.InvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static java.util.Collections.emptyList;

@Slf4j
@Service
public class ClaudeInvoiceService {

    private final AnthropicChatModel chatModel;
    private final InvoiceDirectories invoiceDirectories;
    private final InvoiceRepository invoiceRepository;
    private final FeeTransactionService feeTransactionService;

    public ClaudeInvoiceService(
            AnthropicChatModel chatModel,
            InvoiceDirectories invoiceDirectories,
            InvoiceRepository invoiceRepository,
            FeeTransactionService feeTransactionService) {
        this.chatModel = chatModel;
        this.invoiceDirectories = invoiceDirectories;
        this.invoiceRepository = invoiceRepository;
        this.feeTransactionService = feeTransactionService;
    }

    /**
     * Processes all PDFs in the configured directories, persists their transactions, and returns them.
     */
    public List<FeeTransaction> processAllInvoices() {
        List<FeeTransaction> allTransactions = new ArrayList<>();

        for (String directory : invoiceDirectories.getDirectories()) {
            Path dirPath = Paths.get(directory);
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                log.warn("Directory does not exist or is not a directory: {}", directory);
                continue;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*.pdf")) {
                for (Path pdfPath : stream) {
                    try {
                        log.info("Processing invoice: {}", pdfPath.getFileName());
                        List<FeeTransaction> transactions = extractAndPersistInvoiceTransactions(pdfPath);
                        if (!transactions.isEmpty()) {
                            allTransactions.addAll(transactions);
                        }
                    } catch (Exception e) {
                        log.error("Failed to process PDF {}: {}", pdfPath, e.getMessage());
                    }
                }
            } catch (IOException e) {
                log.error("Failed to read directory {}: {}", directory, e.getMessage());
            }
        }

        log.info("PERSISTED Transactions: ");
        allTransactions.forEach(t -> {
            log.info(t.toString());
        });

        return allTransactions;
    }

    /**
     * Reads a PDF, computes its SHA-256 hash, skips it if already processed,
     * sends its text to Claude, persists the transactions, and returns them.
     */
    private List<FeeTransaction> extractAndPersistInvoiceTransactions(Path pdfPath) throws IOException {
        byte[] fileBytes = Files.readAllBytes(pdfPath);
        String contentHash = sha256Hex(fileBytes);

        boolean alreadyProcessed = invoiceRepository.findByContentHash(contentHash).isPresent();
        if (alreadyProcessed) {
            log.info("Invoice already processed (hash: {}), skipping: {}", contentHash, pdfPath.getFileName());
            return emptyList();
        }

        Invoice invoice = Invoice.builder()
                .filePath(pdfPath.toAbsolutePath().toString())
                .contentHash(contentHash)
                .processedAt(LocalDateTime.now())
                .build();

        String pdfText;
        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            pdfText = stripper.getText(document);
        }

        String promptText = """
                Extract all fee transactions from the invoice below. \
                Output only pipe-separated lines: date|company|amount
                - date: YYYY-MM-DD
                - company: the paying organisation (not the individual recipient); use the shortest recognisable name, applied consistently
                - amount: decimal, no currency symbols
                Only include transactions contributing to the invoice total.
                PDF text:
                """ + pdfText;

        String response = chatModel.call(new Prompt(promptText)).getResult().getOutput().getText();

        List<FeeTransaction> transactions = new ArrayList<>();
        for (String line : response.split("\\r?\\n")) {
            String[] parts = line.split("\\|");
            if (parts.length != 3) continue;
            try {
                LocalDate date = LocalDate.parse(parts[0].trim());
                String company = parts[1].trim();
                BigDecimal amount = new BigDecimal(parts[2].trim());
                transactions.add(FeeTransaction.builder()
                        .date(date)
                        .company(company)
                        .amount(amount)
                        .build());
            } catch (Exception ex) {
                log.warn("Failed to parse line: '{}', skipping.", line);
            }
        }

        if (transactions.isEmpty()) {
            log.warn("No transactions parsed from invoice: {}, skipping persistence.", pdfPath.getFileName());
            return emptyList();
        }

        return feeTransactionService.saveInvoiceWithTransactions(invoice, transactions);
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
