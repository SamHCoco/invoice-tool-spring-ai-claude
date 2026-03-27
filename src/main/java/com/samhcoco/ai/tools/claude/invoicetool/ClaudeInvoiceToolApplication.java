package com.samhcoco.ai.tools.claude.invoicetool;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ClaudeInvoiceToolApplication {

	public static void main(String[] args) {
        SpringApplication.run(ClaudeInvoiceToolApplication.class, args);
	}

}
