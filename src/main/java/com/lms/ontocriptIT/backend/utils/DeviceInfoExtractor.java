package com.lms.ontocriptIT.backend.utils;

import com.lms.ontocriptIT.backend.dtos.DeviceInfoDTO;
import org.springframework.stereotype.Component;
import ua_parser.Client;
import ua_parser.Parser;

import jakarta.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class DeviceInfoExtractor {

    private final Parser uaParser;

    public DeviceInfoExtractor() {
        this.uaParser = new Parser();
    }

    public DeviceInfoDTO extractDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ipAddress = getClientIpAddress(request);

        Client client = uaParser.parse(userAgent);

        // Generate unique device fingerprint
        String fingerprint = generateDeviceFingerprint(ipAddress, userAgent);

        DeviceInfoDTO deviceInfo = DeviceInfoDTO.builder()
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .deviceType(determineDeviceType(userAgent))
                .browser(client.userAgent.family + " " + client.userAgent.major)
                .operatingSystem(client.os.family + " " + client.os.major)
                .deviceFingerprint(fingerprint)
                .build();

        return deviceInfo;
    }

    private String generateDeviceFingerprint(String ipAddress, String userAgent) {
        try {
            String fingerprintData = ipAddress + "|" + userAgent + "|" + System.currentTimeMillis();

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fingerprintData.getBytes());

            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        } catch (NoSuchAlgorithmException e) {
            return "fingerprint_error";
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    private String determineDeviceType(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }

        String ua = userAgent.toLowerCase();

        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return "Mobile";
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            return "Tablet";
        } else {
            return "Desktop";
        }
    }
}
