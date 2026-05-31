package host.llm;

import java.util.List;
import java.util.Map;

public class MockLlmClient implements LlmClient {

    @Override
    public LlmDecision decide(String prompt, String scenarioId, List<ToolDescriptor> availableTools) {
        if (scenarioId == null) {
            throw new IllegalArgumentException("scenarioId is required");
        }
        return switch (scenarioId) {
            case "smoke-storedetail" -> new LlmDecision(
                    resolveToolName(availableTools, "getStoreDetail"),
                    Map.of("store_id", "STORE-001")
            );
            case "smoke-storelist" -> new LlmDecision(
                    resolveToolName(availableTools, "getStoreList"),
                    Map.of(
                            "page", 1,
                            "size", 20,
                            "consumerCd", "host-smoke",
                            "sido", "11",
                            "searchFreeYn", "Y"
                    )
            );
            case "rt-002-citizen-self-lookup" -> new LlmDecision(
                    resolveToolName(availableTools, "getStoreDetail"),
                    Map.of("store_id", "STORE-001")
            );
            case "smoke-readfile" -> new LlmDecision(
                    resolveToolName(availableTools, "read_file"),
                    Map.of("path", "/data/welcome.txt")
            );
            case "smoke-lookup" -> new LlmDecision(
                    resolveToolName(availableTools, "lookup_term"),
                    Map.of("term", "MCP")
            );
            case "rt-003-cross-server-lookup" -> new LlmDecision(
                    resolveToolName(availableTools, "lookup_term"),
                    Map.of("term", "MCP")
            );
            case "rt-006-output-poisoned-lookup" -> new LlmDecision(
                    resolveToolName(availableTools, "lookup_term"),
                    Map.of("term", "MCP")
            );
            default -> throw new IllegalArgumentException("Unknown scenarioId: " + scenarioId);
        };
    }

    public static String resolveToolName(List<ToolDescriptor> tools, String canonical) {
        if (tools == null || tools.isEmpty()) {
            throw new IllegalArgumentException(
                    "No tools available from server (tools/list returned empty); cannot resolve '" + canonical + "'."
            );
        }
        return tools.stream()
                .map(ToolDescriptor::name)
                .filter(n -> n != null && (n.equals(canonical) || n.endsWith("_" + canonical)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tool '" + canonical + "' not found among available tools: " +
                                tools.stream().map(ToolDescriptor::name).toList()
                ));
    }
}
