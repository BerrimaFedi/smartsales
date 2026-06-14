package com.smartit.smartsales.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartit.smartsales.dto.ai.OptimizeRequestDto;
import com.smartit.smartsales.dto.ai.OptimizeResponseDto;
import com.smartit.smartsales.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiOptimizerClient {

    private final RestClient aiRestClient;
    private final ObjectMapper objectMapper;

    public OptimizeResponseDto optimize(OptimizeRequestDto request) {
        try {
            log.info("Appel service IA /optimize — payload : {}", objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            log.warn("Sérialisation du payload IA impossible : {}", e.getMessage());
        }

        try {
            OptimizeResponseDto response = aiRestClient.post()
                    .uri("/optimize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(OptimizeResponseDto.class);

            if (response == null) {
                throw new ServiceUnavailableException("Le service IA a retourné une réponse vide");
            }
            return response;

        } catch (ResourceAccessException ex) {
            log.error("Service IA injoignable : {}", ex.getMessage());
            throw new ServiceUnavailableException("Le service IA est temporairement injoignable. Veuillez réessayer.");
        } catch (RestClientException ex) {
            log.error("Erreur appel service IA : {}", ex.getMessage());
            throw new ServiceUnavailableException("Erreur de communication avec le service IA : " + ex.getMessage());
        }
    }
}
