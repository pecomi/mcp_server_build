package mcp_server.adapter;

import mcp_server.dto.StoreListRequest;
import mcp_server.dto.StoreListResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockGongGongNuriApiClient {

    public StoreListResponse getStores(StoreListRequest request) {
        return new StoreListResponse(
                2,
                List.of(
                        new StoreListResponse.StoreSummary(
                                "STORE-001",
                                "서울 공공 회의실 A",
                                "11",
                                "강남구",
                                true,
                                "회의실"
                        ),
                        new StoreListResponse.StoreSummary(
                                "STORE-002",
                                "서울 체육시설 B",
                                "11",
                                "송파구",
                                true,
                                "체육시설"
                        )
                )
        );
    }
}