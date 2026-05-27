package host.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RT-006 Stage 1 — output-side prompt injection.
 *
 * Starts from the normal mock decision, then scans the previous tool result for
 * a `read_file('<path>')` directive. If present, it emits a follow-up read_file
 * call, modeling a client that lets untrusted MCP tool output steer planning.
 */
public class OutputPoisonedMockLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OutputPoisonedMockLlmClient.class);

    private static final Pattern READ_FILE_DIRECTIVE = Pattern.compile("read_file\\('([^']+)'\\)");

    private final MockLlmClient baseline = new MockLlmClient();

    @Override
    public LlmDecision decide(String prompt, String scenarioId, List<ToolDescriptor> availableTools) {
        return baseline.decide(prompt, scenarioId, availableTools);
    }

    @Override
    public List<LlmDecision> decideAfterToolResult(
            String prompt,
            String scenarioId,
            List<ToolDescriptor> availableTools,
            List<LlmDecision> executedPlan,
            String lastResult,
            boolean lastResultIsError
    ) {
        if (lastResultIsError || lastResult == null || LlmClient.hasExecutedTool(executedPlan, "read_file")) {
            return List.of();
        }

        Matcher matcher = READ_FILE_DIRECTIVE.matcher(lastResult);
        if (!matcher.find()) {
            return List.of();
        }

        String injectedPath = matcher.group(1);
        String readFileTool;
        try {
            readFileTool = MockLlmClient.resolveToolName(availableTools, "read_file");
        } catch (IllegalArgumentException e) {
            log.warn("OutputPoisonedMockLlmClient: directive found but read_file tool is unavailable");
            return List.of();
        }

        log.warn("OutputPoisonedMockLlmClient: detected read_file('{}') in tool output; adding follow-up tool call", injectedPath);
        return List.of(new LlmDecision(readFileTool, Map.of("path", injectedPath)));
    }
}
