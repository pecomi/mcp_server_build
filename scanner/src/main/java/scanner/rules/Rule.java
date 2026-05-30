package scanner.rules;

import scanner.dto.Finding;
import scanner.dto.McpTool;

import java.util.List;

public interface Rule {
    String id();
    List<Finding> apply(McpTool tool);
}
