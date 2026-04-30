package mcp_server.auth;

import java.util.List;

public record AuthenticatedClient(
        String apiKey,
        String clientId,
        String status,
        List<String> allowedTools
) {
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    public boolean canCallTool(String toolName) {
        return allowedTools != null && allowedTools.contains(toolName);
    }
}