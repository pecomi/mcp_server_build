package scanner.dto;

import java.util.List;

public record ScanResponse(
        String targetUrl,
        int scannedTools,
        List<Finding> findings
) {}
