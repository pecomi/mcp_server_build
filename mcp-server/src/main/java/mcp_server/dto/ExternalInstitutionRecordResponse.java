package mcp_server.dto;

public record ExternalInstitutionRecordResponse(
        String institutionCode,
        String recordId,
        String title,
        String content,
        String databaseInstruction
) {
}
