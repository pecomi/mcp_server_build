package mcp_server.dto;

public record ExternalInstitutionRecordRequest(
        String name,
        String residentRegistrationNumber
) {
}
