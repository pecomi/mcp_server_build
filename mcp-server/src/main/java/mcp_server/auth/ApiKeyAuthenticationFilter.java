package mcp_server.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String TOOLS_CALL_METHOD = "tools/call";

    private final ApiKeyPolicyService apiKeyPolicyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiKeyAuthenticationFilter(ApiKeyPolicyService apiKeyPolicyService) {
        this.apiKeyPolicyService = apiKeyPolicyService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // MCP endpoint만 인증 대상으로 둔다.
        return !path.startsWith("/mcp");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);

        try {
            String apiKey = cachedRequest.getHeader(API_KEY_HEADER);

            Optional<AuthenticatedClient> clientOptional =
                    apiKeyPolicyService.findByApiKey(apiKey);

            if (clientOptional.isEmpty()) {
                writeUnauthorized(response, "INVALID_API_KEY", "API Key가 없거나 유효하지 않습니다.");
                return;
            }

            AuthenticatedClient client = clientOptional.get();

            if (!client.isActive()) {
                writeUnauthorized(response, "INACTIVE_API_KEY", "비활성화된 API Key입니다.");
                return;
            }

            String requestedToolName = extractToolNameIfToolCall(cachedRequest);

            if (requestedToolName != null && !client.canCallTool(requestedToolName)) {
                writeForbidden(
                        response,
                        "FORBIDDEN_TOOL",
                        "이 API Key는 " + requestedToolName + " Tool을 호출할 권한이 없습니다."
                );
                return;
            }

            ApiKeyContext.set(client);

            // 중요: 원본 request가 아니라 cachedRequest를 넘겨야 MCP SDK가 body를 다시 읽을 수 있다.
            filterChain.doFilter(cachedRequest, response);

        } finally {
            ApiKeyContext.clear();
        }
    }

    private String extractToolNameIfToolCall(CachedBodyHttpServletRequest request) {
        try {
            String body = request.getCachedBodyAsString();

            if (body == null || body.isBlank()) {
                return null;
            }

            JsonNode root = objectMapper.readTree(body);

            // 현재 구현은 단일 JSON-RPC object 기준.
            // JSON-RPC batch 요청 배열이 들어오면 별도 처리가 필요하다.
            if (!root.isObject()) {
                return null;
            }

            JsonNode methodNode = root.get("method");

            if (methodNode == null || !TOOLS_CALL_METHOD.equals(methodNode.asText())) {
                return null;
            }

            JsonNode paramsNode = root.get("params");

            if (paramsNode == null || !paramsNode.isObject()) {
                return null;
            }

            JsonNode nameNode = paramsNode.get("name");

            if (nameNode == null || nameNode.asText().isBlank()) {
                return null;
            }

            return nameNode.asText();

        } catch (Exception e) {
            // body parsing 실패는 여기서 차단하지 않는다.
            // MCP SDK가 원래대로 invalid request를 처리하게 둔다.
            return null;
        }
    }

    private void writeUnauthorized(
            HttpServletResponse response,
            String code,
            String message
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write("""
                {
                  "error": {
                    "code": "%s",
                    "message": "%s"
                  }
                }
                """.formatted(code, message));
    }

    private void writeForbidden(
            HttpServletResponse response,
            String code,
            String message
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write("""
                {
                  "error": {
                    "code": "%s",
                    "message": "%s"
                  }
                }
                """.formatted(code, message));
    }
}