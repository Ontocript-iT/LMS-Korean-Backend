package com.lms.ontocriptIT.backend.dtos;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardDTO {
    // Summary Counters
    private long totalStudents;
    private long activeStudents;

    private long activeStudentsG1;
    private long activeStudentsG2;
    private long pendingStudents;
    private long totalVideos;
    private long totalZoomClasses;

    // Financial Overview
    private BigDecimal totalRevenue;
    private BigDecimal revenueThisMonth;
    private long pendingPaymentsCount;

    // Engagement Metrics
    private long totalVideoViews;
    private List<VideoStatsDTO> topPerformingVideos;
    private Map<String, Long> studentsByDistrict;

    // Recent Activity Lists
    private List<RecentPaymentDTO> recentPayments;
    private List<RecentUserDTO> recentRegistrations;

    @Data
    @Builder
    public static class VideoStatsDTO {
        private String title;
        private long viewCount;
    }

    @Data
    @Builder
    public static class RecentPaymentDTO {
        private String studentName;
        private BigDecimal amount;
        private String status;
        private String date;
    }

    @Data
    @Builder
    public static class RecentUserDTO {
        private String name;
        private String email;
        private String district;
        private String status;
    }
}