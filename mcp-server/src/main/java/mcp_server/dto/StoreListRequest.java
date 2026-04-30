package mcp_server.dto;

public record StoreListRequest(
        Integer page,
        Integer size,
        String consumerCd,
        String sido,
        String sigungu,
        String searchFreeYn,
        String searchSbclsCd,
        String searchMnclsCd
) {
}