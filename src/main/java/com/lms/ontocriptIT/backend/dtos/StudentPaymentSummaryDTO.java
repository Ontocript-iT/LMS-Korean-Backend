package com.lms.ontocriptIT.backend.dtos;


import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentPaymentSummaryDTO {
    private Long studentId;
    private String studentIdNumber;
    private String studentName;
    private String email;
    private String phoneNumber;
    private BigDecimal totalPaid;
    private int totalPayments;
    private int pendingPayments;
    private int verifiedPayments;
    private boolean hasClassAccess;
    private List<PaymentResponseDTO> recentPayments;
    private List<ClassAccessResponseDTO> classAccesses;
}
