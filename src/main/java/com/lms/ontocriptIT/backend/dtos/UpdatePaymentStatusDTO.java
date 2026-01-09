package com.lms.ontocriptIT.backend.dto;

import com.lms.ontocriptIT.backend.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePaymentStatusDTO {

    @NotNull(message = "Payment ID is required")
    private Long paymentId;

    @NotNull(message = "Status is required")
    private PaymentStatus status;

    private String notes;
}
