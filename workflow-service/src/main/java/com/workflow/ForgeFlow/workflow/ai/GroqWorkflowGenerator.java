package com.workflow.ForgeFlow.workflow.ai;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GroqWorkflowGenerator {

    private static final Logger log = LoggerFactory.getLogger(GroqWorkflowGenerator.class);

    private static final Pattern NAME = Pattern.compile("\"workflowName\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PAYLOAD = Pattern.compile("\"payload\"\\s*:\\s*\"([^\"]*)\"");

    private static final String SYSTEM_PROMPT = """
            You turn a user's sentence into a workflow job.
            Reply with JSON only, no markdown, exactly:
            {"workflowName":"kebab-or-short-name","payload":"short details"}
            workflowName: lowercase words separated by hyphens, max 40 chars.
            payload: compact text, no quotes inside.
            """;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GroqWorkflowGenerator(
            @Value("${forgeflow.ai.groq.base-url}") String baseUrl,
            @Value("${forgeflow.ai.groq.api-key:}") String apiKey,
            @Value("${GROQ_API_KEY:}") String groqEnvKey,
            @Value("${forgeflow.ai.groq.model}") String model,
            @Value("${forgeflow.ai.groq.connect-timeout:3s}") Duration connectTimeout,
            @Value("${forgeflow.ai.groq.read-timeout:10s}") Duration readTimeout) {
        String fromFile = normalizeKey(apiKey);
        String fromEnv = normalizeKey(groqEnvKey);
        this.apiKey = fromFile.isBlank() ? fromEnv : fromFile;
        this.model = model;
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    private static String normalizeKey(String apiKey) {
        if (apiKey == null) {
            return "";
        }
        String key = apiKey.trim();
        if (key.startsWith("\"") && key.endsWith("\"") && key.length() >= 2) {
            key = key.substring(1, key.length() - 1).trim();
        }
        if (key.isBlank() || "paste-your-groq-key-here".equalsIgnoreCase(key)) {
            return "";
        }
        return key;
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public GeneratedWorkflow generate(String prompt) {
        if (!isConfigured()) {
            throw new GroqCallException(HttpStatus.SERVICE_UNAVAILABLE, "Groq API key is missing");
        }

        GroqChatResponse response;
        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", model,
                            "temperature", 0.2,
                            "messages", List.of(
                                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                                    Map.of("role", "user", "content", prompt)
                            )
                    ))
                    .retrieve()
                    .body(GroqChatResponse.class);
        } catch (ResourceAccessException ex) {
            throw new GroqCallException(HttpStatus.GATEWAY_TIMEOUT, "Groq did not respond before the timeout");
        } catch (RestClientResponseException ex) {
            throw new GroqCallException(HttpStatus.BAD_GATEWAY, "Groq returned HTTP " + ex.getStatusCode().value());
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().getFirst().message() == null) {
            throw new GroqCallException(HttpStatus.BAD_GATEWAY, "Groq returned an empty response");
        }

        return parse(response.choices().getFirst().message().content());
    }

    static GeneratedWorkflow parse(String content) {
        String text = content == null ? "" : content.trim();
        Matcher name = NAME.matcher(text);
        Matcher payload = PAYLOAD.matcher(text);
        if (!name.find()) {
            log.warn("Unparsed Groq response: {}", text);
            throw new GroqCallException(HttpStatus.BAD_GATEWAY, "Could not parse workflowName from the AI response");
        }
        String payloadValue = payload.find() ? payload.group(1) : "";
        return new GeneratedWorkflow(name.group(1), payloadValue);
    }

    public record GroqChatResponse(List<Choice> choices) {
        public record Choice(Message message) {
        }

        public record Message(String content) {
        }
    }
}
