package host.dto;

public record RunRequest(
        String scenarioId,
        String prompt
) {}
