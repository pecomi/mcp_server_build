package mock_backend.api;

import mock_backend.domain.StoreDetail;
import mock_backend.domain.StoreRepository;
import mock_backend.dto.StoreListResponse;
import mock_backend.dto.StoreSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PublicStoreListController {

    private static final Logger log = LoggerFactory.getLogger(PublicStoreListController.class);

    private final StoreRepository repository;

    public PublicStoreListController(StoreRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/stores")
    public StoreListResponse listStores(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sido,
            @RequestParam(required = false) String sigungu,
            @RequestParam(required = false) String searchFreeYn,
            @RequestParam(required = false) String searchSbclsCd,
            @RequestParam(required = false) String searchMnclsCd,
            @RequestParam(required = false) String consumerCd
    ) {
        log.info("GET /stores filter sido={} sigungu={} free={} sbcls={} mncls={} page={} size={}",
                sido, sigungu, searchFreeYn, searchSbclsCd, searchMnclsCd, page, size);

        List<StoreDetail> all = repository.findPublic();

        List<StoreSummary> filtered = all.stream()
                .filter(s -> nullOrBlank(sido) || sido.equals(s.sido()))
                .filter(s -> nullOrBlank(sigungu) || sigungu.equals(s.sigungu()))
                .filter(s -> {
                    if (nullOrBlank(searchFreeYn)) {
                        return true;
                    }
                    boolean wantFree = "Y".equalsIgnoreCase(searchFreeYn);
                    return wantFree == s.isFree();
                })
                .map(PublicStoreListController::toSummary)
                .toList();

        List<StoreSummary> paged = applyPaging(filtered, page, size);
        return new StoreListResponse(paged.size(), paged);
    }

    private static boolean nullOrBlank(String v) {
        return v == null || v.isBlank();
    }

    private static StoreSummary toSummary(StoreDetail d) {
        return new StoreSummary(d.id(), d.name(), d.sido(), d.sigungu(), d.isFree(), d.category());
    }

    private static List<StoreSummary> applyPaging(List<StoreSummary> all, Integer page, Integer size) {
        if (page == null || size == null || page < 1 || size < 1) {
            return all;
        }
        int from = Math.min((page - 1) * size, all.size());
        int to = Math.min(from + size, all.size());
        return all.subList(from, to);
    }
}
