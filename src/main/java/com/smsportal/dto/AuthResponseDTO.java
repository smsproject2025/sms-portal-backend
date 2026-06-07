package com.smsportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    private String token;
    private String refreshToken;
    private String type = "Bearer";
    private Long userId;
    private String name;
    private String email;
    private String role;
    private double walletBalance;
}
