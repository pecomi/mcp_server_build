package scanner.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record McpTool(
        String name,
        String title,
        String description,
        JsonNode inputSchema
) {}
