package com.smartit.smartsales.service;

import com.smartit.smartsales.dto.ai.AnalyseRequestDto;
import com.smartit.smartsales.dto.ai.AnalyseResponseDto;
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
public class AiAnalyseClient {

    private final RestClient aiRestClient;

    public AnalyseResponseDto analyser(AnalyseRequestDto request) {
        try {
            AnalyseResponseDto response = aiRestClient.post()
                    .uri("/analyze-performances")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AnalyseResponseDto.class);

            if (response == null) {
                throw new ServiceUnavailableException("Le service d'analyse IA a retourné une réponse vide");
            }
            return response;

        } catch (ResourceAccessException ex) {
            log.error("Service IA injoignable : {}", ex.getMessage());
            throw new ServiceUnavailableException("Le service d'analyse IA est temporairement injoignable.");
        } catch (RestClientException ex) {
            log.error("Erreur appel service IA analyse : {}", ex.getMessage());
            throw new ServiceUnavailableException("Erreur de communication avec le service d'analyse IA : " + ex.getMessage());
        }
    }
}
