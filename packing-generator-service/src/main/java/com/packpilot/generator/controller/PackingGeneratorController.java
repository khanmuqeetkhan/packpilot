package com.packpilot.generator.controller;

import com.packpilot.generator.dto.PackingRequest;
import com.packpilot.generator.dto.PackingResponse;
import com.packpilot.generator.service.PackingGenerationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PackingGeneratorController {

    private final PackingGenerationService generationService;

    public PackingGeneratorController(PackingGenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping("/packing-lists/generate")
    public ResponseEntity<PackingResponse> generate(@Valid @RequestBody PackingRequest request) {
        return ResponseEntity.ok(generationService.generate(request));
    }
}
