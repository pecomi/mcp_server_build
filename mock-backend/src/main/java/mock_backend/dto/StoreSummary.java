package mock_backend.dto;

public record StoreSummary(
        String storeId,
        String name,
        String sido,
        String sigungu,
        boolean free,
        String category
) {}
