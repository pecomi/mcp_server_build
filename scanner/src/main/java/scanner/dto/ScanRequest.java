package scanner.dto;

public record ScanRequest(
        String targetUrl,
        String apiKey
) {}
