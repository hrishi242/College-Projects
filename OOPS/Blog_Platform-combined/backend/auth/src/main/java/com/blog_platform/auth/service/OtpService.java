package com.blog_platform.auth.service;


import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpService {
    public static final int OTP_LENGTH = 6;
    public static final int OTP_EXPIRATION_MINUTES = 5;

    private final RedisService redisService;
    private final EmailService emailService;

    public String generateOtp() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();

        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }

        return otp.toString();
    }

    public void sendOtp(String username, String email) {
        String otp = generateOtp();
        redisService.storeOtp(username, otp, OTP_EXPIRATION_MINUTES);
        // In production, implement actual email/SMS sending here
        emailService.sendOtpEmail(email, username, otp);
        System.out.println("OTP for " + username + ": " + otp);
    }

    public boolean verifyOtp(String username, String otp) {
        String storedOtp = redisService.getOtp(username);
        if (storedOtp == null || !storedOtp.equals(otp)) {
            return false;
        }
        redisService.deleteOtp(username);
        return true;
    }
}
