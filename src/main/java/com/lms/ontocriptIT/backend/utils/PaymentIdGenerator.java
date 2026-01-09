package com.lms.ontocriptIT.backend.utils;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class PaymentIdGenerator {

    private static final String TRANSACTION_PREFIX = "TXN";
    private static final String RECEIPT_PREFIX = "RCP";
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generate unique transaction ID
     * Format: TXN-YYYYMMDD-HHMMSS-RAND6
     * Example: TXN-20260107-143025-A3B7C2
     */
    public String generateTransactionId() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String randomPart = generateRandomAlphanumeric(6);
        return TRANSACTION_PREFIX + "-" + timestamp + "-" + randomPart;
    }

    /**
     * Generate unique receipt number
     * Format: RCP-YYYY-MM-SEQUENTIAL
     * Example: RCP-2026-01-000123
     */
    public String generateReceiptNumber(long sequentialNumber) {
        String yearMonth = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String sequential = String.format("%06d", sequentialNumber);
        return RECEIPT_PREFIX + "-" + yearMonth + "-" + sequential;
    }

    /**
     * Generate random alphanumeric string
     */
    private String generateRandomAlphanumeric(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append(chars.charAt(random.nextInt(chars.length())));
        }
        return result.toString();
    }

    /**
     * Generate simple numeric transaction ID
     * Format: TXN-TIMESTAMP-RANDOM
     */
    public String generateSimpleTransactionId() {
        long timestamp = System.currentTimeMillis();
        int randomNum = random.nextInt(999999);
        return TRANSACTION_PREFIX + timestamp + String.format("%06d", randomNum);
    }
}
