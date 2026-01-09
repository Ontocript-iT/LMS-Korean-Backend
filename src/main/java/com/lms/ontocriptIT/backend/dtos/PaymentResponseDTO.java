package com.lms.ontocriptIT.backend.dtos;

import com.lms.ontocriptIT.backend.entity.PaymentMethod;
import com.lms.ontocriptIT.backend.entity.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDTO {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentIdNumber;
    private String email;
    private YearMonth paymentMonth;
    private BigDecimal amount;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private String transactionId;
    private String receiptNumber;
    private LocalDateTime paidDate;
    private LocalDateTime verifiedDate;
    private String verifiedByName;
    private String notes;
    private LocalDateTime createdAt;
}

