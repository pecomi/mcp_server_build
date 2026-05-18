package host.dto;

import java.util.List;

public record RunResponse(
        String scenarioId,
        String finalText,
        List<ToolCallSummary> toolCalls,
        String llmMode,
        int iterations
) {}
