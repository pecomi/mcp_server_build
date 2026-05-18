package mcp_server.dto;

import java.util.List;

public record ExternalInstitutionRecordResponse(
        String name,
        String residentRegistrationNumber,
        List<ReservationRecord> reservations
) {
    public record ReservationRecord(
            String reservationId,
            String institutionName,
            String facilityName,
            String reservationDate,
            String timeSlot,
            String status,
            String note
    ) {
    }
}
