package com.lms.ontocriptIT.backend.services.serviceImpl;

import com.lms.ontocriptIT.backend.dtos.ClassAccessResponseDTO;
import com.lms.ontocriptIT.backend.dtos.PaymentResponseDTO;
import com.lms.ontocriptIT.backend.dtos.StudentPaymentSummaryDTO;
import com.lms.ontocriptIT.backend.entity.*;
import com.lms.ontocriptIT.backend.repository.*;
import com.lms.ontocriptIT.backend.services.centralServices.PaymentService;
import com.lms.ontocriptIT.backend.utils.PaymentIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ClassAccessRepository classAccessRepository;
    private final PaymentIdGenerator paymentIdGenerator;

    @Override
    @Transactional
    public ResponseEntity<?> recordPayment(com.lms.ontocriptIT.backend.dto.PaymentDTO paymentDTO) {
        try {
            User student = userRepository.findById(paymentDTO.getStudentId())
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            if (paymentRepository.findByStudentAndPaymentMonth(student, paymentDTO.getPaymentMonth()).isPresent()) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", "Payment record already exists for this month");
                response.put("paymentMonth", paymentDTO.getPaymentMonth().toString());
                response.put("status", HttpStatus.BAD_REQUEST.value());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            String transactionId = generateUniqueTransactionId();
            String receiptNumber = generateUniqueReceiptNumber();

            Payment payment = Payment.builder()
                    .student(student)
                    .paymentMonth(paymentDTO.getPaymentMonth())
                    .amount(paymentDTO.getAmount())
                    .status(PaymentStatus.VERIFIED)
                    .paymentMethod(paymentDTO.getPaymentMethod())
                    .transactionId(transactionId)
                    .receiptNumber(receiptNumber)
                    .notes(paymentDTO.getNotes())
                    .paidDate(LocalDateTime.now())
                    .build();

            Payment savedPayment = paymentRepository.save(payment);
            PaymentResponseDTO paymentResponseDTO = convertToResponseDTO(savedPayment);

            HashMap<String, Object> response = new HashMap<>();
            response.put("payment", paymentResponseDTO);
            response.put("transactionId", transactionId);
            response.put("receiptNumber", receiptNumber);
            response.put("message", "Payment recorded successfully");
            response.put("status", HttpStatus.CREATED.value());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", HttpStatus.NOT_FOUND.value());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to record payment: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private String generateUniqueTransactionId() {
        String transactionId;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            transactionId = paymentIdGenerator.generateTransactionId();
            attempts++;

            if (attempts >= maxAttempts) {
                throw new RuntimeException("Failed to generate unique transaction ID after " + maxAttempts + " attempts");
            }
        } while (paymentRepository.existsByTransactionId(transactionId));

        return transactionId;
    }

    private String generateUniqueReceiptNumber() {
        String receiptNumber;
        int attempts = 0;
        int maxAttempts = 10;

        LocalDateTime now = LocalDateTime.now();
        long sequentialNumber = paymentRepository.countPaymentsByYearAndMonth(
                now.getYear(),
                now.getMonthValue()
        ) + 1;

        do {
            receiptNumber = paymentIdGenerator.generateReceiptNumber(sequentialNumber);
            attempts++;
            sequentialNumber++;

            if (attempts >= maxAttempts) {
                throw new RuntimeException("Failed to generate unique receipt number after " + maxAttempts + " attempts");
            }
        } while (paymentRepository.existsByReceiptNumber(receiptNumber));

        return receiptNumber;
    }

    @Override
    @Transactional
    public ResponseEntity<?> updatePaymentStatus(com.lms.ontocriptIT.backend.dto.UpdatePaymentStatusDTO dto, Long adminId) {
        try {
            Payment payment = paymentRepository.findById(dto.getPaymentId())
                    .orElseThrow(() -> new RuntimeException("Payment not found"));

            User admin = userRepository.findById(adminId)
                    .orElseThrow(() -> new RuntimeException("Admin not found"));

            PaymentStatus previousStatus = payment.getStatus();
            payment.setStatus(dto.getStatus());
            payment.setNotes(dto.getNotes());

            if (dto.getStatus() == PaymentStatus.VERIFIED) {
                payment.setVerifiedDate(LocalDateTime.now());
                payment.setVerifiedBy(admin);
            }

            Payment savedPayment = paymentRepository.save(payment);

            HashMap<String, Object> response = new HashMap<>();
            response.put("paymentId", savedPayment.getId());
            response.put("transactionId", savedPayment.getTransactionId());
            response.put("receiptNumber", savedPayment.getReceiptNumber());
            response.put("previousStatus", previousStatus.toString());
            response.put("currentStatus", savedPayment.getStatus().toString());
            response.put("studentName", savedPayment.getStudent().getFirstName() + " " + savedPayment.getStudent().getLastName());
            response.put("paymentMonth", savedPayment.getPaymentMonth().toString());
            response.put("verifiedBy", admin.getFirstName() + " " + admin.getLastName());
            response.put("message", "Payment status updated successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", HttpStatus.NOT_FOUND.value());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to update payment status: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public ResponseEntity<?> getStudentPayments(Long studentId) {
        try {
            List<Payment> payments = paymentRepository.findByStudentIdOrderByPaymentMonthDesc(studentId);

            List<PaymentResponseDTO> responseDTOs = payments.stream()
                    .map(this::convertToResponseDTO)
                    .collect(Collectors.toList());

            HashMap<String, Object> response = new HashMap<>();
            response.put("data", responseDTOs);
            response.put("count", responseDTOs.size());
            response.put("studentId", studentId);
            response.put("message", "Student payments retrieved successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to retrieve student payments: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public ResponseEntity<?> getAllPayments() {
        try {
            List<Payment> payments = paymentRepository.findAll();

            List<PaymentResponseDTO> responseDTOs = payments.stream()
                    .map(this::convertToResponseDTO)
                    .collect(Collectors.toList());

            HashMap<String, Object> response = new HashMap<>();
            response.put("data", responseDTOs);
            response.put("count", responseDTOs.size());
            response.put("message", "All payments retrieved successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to retrieve all payments: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public ResponseEntity<?> getPaymentsByMonth(YearMonth month) {
        try {
            List<Payment> payments = paymentRepository.findByPaymentMonth(month);

            List<PaymentResponseDTO> responseDTOs = payments.stream()
                    .map(this::convertToResponseDTO)
                    .collect(Collectors.toList());

            HashMap<String, Object> response = new HashMap<>();
            response.put("data", responseDTOs);
            response.put("count", responseDTOs.size());
            response.put("month", month.toString());
            response.put("message", "Payments for " + month.toString() + " retrieved successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to retrieve payments by month: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public ResponseEntity<?> getPaymentsByStatus(String status) {
        try {
            PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
            List<Payment> payments = paymentRepository.findByStatus(paymentStatus);

            List<PaymentResponseDTO> responseDTOs = payments.stream()
                    .map(this::convertToResponseDTO)
                    .collect(Collectors.toList());

            HashMap<String, Object> response = new HashMap<>();
            response.put("data", responseDTOs);
            response.put("count", responseDTOs.size());
            response.put("status", HttpStatus.OK.value());
            response.put("paymentStatus", paymentStatus.toString());
            response.put("message", "Payments with status " + paymentStatus + " retrieved successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Invalid payment status: " + status);
            response.put("validStatuses", List.of("PENDING", "VERIFIED", "REJECTED"));
            response.put("status", HttpStatus.BAD_REQUEST.value());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to retrieve payments by status: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public ResponseEntity<?> getStudentPaymentSummary(Long studentId) {
        try {
            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            List<Payment> payments = paymentRepository.findByStudentIdOrderByPaymentMonthDesc(studentId);

            StudentPaymentSummaryDTO summary = StudentPaymentSummaryDTO.builder()
                    .studentId(student.getId())
                    .studentIdNumber(student.getStudentId())
                    .studentName(student.getFirstName() + " " + student.getLastName())
                    .email(student.getEmail())
                    .phoneNumber(student.getPhoneNumber1())
                    .totalPaid(paymentRepository.getTotalPaidByStudentId(studentId))
                    .totalPayments((int) payments.stream().count())
                    .pendingPayments(paymentRepository.countByStudentIdAndStatus(studentId, PaymentStatus.PENDING))
                    .verifiedPayments(paymentRepository.countByStudentIdAndStatus(studentId, PaymentStatus.VERIFIED))
                    .hasClassAccess(classAccessRepository.hasAnyActiveAccess(studentId))
                    .recentPayments(payments.stream()
                            .limit(5)
                            .map(this::convertToResponseDTO)
                            .collect(Collectors.toList()))
                    .classAccesses(classAccessRepository.findByStudentId(studentId).stream()
                            .map(this::convertToClassAccessResponseDTO)
                            .collect(Collectors.toList()))
                    .build();

            HashMap<String, Object> response = new HashMap<>();
            response.put("summary", summary);
            response.put("message", "Student payment summary retrieved successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", HttpStatus.NOT_FOUND.value());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to retrieve payment summary: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> deletePayment(Long paymentId) {
        try {
            if (!paymentRepository.existsById(paymentId)) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", "Payment not found");
                response.put("paymentId", paymentId);
                response.put("status", HttpStatus.NOT_FOUND.value());

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Payment payment = paymentRepository.findById(paymentId).get();
            String transactionId = payment.getTransactionId();
            String receiptNumber = payment.getReceiptNumber();

            paymentRepository.deleteById(paymentId);

            HashMap<String, Object> response = new HashMap<>();
            response.put("paymentId", paymentId);
            response.put("transactionId", transactionId);
            response.put("receiptNumber", receiptNumber);
            response.put("message", "Payment deleted successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to delete payment: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public ResponseEntity<?> getThisMonthPaymentCompleterStudents(Integer year, Integer month,int page,int size){
        try {
            YearMonth targetYearMonth;
            if (year != null && month != null) {
                targetYearMonth = YearMonth.of(year, month);
            } else {
                targetYearMonth = YearMonth.now();
            }
            Pageable pageable = Pageable.ofSize(size).withPage(page);
            Page<User> studentsPage = paymentRepository.findStudentsWithVerifiedPaymentForMonth(
                    targetYearMonth, pageable
            );


            List<com.lms.ontocriptIT.backend.dtos.StudentDTO> studentDTOs = studentsPage.getContent().stream()
                    .map(student -> com.lms.ontocriptIT.backend.dtos.StudentDTO.builder()
                            .id(student.getId())
                            .studentId(student.getStudentId())
                            .firstName(student.getFirstName())
                            .lastName(student.getLastName())
                            .email(student.getEmail())
                            .district(student.getDistrict())
                            .phoneNumber1(student.getPhoneNumber1())
                            .phoneNumber2(student.getPhoneNumber2())
                            .status(student.getStatus().toString())
                            .createdAt(student.getCreatedAt())
                            .paymentCount(paymentRepository.countByStudentId(student.getId()))
                            .classAccessCount(classAccessRepository.countByStudentId(student.getId()))
                            .build())
                    .collect(Collectors.toList());

            HashMap<String, Object> response = new HashMap<>();
            response.put("data", studentDTOs);
            response.put("count", studentDTOs.size());
            response.put("totalItems", studentsPage.getTotalElements());
            response.put("currentPage", studentsPage.getNumber());
            response.put("totalPages", studentsPage.getTotalPages());
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to retrieve students: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private PaymentResponseDTO convertToResponseDTO(Payment payment) {
        try {
            return PaymentResponseDTO.builder()
                    .id(payment.getId())
                    .studentId(payment.getStudent().getId())
                    .studentName(payment.getStudent().getFirstName() + " " + payment.getStudent().getLastName())
                    .studentIdNumber(payment.getStudent().getStudentId())
                    .email(payment.getStudent().getEmail())
                    .paymentMonth(payment.getPaymentMonth())
                    .amount(payment.getAmount())
                    .status(payment.getStatus())
                    .paymentMethod(payment.getPaymentMethod())
                    .transactionId(payment.getTransactionId())
                    .receiptNumber(payment.getReceiptNumber())
                    .paidDate(payment.getPaidDate())
                    .verifiedDate(payment.getVerifiedDate())
                    .verifiedByName(payment.getVerifiedBy() != null ?
                            payment.getVerifiedBy().getFirstName() + " " + payment.getVerifiedBy().getLastName() : null)
                    .notes(payment.getNotes())
                    .createdAt(payment.getCreatedAt())
                    .build();
        } catch (Exception e) {
            // Return minimal DTO on error
            return PaymentResponseDTO.builder()
                    .id(payment.getId())
                    .transactionId(payment.getTransactionId())
                    .receiptNumber(payment.getReceiptNumber())
                    .amount(payment.getAmount())
                    .status(payment.getStatus())
                    .build();
        }
    }

    private ClassAccessResponseDTO convertToClassAccessResponseDTO(ClassAccess classAccess) {
        try {
            return ClassAccessResponseDTO.builder()
                    .id(classAccess.getId())
                    .studentId(classAccess.getStudent().getId())
                    .studentName(classAccess.getStudent().getFirstName() + " " + classAccess.getStudent().getLastName())
                    .classLink(classAccess.getClassLink())
                    .className(classAccess.getClassName())
                    .hasAccess(classAccess.isHasAccess())
                    .accessGrantedDate(classAccess.getAccessGrantedDate())
                    .accessRevokedDate(classAccess.getAccessRevokedDate())
                    .grantedByName(classAccess.getGrantedBy() != null ?
                            classAccess.getGrantedBy().getFirstName() + " " + classAccess.getGrantedBy().getLastName() : null)
                    .revokedByName(classAccess.getRevokedBy() != null ?
                            classAccess.getRevokedBy().getFirstName() + " " + classAccess.getRevokedBy().getLastName() : null)
                    .notes(classAccess.getNotes())
                    .build();
        } catch (Exception e) {
            // Return minimal DTO on error
            return ClassAccessResponseDTO.builder()
                    .id(classAccess.getId())
                    .className(classAccess.getClassName())
                    .hasAccess(classAccess.isHasAccess())
                    .build();
        }
    }
}
