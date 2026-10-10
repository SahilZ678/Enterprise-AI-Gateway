package com.example.aigateway.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.example.aigateway.dto.ChatRequest;
import com.example.aigateway.dto.ChatResponse;
import com.example.aigateway.service.GatewayService;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final GatewayService gatewayService;

    public ChatController(
            GatewayService gatewayService
    ) {
        this.gatewayService = gatewayService;
    }

    @PostMapping
    public ChatResponse chat(
            @Valid @RequestBody ChatRequest request,
            @RequestHeader("X-API-Key") String clientApiKey
    ) {
        return gatewayService.chat(request, clientApiKey);
    }
}
