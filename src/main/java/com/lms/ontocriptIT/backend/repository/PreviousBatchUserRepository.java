package com.lms.ontocriptIT.backend.repository;

import com.lms.ontocriptIT.backend.entity.PreviousBatchUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PreviousBatchUserRepository extends JpaRepository<PreviousBatchUser, Long> {
}