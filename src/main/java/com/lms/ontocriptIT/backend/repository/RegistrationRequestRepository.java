package com.lms.ontocriptIT.backend.repository;

import com.lms.ontocriptIT.backend.auth.RegistrationRequest;
import com.lms.ontocriptIT.backend.entity.AccountStatus;
import com.lms.ontocriptIT.backend.entity.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, Long> {
    Page<RegistrationRequest> findByStatus(RequestStatus status, Pageable pageable);
    Optional<RegistrationRequest> findByEmail(String email);
    Optional<RegistrationRequest> findByIdNumber(String idNumber);
    boolean existsByEmail(String email);
    boolean existsByIdNumber(String idNumber);

    long countByStatus(RequestStatus pending);
}