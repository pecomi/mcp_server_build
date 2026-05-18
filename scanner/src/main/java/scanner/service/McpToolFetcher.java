package scanner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import scanner.dto.McpTool;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class McpToolFetcher {

    private static final Logger log = LoggerFactory.getLogger(McpToolFetcher.class);
    private static final String MCP_PROTOCOL_VERSION = "2025-06-18";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicLong reqId = new AtomicLong(0);

    public McpToolFetcher(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public List<McpTool> fetch(String targetUrl, String apiKey) {
        String sessionId = initialize(targetUrl, apiKey);
        return listTools(targetUrl, apiKey, sessionId);
    }

    private String initialize(String targetUrl, String apiKey) {
        Map<String, Object> body = Map.of(
                "jsonrpc", "2.0",
                "id", reqId.incrementAndGet(),
                "method", "initialize",
                "params", Map.of(
                        "protocolVersion", MCP_PROTOCOL_VERSION,
                        "capabilities", Map.of(),
                        "clientInfo", Map.of("name", "mcp-lab-scanner", "version", "0.0.1")
                )
        );

        ResponseEntity<byte[]> response = restClient.post()
                .uri(targetUrl)
                .header("Accept", "application/json, text/event-stream")
                .header("Content-Type", "application/json")
                .header("MCP-Protocol-Version", MCP_PROTOCOL_VERSION)
                .header("X-API-Key", apiKey)
                .body(body)
                .retrieve()
                .toEntity(byte[].class);

        HttpHeaders headers = response.getHeaders();
        List<String> sessionIds = headers.get("Mcp-Session-Id");
        if (sessionIds == null || sessionIds.isEmpty()) {
            sessionIds = headers.get("mcp-session-id");
        }
        if (sessionIds == null || sessionIds.isEmpty()) {
            String body0 = response.getBody() == null ? "" : new String(response.getBody(), StandardCharsets.UTF_8);
            throw new IllegalStateException("initialize returned no Mcp-Session-Id. body=" + body0);
        }
        return sessionIds.get(0);
    }

    private List<McpTool> listTools(String targetUrl, String apiKey, String sessionId) {
        Map<String, Object> body = Map.of(
                "jsonrpc", "2.0",
                "id", reqId.incrementAndGet(),
                "method", "tools/list",
                "params", Map.of()
        );

        byte[] bytes = restClient.post()
                .uri(targetUrl)
                .header("Accept", "application/json, text/event-stream")
                .header("Content-Type", "application/json")
                .header("MCP-Protocol-Version", MCP_PROTOCOL_VERSION)
                .header("X-API-Key", apiKey)
                .header("Mcp-Session-Id", sessionId)
                .body(body)
                .retrieve()
                .body(byte[].class);

        String raw = bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
        String json = extractJson(raw);

        List<McpTool> tools = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode toolsNode = root.path("result").path("tools");
            if (toolsNode.isArray()) {
                for (JsonNode t : toolsNode) {
                    tools.add(new McpTool(
                            t.path("name").asText(""),
                            t.path("title").asText(""),
                            t.path("description").asText(""),
                            t.path("inputSchema")
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse tools/list response: {}", e.getMessage());
        }
        log.info("fetched {} tools from {}", tools.size(), targetUrl);
        return tools;
    }

    private static String extractJson(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        if (!body.contains("data:")) {
            return body;
        }
        StringBuilder sb = new StringBuilder();
        for (String line : body.split("\\R")) {
            if (line.startsWith("data:")) {
                sb.append(line.substring(5).trim());
            }
        }
        String joined = sb.toString();
        return joined.isEmpty() ? body : joined;
    }
}
