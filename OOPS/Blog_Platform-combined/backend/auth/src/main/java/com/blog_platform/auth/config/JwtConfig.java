package com.blog_platform.auth.config;


import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
@Getter
public class JwtConfig {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private int jwtExpiration;

    public SecretKey getJwtSecret() {
        // Convert the raw secret to proper HMAC-SHA key
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

}
