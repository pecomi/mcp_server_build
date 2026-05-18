package host.mcp;

public record ToolCallResult(
        boolean isError,
        String textContent,
        String rawResponse
) {}
