package com.lms.ontocriptIT.backend.repository;

import com.lms.ontocriptIT.backend.entity.User;
import com.lms.ontocriptIT.backend.entity.Video;
import com.lms.ontocriptIT.backend.entity.VideoAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoAccessRepository extends JpaRepository<VideoAccess, Long> {

    Optional<VideoAccess> findByStudentAndVideo(User student, Video video);

    List<VideoAccess> findByStudent(User student);

    List<VideoAccess> findByVideo(Video video);

    @Query("SELECT va FROM VideoAccess va WHERE va.student.id = :studentId AND va.hasAccess = true AND va.attemptsUsed < va.maxAttempts")
    List<VideoAccess> findAvailableVideosForStudent(Long studentId);

    @Query("SELECT va FROM VideoAccess va WHERE va.student.id = :studentId AND va.video.id = :videoId")
    Optional<VideoAccess> findByStudentIdAndVideoId(Long studentId, Long videoId);
}
