package mcp_server.dto;

public record ExternalInstitutionRecordRequest(
        String institutionCode,
        String recordId,
        String consumerCd
) {
}
