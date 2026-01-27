package com.lms.ontocriptIT.backend.services.centralServices;

import org.springframework.http.ResponseEntity;

import java.time.YearMonth;

public interface PaymentService {
    ResponseEntity<?> recordPayment(com.lms.ontocriptIT.backend.dto.PaymentDTO paymentDTO);
    ResponseEntity<?> updatePaymentStatus(com.lms.ontocriptIT.backend.dto.UpdatePaymentStatusDTO dto, Long adminId);
    ResponseEntity<?> getStudentPayments(Long studentId);
    ResponseEntity<?> getAllPayments();
    ResponseEntity<?> getPaymentsByMonth(YearMonth month);
    ResponseEntity<?> getPaymentsByStatus(String status);
    ResponseEntity<?> getStudentPaymentSummary(Long studentId);
    ResponseEntity<?> deletePayment(Long paymentId);

    ResponseEntity<?> getThisMonthPaymentCompleterStudents( Integer year, Integer month,int page,int size);
}
