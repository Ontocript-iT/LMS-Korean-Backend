package com.lms.ontocriptIT.backend.repository;

import com.lms.ontocriptIT.backend.entity.User;
import com.lms.ontocriptIT.backend.entity.Video;
import com.lms.ontocriptIT.backend.entity.VideoAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoAccessRepository extends JpaRepository<VideoAccess, Long> {

    Optional<VideoAccess> findByStudentAndVideo(User student, Video video);

    List<VideoAccess> findByStudent(User student);

    List<VideoAccess> findByVideo(Video video);

    @Query("SELECT va FROM VideoAccess va WHERE va.student.id = :studentId AND va.hasAccess = true AND va.attemptsUsed < va.maxAttempts ORDER BY va.video.id DESC")
    List<VideoAccess> findAvailableVideosForStudentDesc(Long studentId);

    @Query("SELECT va FROM VideoAccess va WHERE va.student.id = :studentId AND va.video.id = :videoId")
    Optional<VideoAccess> findByStudentIdAndVideoId(Long studentId, Long videoId);


    @Modifying(flushAutomatically = true, clearAutomatically = true) // <--- THIS IS KEY
    @Transactional
    @Query("DELETE FROM VideoAccess va WHERE va.video.id = :videoId")
    void deleteByVideoId(@Param("videoId") Long videoId);

    VideoAccess findByStudentId(Long id);

//    Optional<VideoAccess> findByUser_IdAndVideo_Id(Long userId, Long videoId);
}
