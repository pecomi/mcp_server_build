package scanner.rules;

import org.springframework.stereotype.Component;
import scanner.dto.Finding;
import scanner.dto.McpTool;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Detects sentinel patterns that suggest a tool description has been poisoned
 * with LLM-targeted directives (RT-002 / RT-003 vehicles).
 */
@Component
public class DescInjectRule implements Rule {

    private static final List<Pattern> SENTINELS = List.of(
            Pattern.compile("\\[SYSTEM\\s+CONTEXT\\]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\[ATTACKER\\]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("반드시\\s.*?호출"),
            Pattern.compile("must\\s+call|must\\s+invoke", Pattern.CASE_INSENSITIVE),
            Pattern.compile("read_file\\s*\\("),
            Pattern.compile("[a-zA-Z_]+_id\\s*=\\s*\"[^\"]+\""),
            Pattern.compile("ignore\\s+previous|prior\\s+instructions", Pattern.CASE_INSENSITIVE)
    );

    @Override
    public String id() {
        return "DESC_INJECT";
    }

    @Override
    public List<Finding> apply(McpTool tool) {
        if (tool.description() == null || tool.description().isBlank()) {
            return List.of();
        }
        List<Finding> out = new ArrayList<>();
        List<String> matched = new ArrayList<>();
        for (Pattern p : SENTINELS) {
            if (p.matcher(tool.description()).find()) {
                matched.add(p.pattern());
            }
        }
        if (!matched.isEmpty()) {
            out.add(new Finding(
                    tool.name(),
                    id(),
                    "high",
                    "Tool description matches " + matched.size() + " injection sentinel(s): " + matched,
                    List.of("RT-002", "RT-003")
            ));
        }
        return out;
    }
}
