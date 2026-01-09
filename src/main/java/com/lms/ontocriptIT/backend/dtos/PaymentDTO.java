package com.lms.ontocriptIT.backend.dto;

import com.lms.ontocriptIT.backend.entity.PaymentMethod;
import com.lms.ontocriptIT.backend.entity.PaymentStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDTO {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Payment month is required")
    private YearMonth paymentMonth;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String transactionId;

    private String receiptNumber;

    private String notes;
}
