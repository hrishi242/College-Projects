// THIS SHOULD BE IN THE BLOG SERVICE, NOT RECOMMENDATION SERVICE
package com.blog_platform.blog.config;  // <- Note package change

import com.blog_platform.blog.dto.BlogEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, BlogEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Critical: Add type mapping for consumer compatibility
        config.put(JsonSerializer.TYPE_MAPPINGS,
                "blogEvent:com.blog_platform.blog.dto.BlogEvent");

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, BlogEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}