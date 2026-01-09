package com.lms.ontocriptIT.backend.repository;

import com.lms.ontocriptIT.backend.entity.VideoWatchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoWatchLogRepository extends JpaRepository<VideoWatchLog, Long> {

    @Query("SELECT vwl FROM VideoWatchLog vwl WHERE vwl.student.id = :studentId AND vwl.video.id = :videoId ORDER BY vwl.createdAt DESC")
    List<VideoWatchLog> findByStudentIdAndVideoId(Long studentId, Long videoId);

    @Query("SELECT vwl FROM VideoWatchLog vwl WHERE vwl.student.id = :studentId ORDER BY vwl.createdAt DESC")
    List<VideoWatchLog> findByStudentId(Long studentId);
}
