package mock_backend.api;

import mock_backend.domain.StoreDetail;
import mock_backend.domain.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class PublicStoreController {

    private static final Logger log = LoggerFactory.getLogger(PublicStoreController.class);

    private final StoreRepository repository;
    private final boolean btBackendAuthzEnabled;

    public PublicStoreController(
            StoreRepository repository,
            @Value("${bt.backend-authz.enabled:false}") boolean btBackendAuthzEnabled
    ) {
        this.repository = repository;
        this.btBackendAuthzEnabled = btBackendAuthzEnabled;
    }

    @GetMapping("/stores/{id}")
    public ResponseEntity<?> getStore(@PathVariable String id) {
        log.info("GET /stores/{} (bt={})", id, btBackendAuthzEnabled ? "ON" : "OFF");
        Optional<StoreDetail> found = repository.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.status(404).body(ErrorBody.of("NOT_FOUND", "store not found"));
        }
        StoreDetail store = found.get();
        if (btBackendAuthzEnabled && store.restricted()) {
            log.warn("BT-001: refusing to serve restricted store '{}' on unauth endpoint", id);
            return ResponseEntity.status(403).body(ErrorBody.of("RESTRICTED", "store is restricted; use /secure/stores/{id} with valid bearer token."));
        }
        return ResponseEntity.ok(store);
    }
}
