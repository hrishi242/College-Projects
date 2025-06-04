package com.blog_platform.auth.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OtpVerificationRequest {
    @NotBlank
    private String username;

    @NotBlank
    @Size(min = 6, max = 6)
    private String otp;
}