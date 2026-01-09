package com.lms.ontocriptIT.backend.controller;


import com.lms.ontocriptIT.backend.dto.PaymentDTO;
import com.lms.ontocriptIT.backend.dto.UpdatePaymentStatusDTO;
import com.lms.ontocriptIT.backend.entity.User;
import com.lms.ontocriptIT.backend.services.centralServices.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/record")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> recordPayment(@Valid @RequestBody PaymentDTO paymentDTO) {
        return paymentService.recordPayment(paymentDTO);
    }

    @PutMapping("/update-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePaymentStatus(
            @Valid @RequestBody UpdatePaymentStatusDTO dto,
            @AuthenticationPrincipal User admin) {
        return paymentService.updatePaymentStatus(dto, admin.getId());
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public ResponseEntity<?> getStudentPayments(@PathVariable Long studentId) {
        return paymentService.getStudentPayments(studentId);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/month/{year}/{month}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPaymentsByMonth(
            @PathVariable int year,
            @PathVariable int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        return paymentService.getPaymentsByMonth(yearMonth);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPaymentsByStatus(@PathVariable String status) {
        return paymentService.getPaymentsByStatus(status);
    }

    @GetMapping("/summary/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    public ResponseEntity<?> getStudentPaymentSummary(@PathVariable Long studentId) {
        return paymentService.getStudentPaymentSummary(studentId);
    }

    @DeleteMapping("/{paymentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deletePayment(@PathVariable Long paymentId) {
        return paymentService.deletePayment(paymentId);
    }

    @GetMapping("/getThisMonthPaymentCompleterStudents")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getThisMonthPaymentCompleterStudents() {
        return paymentService.getThisMonthPaymentCompleterStudents();
    }
}
