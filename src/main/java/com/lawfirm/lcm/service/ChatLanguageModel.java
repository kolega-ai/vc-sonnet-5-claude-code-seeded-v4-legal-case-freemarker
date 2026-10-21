package com.lawfirm.lcm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

// Thin client for the firm's hosted legal assistant model (OpenAI-compatible chat API).
// The endpoint is configured per environment; the firm runs the model in its own tenancy.
@Service
public class ChatLanguageModel {

    private final HttpClient modelClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.model.endpoint:https://models.lawfirm.internal/v1/chat/completions}")
    private String endpoint;

    public String generate(List<Map<String, String>> messages) throws Exception {
        String requestBody = mapper.writeValueAsString(Map.of(
                "model", "lexmanage-assistant",
                "messages", messages));
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = modelClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = mapper.readTree(response.body());
        return root.path("choices").path(0).path("message").path("content").asText();
    }
}
