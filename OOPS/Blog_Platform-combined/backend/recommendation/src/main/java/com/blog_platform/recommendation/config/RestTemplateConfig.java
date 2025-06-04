package com.blog_platform.recommendation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Value("${embedding.service.timeout:30}")
    private int timeoutInSeconds;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .requestFactory(() -> {
                    var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout((int) Duration.ofSeconds(timeoutInSeconds).toMillis());
                    factory.setReadTimeout((int) Duration.ofSeconds(timeoutInSeconds).toMillis());
                    return factory;
                })
                .build();
    }
}