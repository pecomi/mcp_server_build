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
}
