package com.blog_platform.auth.service;


import com.blog_platform.auth.dto.SignupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, Object> redisTemplate;

    public void storeOtp(String username, String otp, int expirationInMinutes) {
        redisTemplate.opsForValue().set(
                "otp:" + username,
                otp,
                expirationInMinutes,
                TimeUnit.MINUTES
        );
    }

    public String getOtp(String username) {
        return (String) redisTemplate.opsForValue().get("otp:" + username);
    }

    public void deleteOtp(String username) {
        redisTemplate.delete("otp:" + username);
    }

    public void storeTempUser(String username, SignupRequest signupRequest) {
        redisTemplate.opsForValue().set(
                "temp_user:" + username,
                signupRequest,
                10,
                TimeUnit.MINUTES
        );
    }

    public SignupRequest getTempUser(String username) {
        return (SignupRequest) redisTemplate.opsForValue().get("temp_user:" + username);
    }

    public void deleteTempUser(String username) {
        redisTemplate.delete("temp_user:" + username);
    }
}