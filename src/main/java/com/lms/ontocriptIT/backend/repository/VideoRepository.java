package com.lms.ontocriptIT.backend.repository;

import com.lms.ontocriptIT.backend.entity.Video;
import com.lms.ontocriptIT.backend.entity.VideoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    Optional<Video> findByBunnyVideoId(String bunnyVideoId);

    List<Video> findByIsActiveTrue();

    List<Video> findByUploadMonth(YearMonth uploadMonth);

    List<Video> findByStatus(VideoStatus status);

    @Query("SELECT COUNT(v) FROM Video v WHERE v.uploadMonth = :month AND v.isActive = true")
    long countActiveVideosByMonth(YearMonth month);

    @Query("SELECT v FROM Video v WHERE v.isActive = true ORDER BY v.createdAt DESC")
    List<Video> findAllActiveVideosOrderByCreatedDesc();
}
