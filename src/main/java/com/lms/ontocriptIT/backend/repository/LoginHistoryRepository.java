package com.lms.ontocriptIT.backend.repository;

import com.lms.ontocriptIT.backend.entity.LoginHistory;
import com.lms.ontocriptIT.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    List<LoginHistory> findByUserOrderByLoginTimeDesc(User user);

    List<LoginHistory> findByUserIdOrderByLoginTimeDesc(Long userId);

    @Query("SELECT lh FROM LoginHistory lh WHERE lh.user.id = :userId AND lh.loginSuccessful = true ORDER BY lh.loginTime DESC")
    List<LoginHistory> findSuccessfulLoginsByUserId(Long userId);

    @Query("SELECT lh FROM LoginHistory lh WHERE lh.loginTime >= :startDate ORDER BY lh.loginTime DESC")
    List<LoginHistory> findRecentLogins(LocalDateTime startDate);

    long countByUserIdAndLoginSuccessful(Long userId, boolean loginSuccessful);

    // FIXED: Device fingerprint SELECT queries
    @Query("SELECT lh FROM LoginHistory lh WHERE lh.user.id = :userId AND lh.deviceFingerprint = :fingerprint AND lh.loginSuccessful = true AND lh.isCurrentSession = true")
    Optional<LoginHistory> findCurrentSessionByUserIdAndFingerprint(@Param("userId") Long userId, @Param("fingerprint") String fingerprint);

    @Query("SELECT lh FROM LoginHistory lh WHERE lh.user.id = :userId AND lh.loginSuccessful = true AND lh.isCurrentSession = true")
    Optional<LoginHistory> findCurrentSessionByUserId(@Param("userId") Long userId);

    @Query("SELECT lh FROM LoginHistory lh WHERE lh.deviceFingerprint = :fingerprint ORDER BY lh.loginTime DESC")
    Optional<LoginHistory> findLatestSessionByFingerprint(String fingerprint);

    // FIXED: UPDATE queries with @Modifying and @Transactional
    @Modifying
    @Transactional
    @Query("UPDATE LoginHistory lh SET lh.isCurrentSession = false WHERE lh.user.id = :userId AND lh.isCurrentSession = true")
    int invalidateCurrentSessionsByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE LoginHistory lh SET lh.isCurrentSession = false WHERE lh.deviceFingerprint = :fingerprint")
    int invalidateSessionByFingerprint(@Param("fingerprint") String fingerprint);

}
