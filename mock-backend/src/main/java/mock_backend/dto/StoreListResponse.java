package mock_backend.dto;

import java.util.List;

public record StoreListResponse(
        int count,
        List<StoreSummary> stores
) {}
