package com.lms.ontocriptIT.backend.services.serviceImpl;

import com.lms.ontocriptIT.backend.dtos.LoginHistoryDTO;
import com.lms.ontocriptIT.backend.entity.LoginHistory;
import com.lms.ontocriptIT.backend.repository.LoginHistoryRepository;
import com.lms.ontocriptIT.backend.services.centralServices.LoginHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoginHistoryServiceImpl implements LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;

    @Override
    public ResponseEntity<?> getStudentLoginHistory(Long studentId) {
        try {
            List<LoginHistory> history = loginHistoryRepository.findByUserIdOrderByLoginTimeDesc(studentId);

            List<LoginHistoryDTO> historyDTOs = history.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            long successfulLogins = loginHistoryRepository.countByUserIdAndLoginSuccessful(studentId, true);
            long failedLogins = loginHistoryRepository.countByUserIdAndLoginSuccessful(studentId, false);

            HashMap<String, Object> response = new HashMap<>();
            response.put("data", historyDTOs);
            response.put("totalLogins", history.size());
            response.put("successfulLogins", successfulLogins);
            response.put("failedLogins", failedLogins);
            response.put("message", "Login history retrieved successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to retrieve login history: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public ResponseEntity<?> getRecentLogins(int days) {
        try {
            LocalDateTime startDate = LocalDateTime.now().minusDays(days);
            List<LoginHistory> history = loginHistoryRepository.findRecentLogins(startDate);

            List<LoginHistoryDTO> historyDTOs = history.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            HashMap<String, Object> response = new HashMap<>();
            response.put("data", historyDTOs);
            response.put("count", historyDTOs.size());
            response.put("period", days + " days");
            response.put("message", "Recent logins retrieved successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to retrieve recent logins: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Override
    public ResponseEntity<?> getAllLoginHistory() {
        try {
            List<LoginHistory> history = loginHistoryRepository.findAll();

            List<LoginHistoryDTO> historyDTOs = history.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            HashMap<String, Object> response = new HashMap<>();
            response.put("data", historyDTOs);
            response.put("count", historyDTOs.size());
            response.put("message", "All login history retrieved successfully");
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Failed to retrieve login history: " + e.getMessage());
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private LoginHistoryDTO convertToDTO(LoginHistory history) {
        return LoginHistoryDTO.builder()
                .id(history.getId())
                .userId(history.getUser().getId())
                .studentId(history.getUser().getStudentId())
                .studentName(history.getUser().getFirstName() + " " + history.getUser().getLastName())
                .loginTime(history.getLoginTime())
                .ipAddress(history.getIpAddress())
                .deviceType(history.getDeviceType())
                .browser(history.getBrowser())
                .operatingSystem(history.getOperatingSystem())
                .loginSuccessful(history.isLoginSuccessful())
                .failureReason(history.getFailureReason())
                .build();
    }
}
