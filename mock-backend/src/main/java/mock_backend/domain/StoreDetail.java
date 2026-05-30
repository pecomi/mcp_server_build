package mock_backend.domain;

public record StoreDetail(
        String id,
        String name,
        String sido,
        String sigungu,
        String category,
        boolean isFree,
        String status,
        boolean restricted,
        String operatorName,
        String operatorPhone,
        String internalNotes
) {}
