package com.example.invoice.service;

import com.example.invoice.model.Invoice;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InvoiceExtractor {

    // naive regexes for demo
    private static final Pattern INVOICE_NO = Pattern.compile("(?i)invoice\s*#?:?\s*(\\w+-?\\d+)");
    private static final Pattern TOTAL_AMOUNT = Pattern.compile("(?i)total\s*[:\\s]\s*$?([0-9.,]+)");
    private static final Pattern DATE = Pattern.compile("(?i)date\s*[:\\s]\s*([0-9]{2,4}[-/\\.][0-9]{1,2}[-/\\.][0-9]{1,4})");

    public Invoice extractInvoice(File pdfFile) throws IOException {
        String text = extractText(pdfFile);

        Invoice inv = new Invoice();
        inv.setInvoiceNumber(findFirst(INVOICE_NO, text));
        inv.setVendor(null); // vendor parsing could be improved
        inv.setTotalAmount(parseAmount(findFirst(TOTAL_AMOUNT, text)));
        inv.setInvoiceDate(findFirst(DATE, text));
        inv.setCreatedAt(Instant.now());
        return inv;
    }

    public String extractFieldFromJson(String json, String field) {
        // very small helper for expected simple message formats
        Pattern p = Pattern.compile("\"" + field + "\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
        Matcher m = p.matcher(json);
        if (m.find()) return m.group(1);
        return null;
    }

    private String extractText(File pdfFile) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String findFirst(Pattern p, String text) {
        if (text == null) return null;
        Matcher m = p.matcher(text);
        if (m.find()) return m.group(1);
        return null;
    }

    private Double parseAmount(String amt) {
        if (amt == null) return null;
        try {
            String cleaned = amt.replaceAll(",", "");
            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            return null;
        }
    }
}

