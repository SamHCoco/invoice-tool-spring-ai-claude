package com.samhcoco.ai.tools.claude.invoicetool.service;

import com.samhcoco.ai.tools.claude.invoicetool.model.FeeTransaction;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ClaudeInvoiceService {

    private final AnthropicChatModel chatModel;
    private final List<String> invoiceDirectories;

    public ClaudeInvoiceService(
            AnthropicChatModel chatModel,
            @Value("${invoice.directories}") List<String> invoiceDirectories) {
        this.chatModel = chatModel;
        this.invoiceDirectories = invoiceDirectories;
    }

    /**
     * Processes all PDFs in the configured directories and extracts transactions.
     */
    public List<FeeTransaction> processAllInvoices() {
        List<FeeTransaction> allTransactions = new ArrayList<>();

        for (String directory : invoiceDirectories) {
            Path dirPath = Paths.get(directory);
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                log.warn("Directory does not exist or is not a directory: {}", directory);
                continue;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*.pdf")) {
                for (Path pdfPath : stream) {
                    try {
                        log.info("Processing invoice: {}", pdfPath.getFileName());
                        List<FeeTransaction> transactions = extractInvoiceTransactions(pdfPath);
                        allTransactions.addAll(transactions);
                    } catch (Exception e) {
                        log.error("Failed to process PDF {}: {}", pdfPath, e.getMessage());
                    }
                }
            } catch (IOException e) {
                log.error("Failed to read directory {}: {}", directory, e.getMessage());
            }
        }

        return allTransactions;
    }

    /**
     * Reads a PDF, sends its text to Claude, and returns a list of FeeTransaction.
     */
    private List<FeeTransaction> extractInvoiceTransactions(Path pdfPath) {
        String pdfText;
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            pdfText = stripper.getText(document);
        } catch (IOException e) {
            log.error("Failed to read PDF file at {}: {}", pdfPath, e.getMessage());
            throw new RuntimeException("Failed to read PDF", e);
        }

        String promptText = """
                Extract all invoice transactions from the following PDF text.
                For each transaction, return a single line in this format (pipe-separated):
                date|company|amount
                Use ISO date format YYYY-MM-DD and numeric amount with decimal dot.
                Only include transactions that contribute to the invoice total.
                PDF text:
                """ + pdfText;

        String response = chatModel.call(new Prompt(promptText)).getResult().getOutput().getText();

        List<FeeTransaction> transactions = new ArrayList<>();
        String[] lines = response.split("\\r?\\n");
        for (String line : lines) {
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

        return transactions;
    }
}