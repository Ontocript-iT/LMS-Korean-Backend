package com.lms.ontocriptIT.backend.dtos;


import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfoDTO {
    private String ipAddress;
    private String userAgent;
    private String deviceType;
    private String browser;
    private String operatingSystem;
    private String deviceFingerprint;
}
