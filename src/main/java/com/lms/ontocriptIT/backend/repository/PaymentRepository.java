package com.lms.ontocriptIT.backend.repository;

import com.lms.ontocriptIT.backend.entity.Payment;
import com.lms.ontocriptIT.backend.entity.PaymentStatus;
import com.lms.ontocriptIT.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByStudent(User student);

    List<Payment> findByStudentOrderByPaymentMonthDesc(User student);

    List<Payment> findByStatus(PaymentStatus status);

    Optional<Payment> findByStudentAndPaymentMonth(User student, YearMonth paymentMonth);

    List<Payment> findByPaymentMonth(YearMonth paymentMonth);

    @Query("SELECT p FROM Payment p WHERE p.student.id = :studentId ORDER BY p.paymentMonth DESC")
    List<Payment> findByStudentIdOrderByPaymentMonthDesc(Long studentId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.student.id = :studentId AND p.status = :status")
    int countByStudentIdAndStatus(Long studentId, PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.student.id = :studentId AND p.status = 'VERIFIED'")
    java.math.BigDecimal getTotalPaidByStudentId(Long studentId);

    boolean existsByTransactionId(String transactionId);

    boolean existsByReceiptNumber(String receiptNumber);

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByReceiptNumber(String receiptNumber);

    @Query("SELECT COUNT(p) FROM Payment p WHERE YEAR(p.createdAt) = :year AND MONTH(p.createdAt) = :month")
    long countPaymentsByYearAndMonth(int year, int month);

    @Query("SELECT MAX(p.id) FROM Payment p")
    Long findMaxId();

    @Query("SELECT DISTINCT p.student FROM Payment p " +
            "WHERE p.status = 'VERIFIED' " +
            "AND p.paymentMonth = :targetMonth")
    Page<User> findStudentsWithVerifiedPaymentForMonth(
            @Param("targetMonth") YearMonth targetMonth, Pageable pageable
            );
    int countByStudentId(Long id);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'VERIFIED'")
    BigDecimal sumAllVerifiedPayments();

    // Fixes findTop10ByOrderByCreatedAtDesc()
    List<Payment> findTop10ByOrderByCreatedAtDesc();

    @Query("SELECT DISTINCT p.student FROM Payment p " +
            "WHERE p.paymentMonth = :month AND p.status = :status")
    List<User> findStudentsByMonthAndStatus(
            @Param("month") YearMonth month,
            @Param("status") PaymentStatus status
    );
}
