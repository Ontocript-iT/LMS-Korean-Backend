package com.lms.ontocriptIT.backend.services.serviceImpl;

import com.lms.ontocriptIT.backend.auth.RegistrationRequest;
import com.lms.ontocriptIT.backend.dtos.AdminDashboardDTO;
import com.lms.ontocriptIT.backend.entity.*;
import com.lms.ontocriptIT.backend.repository.*;
import com.lms.ontocriptIT.backend.services.centralServices.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final VideoRepository videoRepository;
    private final ZoomClassRepository zoomClassRepository;
    private final VideoWatchLogRepository watchLogRepository;

    private final RegistrationRequestRepository registrationRequestRepository;

    @Override
    public AdminDashboardDTO getAdminAnalysis() {
        YearMonth currentMonth = YearMonth.now();

        long totalStudents = userRepository.countByRole(Role.STUDENT);
        long activeStudents = userRepository.countByStatusAndRole(AccountStatus.ACTIVE,Role.STUDENT);
        long pendingStudents = registrationRequestRepository.countByStatus(RequestStatus.PENDING);
        long totalVideos = videoRepository.count();
        long totalZoom = zoomClassRepository.count();

        BigDecimal totalRevenue = paymentRepository.sumAllVerifiedPayments();
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        List<AdminDashboardDTO.VideoStatsDTO> topVideos = watchLogRepository
                .findTopVideos(PageRequest.of(0, 5))
                .stream()
                .map(obj -> AdminDashboardDTO.VideoStatsDTO.builder()
                        .title((String) obj[0])
                        .viewCount((Long) obj[1])
                        .build())
                .collect(Collectors.toList());


        List<AdminDashboardDTO.RecentPaymentDTO> recentPayments = paymentRepository
                .findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(p -> AdminDashboardDTO.RecentPaymentDTO.builder()
                        .studentName(p.getStudent().getFirstName() + " " + p.getStudent().getLastName())
                        .amount(p.getAmount())
                        .status(p.getStatus().name())
                        .date(p.getCreatedAt().toString())
                        .build())
                .collect(Collectors.toList());

        List<AdminDashboardDTO.RecentUserDTO> recentUsers = userRepository
                .findTop10ByRoleOrderByCreatedAtDesc(Role.STUDENT)
                .stream()
                .map(u -> AdminDashboardDTO.RecentUserDTO.builder()
                        .name(u.getFirstName() + " " + u.getLastName())
                        .email(u.getEmail())
                        .district(u.getDistrict())
                        .status(u.getStatus().name())
                        .build())
                .collect(Collectors.toList());

        return AdminDashboardDTO.builder()
                .totalStudents(totalStudents)
                .activeStudents(activeStudents)
                .pendingStudents(pendingStudents)
                .totalVideos(totalVideos)
                .totalZoomClasses(totalZoom)
                .totalRevenue(totalRevenue)
                .topPerformingVideos(topVideos)
                .recentPayments(recentPayments)
                .recentRegistrations(recentUsers)
                .totalVideoViews(watchLogRepository.count())
                .build();
    }
}
