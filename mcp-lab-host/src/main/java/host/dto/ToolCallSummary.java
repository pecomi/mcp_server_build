package host.dto;

import java.util.Map;

public record ToolCallSummary(
        String name,
        Map<String, Object> arguments,
        String result,
        boolean isError
) {}
