package com.example.becircuitos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
        // Consider adding a custom ClientHttpRequestFactory for timeouts, etc.
        // E.g., HttpComponentsClientHttpRequestFactory or SimpleClientHttpRequestFactory
    }
}
