package scanner.rules;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import scanner.dto.Finding;
import scanner.dto.McpTool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Flags string args with security-sensitive names (path/*_id/file/url/target)
 * that lack a pattern or enum constraint. Targets RT-003 / RT-004 sink vectors
 * (path traversal, IDOR-like by-id lookups).
 */
@Component
public class ArgNoPatternRule implements Rule {

    private static final List<String> SENSITIVE_NAMES = List.of(
            "path", "file", "url", "target", "store_id", "user_id", "id"
    );

    @Override
    public String id() {
        return "ARG_NO_PATTERN";
    }

    @Override
    public List<Finding> apply(McpTool tool) {
        JsonNode schema = tool.inputSchema();
        if (schema == null || schema.isMissingNode() || !schema.has("properties")) {
            return List.of();
        }
        JsonNode props = schema.path("properties");
        List<Finding> out = new ArrayList<>();

        Iterator<Map.Entry<String, JsonNode>> it = props.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            String argName = entry.getKey();
            JsonNode arg = entry.getValue();

            String type = arg.path("type").asText("");
            if (!"string".equalsIgnoreCase(type)) {
                continue;
            }
            if (!isSensitive(argName)) {
                continue;
            }
            boolean hasPattern = arg.has("pattern") && !arg.path("pattern").asText("").isBlank();
            boolean hasEnum = arg.has("enum") && arg.path("enum").isArray() && arg.path("enum").size() > 0;
            if (hasPattern || hasEnum) {
                continue;
            }
            out.add(new Finding(
                    tool.name(),
                    id(),
                    "high",
                    "Arg '" + argName + "' is string with no pattern/enum constraint; security-sensitive name suggests injection sink.",
                    List.of("RT-002", "RT-003", "RT-004")
            ));
        }
        return out;
    }

    private static boolean isSensitive(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        for (String s : SENSITIVE_NAMES) {
            if (lower.equals(s) || lower.endsWith("_" + s)) {
                return true;
            }
        }
        return false;
    }
}
