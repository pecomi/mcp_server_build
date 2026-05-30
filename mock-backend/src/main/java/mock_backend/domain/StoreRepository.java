package mock_backend.domain;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class StoreRepository {

    private final Map<String, StoreDetail> stores;

    public StoreRepository() {
        Map<String, StoreDetail> map = new LinkedHashMap<>();
        for (StoreDetail s : seed()) {
            map.put(s.id(), s);
        }
        this.stores = Map.copyOf(map);
    }

    public Optional<StoreDetail> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(stores.get(id));
    }

    public List<StoreDetail> findPublic() {
        return stores.values().stream()
                .filter(s -> !s.restricted() && "PUBLISHED".equals(s.status()))
                .toList();
    }

    private static List<StoreDetail> seed() {
        return List.of(
                new StoreDetail(
                        "STORE-001", "서울 공공 회의실 A",
                        "11", "강남구", "회의실",
                        true, "PUBLISHED", false,
                        null, null, null
                ),
                new StoreDetail(
                        "STORE-002", "서울 체육시설 B",
                        "11", "송파구", "체육시설",
                        true, "PUBLISHED", false,
                        null, null, null
                ),
                new StoreDetail(
                        "STORE-003", "강남 도서관 C",
                        "11", "강남구", "도서관",
                        true, "PUBLISHED", false,
                        null, null, null
                ),
                new StoreDetail(
                        "STORE-INTERNAL-001", "정부청사 회의실 9F",
                        "11", "종로구", "회의실",
                        false, "PUBLISHED", true,
                        "김OO", "02-XXXX-1234", "VIP 의전용, 외부 노출 금지"
                ),
                new StoreDetail(
                        "STORE-INTERNAL-002", "군 시설 체육관 B동",
                        "11", "용산구", "체육시설",
                        false, "PUBLISHED", true,
                        "박OO", "02-XXXX-5678", "군 관계자 전용"
                ),
                new StoreDetail(
                        "STORE-DRAFT-001", "(미정) 신축 회의실 D",
                        "11", "마포구", "회의실",
                        true, "DRAFT", true,
                        "이OO", "02-XXXX-9012", "2026-Q3 발행 예정, 사전 검토 중"
                )
        );
    }
}
