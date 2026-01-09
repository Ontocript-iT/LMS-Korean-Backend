package com.lms.ontocriptIT.backend.repository;

import com.lms.ontocriptIT.backend.entity.AccountStatus;
import com.lms.ontocriptIT.backend.entity.Role;
import com.lms.ontocriptIT.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByStudentId(String studentId);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber1(String phoneNumber);
    Optional<User> findByPhoneNumber2(String phoneNumber);
    Optional<User> findByIdNumber(String idNumber);
    Optional<User> findByResetPasswordToken(String token);
    boolean existsByEmail(String email);

    Page<User> findByRoleAndStatus(Role student, AccountStatus active, Pageable pageable);

    List<User> findByStudentIdContainingAndRoleAndStatus(String studentId, Role student, AccountStatus active);


}