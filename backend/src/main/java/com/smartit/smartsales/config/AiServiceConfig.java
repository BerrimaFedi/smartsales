package com.smartit.smartsales.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AiServiceConfig {

    /**
     * On injecte RestClient.Builder (prototype Spring Boot) plutôt que d'appeler
     * RestClient.builder() statique : le builder auto-configuré embarque l'ObjectMapper
     * Spring Boot (avec ParameterNamesModule) capable de sérialiser les Java records.
     * Sans ça, les records produisent un body vide → FastAPI répond 422.
     */
    @Bean
    public RestClient aiRestClient(RestClient.Builder builder,
                                   @Value("${app.ai-service.url}") String baseUrl) {
        return builder
                .baseUrl(baseUrl)
                .build();
    }
}
