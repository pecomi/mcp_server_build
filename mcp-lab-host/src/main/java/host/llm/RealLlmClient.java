package host.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Anthropic Messages API (Tool Use) backed LlmClient.
 *
 * Stage 2 of the project's two-stage evaluation policy. Stage 1 uses MockLlmClient
 * / PoisonedMockLlmClient / CrossServerPoisonedMockLlmClient to validate the
 * mechanism deterministically; Stage 2 verifies whether a real LLM behaves the
 * same way (follows description directives or resists).
 *
 * Wired by LlmConfig when LLM_MODE=real_deterministic. Requires ANTHROPIC_API_KEY.
 */
public class RealLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(RealLlmClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;
    private final String apiUrl;

    public RealLlmClient(RestClient.Builder restClientBuilder, String apiKey, String model, String apiUrl) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey;
        this.model = model;
        this.apiUrl = apiUrl;
    }

    @Override
    public LlmDecision decide(String prompt, String scenarioId, List<ToolDescriptor> availableTools) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY is not set; cannot use LLM_MODE=real_deterministic.");
        }
        if (availableTools == null || availableTools.isEmpty()) {
            throw new IllegalStateException("No tools available from server; cannot dispatch.");
        }

        List<Map<String, Object>> tools = buildToolDefinitions(availableTools);
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", 1024);
        body.put("temperature", 0);
        body.put("tools", tools);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

        log.info("Anthropic API call: model={} prompt='{}' toolCount={}", model, prompt, tools.size());

        byte[] respBytes = restClient.post()
                .uri(apiUrl)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .body(body)
                .retrieve()
                .body(byte[].class);

        String respString = respBytes == null ? "" : new String(respBytes, StandardCharsets.UTF_8);
        log.debug("Anthropic response (first 600): {}", respString.substring(0, Math.min(600, respString.length())));

        try {
            JsonNode root = objectMapper.readTree(respString);
            JsonNode content = root.path("content");
            if (content.isArray()) {
                for (JsonNode item : content) {
                    if ("tool_use".equals(item.path("type").asText(""))) {
                        String toolName = item.path("name").asText("");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> input = objectMapper.convertValue(item.path("input"), Map.class);
                        if (input == null) input = Map.of();
                        log.info("Anthropic chose tool='{}' args={}", toolName, input);
                        return new LlmDecision(toolName, input);
                    }
                }
            }
            throw new IllegalStateException(
                    "Anthropic API returned no tool_use. stop_reason=" + root.path("stop_reason").asText("") +
                            " content[0]=" + (content.isArray() && content.size() > 0 ? content.get(0).toString() : "(empty)")
            );
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Anthropic response: " + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> buildToolDefinitions(List<ToolDescriptor> tools) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < tools.size(); i++) {
            ToolDescriptor t = tools.get(i);
            Map<String, Object> tool = new HashMap<>();
            tool.put("name", t.name());
            tool.put("description", t.description() == null ? "" : t.description());
            tool.put("input_schema", parseSchema(t.inputSchemaJson()));
            // prompt caching: cache the tools block via cache_control on last tool
            if (i == tools.size() - 1) {
                tool.put("cache_control", Map.of("type", "ephemeral"));
            }
            out.add(tool);
        }
        return out;
    }

    private Object parseSchema(String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) {
            return Map.of("type", "object", "properties", Map.of());
        }
        try {
            return objectMapper.readValue(schemaJson, Map.class);
        } catch (Exception e) {
            log.warn("Could not parse tool input_schema, falling back to empty: {}", e.getMessage());
            return Map.of("type", "object", "properties", Map.of());
        }
    }
}
