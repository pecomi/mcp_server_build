package mcp_server.dto;

import java.util.List;

public record StoreListResponse(
        int count,
        List<StoreSummary> stores
) {
    public record StoreSummary(
            String storeId,
            String name,
            String sido,
            String sigungu,
            boolean free,
            String category
    ) {
    }
}