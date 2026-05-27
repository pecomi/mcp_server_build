package host.llm;

import java.util.List;

public interface LlmClient {

    LlmDecision decide(String prompt, String scenarioId, List<ToolDescriptor> availableTools);

    /**
     * Multi-step plan. Default implementation wraps single-step decide() in a one-element list.
     * Cross-server / multi-tool scenarios (e.g., RT-003) override this to return a sequence of calls.
     */
    default List<LlmDecision> decideMultiStep(String prompt, String scenarioId, List<ToolDescriptor> availableTools) {
        return List.of(decide(prompt, scenarioId, availableTools));
    }

    /**
     * Optional replanning hook after a tool result enters the model context.
     * Output-poisoning scenarios override this to model a client that treats
     * untrusted tool output as follow-up instructions.
     */
    default List<LlmDecision> decideAfterToolResult(
            String prompt,
            String scenarioId,
            List<ToolDescriptor> availableTools,
            List<LlmDecision> executedPlan,
            String lastResult,
            boolean lastResultIsError
    ) {
        return List.of();
    }

    static boolean hasExecutedTool(List<LlmDecision> executedPlan, String canonical) {
        if (executedPlan == null) {
            return false;
        }
        return executedPlan.stream()
                .map(LlmDecision::toolName)
                .anyMatch(name -> name != null && (name.equals(canonical) || name.endsWith("_" + canonical)));
    }
}
