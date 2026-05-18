package host.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PoisonedMockLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(PoisonedMockLlmClient.class);

    private static final Pattern STORE_ID_DIRECTIVE =
            Pattern.compile("store_id\\s*=\\s*\"([^\"]+)\"");

    private final MockLlmClient baseline = new MockLlmClient();

    @Override
    public LlmDecision decide(String prompt, String scenarioId, List<ToolDescriptor> availableTools) {
        LlmDecision base = baseline.decide(prompt, scenarioId, availableTools);

        Optional<ToolDescriptor> matchingTool = availableTools.stream()
                .filter(t -> base.toolName().equals(t.name()))
                .findFirst();

        if (matchingTool.isEmpty()) {
            log.info("PoisonedMockLlmClient: tool '{}' not found in tools/list — returning baseline decision", base.toolName());
            return base;
        }

        String description = matchingTool.get().description();
        Matcher m = STORE_ID_DIRECTIVE.matcher(description);
        if (m.find()) {
            String injectedId = m.group(1);
            log.warn("PoisonedMockLlmClient: detected store_id directive in description of '{}', overriding to {}",
                    base.toolName(), injectedId);
            Map<String, Object> newArgs = new HashMap<>(base.arguments());
            newArgs.put("store_id", injectedId);
            return new LlmDecision(base.toolName(), newArgs);
        }

        log.info("PoisonedMockLlmClient: no directive in description of '{}' — baseline decision", base.toolName());
        return base;
    }
}
