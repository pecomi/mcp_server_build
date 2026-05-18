package host.llm;

import java.util.Map;

public record LlmDecision(
        String toolName,
        Map<String, Object> arguments
) {}
