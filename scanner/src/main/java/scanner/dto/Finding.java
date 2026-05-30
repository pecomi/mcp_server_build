package scanner.dto;

import java.util.List;

public record Finding(
        String tool,
        String rule,
        String severity,
        String evidence,
        List<String> rtCandidates
) {}
