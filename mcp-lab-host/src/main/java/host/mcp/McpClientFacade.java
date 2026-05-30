package host.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import host.llm.ToolDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class McpClientFacade {

    private static final Logger log = LoggerFactory.getLogger(McpClientFacade.class);
    private static final String MCP_PROTOCOL_VERSION = "2025-06-18";

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicLong reqId = new AtomicLong(0);

    private final String serverUrl;
    private final String apiKey;

    public McpClientFacade(
            @Value("${host.mcp.server-url}") String serverUrl,
            @Value("${host.mcp.api-key}") String apiKey
    ) {
        this.serverUrl = serverUrl;
        this.apiKey = apiKey;
    }

    public String initialize() {
        Map<String, Object> body = Map.of(
                "jsonrpc", "2.0",
                "id", reqId.incrementAndGet(),
                "method", "initialize",
                "params", Map.of(
                        "protocolVersion", MCP_PROTOCOL_VERSION,
                        "capabilities", Map.of(),
                        "clientInfo", Map.of("name", "mcp-lab-host", "version", "0.0.1")
                )
        );

        ResponseEntity<byte[]> response = restClient.post()
                .uri(serverUrl)
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
        String sessionId = sessionIds.get(0);
        log.info("MCP session opened: {}", sessionId);
        return sessionId;
    }

    public List<ToolDescriptor> listTools(String sessionId) {
        Map<String, Object> body = Map.of(
                "jsonrpc", "2.0",
                "id", reqId.incrementAndGet(),
                "method", "tools/list",
                "params", Map.of()
        );
        String raw = postWithSession(body, sessionId);
        log.info("tools/list RAW response (len={}): {}",
                raw == null ? -1 : raw.length(),
                raw == null ? "<null>" : raw.substring(0, Math.min(800, raw.length())));
        String json = extractJson(raw);
        log.info("tools/list extracted json (len={}): {}",
                json == null ? -1 : json.length(),
                json == null ? "<null>" : json.substring(0, Math.min(800, json.length())));
        List<ToolDescriptor> tools = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode toolsNode = root.path("result").path("tools");
            log.info("tools/list parsed: result.tools is array={}, size={}",
                    toolsNode.isArray(), toolsNode.size());
            if (toolsNode.isArray()) {
                for (JsonNode t : toolsNode) {
                    JsonNode schemaNode = t.path("inputSchema");
                    String schemaJson = schemaNode.isMissingNode() || schemaNode.isNull()
                            ? "{\"type\":\"object\",\"properties\":{}}"
                            : schemaNode.toString();
                    tools.add(new ToolDescriptor(
                            t.path("name").asText(""),
                            t.path("title").asText(""),
                            t.path("description").asText(""),
                            schemaJson
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse tools/list response: {}", e.getMessage(), e);
        }
        return tools;
    }

    public ToolCallResult callTool(String sessionId, String toolName, Map<String, Object> arguments) {
        Map<String, Object> body = Map.of(
                "jsonrpc", "2.0",
                "id", reqId.incrementAndGet(),
                "method", "tools/call",
                "params", Map.of(
                        "name", toolName,
                        "arguments", arguments == null ? Map.of() : arguments
                )
        );
        String raw = postWithSession(body, sessionId);
        String json = extractJson(raw);
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode result = root.path("result");
            boolean isError = result.path("isError").asBoolean(false);
            JsonNode contentArr = result.path("content");
            String text = "";
            if (contentArr.isArray() && contentArr.size() > 0) {
                text = contentArr.get(0).path("text").asText("");
            }
            return new ToolCallResult(isError, text, raw);
        } catch (Exception e) {
            log.warn("Failed to parse tools/call response: {}", e.getMessage());
            return new ToolCallResult(true, "<parse error: " + e.getMessage() + ">", raw);
        }
    }

    private String postWithSession(Map<String, Object> body, String sessionId) {
        byte[] bytes = restClient.post()
                .uri(serverUrl)
                .header("Accept", "application/json, text/event-stream")
                .header("Content-Type", "application/json")
                .header("MCP-Protocol-Version", MCP_PROTOCOL_VERSION)
                .header("X-API-Key", apiKey)
                .header("Mcp-Session-Id", sessionId)
                .body(body)
                .retrieve()
                .body(byte[].class);
        return bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8);
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
