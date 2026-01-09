package com.lms.ontocriptIT.backend.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSummaryDTO {
    private Long id;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
}
