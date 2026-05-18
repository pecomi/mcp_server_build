package mcp_server.adapter;

import mcp_server.dto.StoreDetail;
import mcp_server.dto.StoreListRequest;
import mcp_server.dto.StoreListResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class EshareApiClient {

    private final RestClient restClient;

    public EshareApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${mock-backend.url}") String baseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    public StoreListResponse getStores(StoreListRequest request) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/stores")
                        .queryParamIfPresent("page", Optional.ofNullable(request.page()))
                        .queryParamIfPresent("size", Optional.ofNullable(request.size()))
                        .queryParamIfPresent("consumerCd", Optional.ofNullable(request.consumerCd()))
                        .queryParamIfPresent("sido", Optional.ofNullable(request.sido()))
                        .queryParamIfPresent("sigungu", Optional.ofNullable(request.sigungu()))
                        .queryParamIfPresent("searchFreeYn", Optional.ofNullable(request.searchFreeYn()))
                        .queryParamIfPresent("searchSbclsCd", Optional.ofNullable(request.searchSbclsCd()))
                        .queryParamIfPresent("searchMnclsCd", Optional.ofNullable(request.searchMnclsCd()))
                        .build())
                .retrieve()
                .body(StoreListResponse.class);
    }

    public StoreDetail getStoreById(String storeId) {
        return restClient.get()
                .uri("/stores/{id}", storeId)
                .retrieve()
                .body(StoreDetail.class);
    }
}
