package mcp_server.auth;

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

    private final ApiKeyPolicyService apiKeyPolicyService;

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
        try {
            String apiKey = request.getHeader(API_KEY_HEADER);

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

            ApiKeyContext.set(client);
            filterChain.doFilter(request, response);

        } finally {
            ApiKeyContext.clear();
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
}