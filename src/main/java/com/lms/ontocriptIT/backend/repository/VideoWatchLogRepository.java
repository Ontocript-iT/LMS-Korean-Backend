package com.lms.ontocriptIT.backend.repository;

import com.lms.ontocriptIT.backend.entity.VideoWatchLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface VideoWatchLogRepository extends JpaRepository<VideoWatchLog, Long> {

    @Query("SELECT vwl FROM VideoWatchLog vwl WHERE vwl.student.id = :studentId AND vwl.video.id = :videoId ORDER BY vwl.createdAt DESC")
    List<VideoWatchLog> findByStudentIdAndVideoId(Long studentId, Long videoId);

    @Query("SELECT vwl FROM VideoWatchLog vwl WHERE vwl.student.id = :studentId ORDER BY vwl.createdAt DESC")
    List<VideoWatchLog> findByStudentId(Long studentId);

    @Query("SELECT v.title as title, COUNT(log.id) as count FROM VideoWatchLog log JOIN log.video v GROUP BY v.id ORDER BY count DESC")
    List<Object[]> findTopVideos(Pageable pageable);


    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM VideoWatchLog log WHERE log.video.id = :videoId")
    void deleteByVideoId(@Param("videoId") Long videoId);
}
