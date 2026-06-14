package com.smartit.smartsales.controller;

import com.smartit.smartsales.dto.request.AssistantRequest;
import com.smartit.smartsales.dto.response.AssistantResponse;
import com.smartit.smartsales.service.AssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping
    public AssistantResponse chat(@RequestBody AssistantRequest request) {
        return assistantService.process(request.message());
    }
}
