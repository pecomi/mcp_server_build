package mcp_server.validation;

import mcp_server.dto.StoreListRequest;
import org.springframework.stereotype.Component;

@Component
public class StoreListValidator {

    public void validate(StoreListRequest request) {
        if (request.page() == null || request.page() < 1) {
            throw new IllegalArgumentException("page는 1 이상이어야 합니다.");
        }

        if (request.size() == null || request.size() < 1 || request.size() > 200) {
            throw new IllegalArgumentException("size는 1 이상 200 이하이어야 합니다.");
        }

        if (request.consumerCd() == null || request.consumerCd().isBlank()) {
            throw new IllegalArgumentException("consumerCd는 필수입니다.");
        }

        if (request.searchFreeYn() != null &&
                !request.searchFreeYn().equals("Y") &&
                !request.searchFreeYn().equals("N")) {
            throw new IllegalArgumentException("searchFreeYn은 Y 또는 N이어야 합니다.");
        }
    }
}