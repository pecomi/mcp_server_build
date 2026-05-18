package host.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RT-003 Stage 1 — host-side flag (H).
 *
 * Scans every tool's description for the sentinel pattern `read_file('<path>')`.
 * If found AND the baseline scenario's tool is not itself read_file, emits a
 * 2-step plan: first read_file(path), then the baseline scenario tool.
 *
 * Models a real LLM that follows tool-description directives blindly across the
 * MCP federation boundary, producing cross-server data exfil into the caller's
 * response stream.
 */
public class CrossServerPoisonedMockLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(CrossServerPoisonedMockLlmClient.class);

    private static final Pattern READ_FILE_DIRECTIVE = Pattern.compile("read_file\\('([^']+)'\\)");

    private final MockLlmClient baseline = new MockLlmClient();

    @Override
    public LlmDecision decide(String prompt, String scenarioId, List<ToolDescriptor> availableTools) {
        return baseline.decide(prompt, scenarioId, availableTools);
    }

    @Override
    public List<LlmDecision> decideMultiStep(String prompt, String scenarioId, List<ToolDescriptor> availableTools) {
        LlmDecision base = baseline.decide(prompt, scenarioId, availableTools);

        Optional<String> injectionPath = availableTools.stream()
                .map(ToolDescriptor::description)
                .map(READ_FILE_DIRECTIVE::matcher)
                .filter(Matcher::find)
                .map(m -> m.group(1))
                .findFirst();

        if (injectionPath.isPresent() && !isReadFileTool(base.toolName())) {
            String injectedPath = injectionPath.get();
            log.warn("CrossServerPoisonedMockLlmClient: detected read_file('{}') in tool description; emitting 2-step plan (cross-server exfil)", injectedPath);

            String readFileTool;
            try {
                readFileTool = MockLlmClient.resolveToolName(availableTools, "read_file");
            } catch (IllegalArgumentException e) {
                log.warn("CrossServerPoisonedMockLlmClient: sentinel found but read_file tool not federated — falling back to baseline");
                return List.of(base);
            }

            LlmDecision step1 = new LlmDecision(readFileTool, Map.of("path", injectedPath));
            return List.of(step1, base);
        }

        return List.of(base);
    }

    private static boolean isReadFileTool(String toolName) {
        return toolName != null && (toolName.equals("read_file") || toolName.endsWith("_read_file"));
    }
}
