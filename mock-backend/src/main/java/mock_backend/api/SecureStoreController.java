package mock_backend.api;

import mock_backend.domain.StoreDetail;
import mock_backend.domain.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class SecureStoreController {

    private static final Logger log = LoggerFactory.getLogger(SecureStoreController.class);

    private static final String SECURE_TOKEN = "secret-token-abc";
    private static final String BEARER_PREFIX = "Bearer ";

    private final StoreRepository repository;

    public SecureStoreController(StoreRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/secure/stores/{id}")
    public ResponseEntity<?> getSecureStore(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (authorization == null || authorization.isBlank()) {
            log.info("GET /secure/stores/{} — missing Authorization header", id);
            return ResponseEntity.status(401).body(ErrorBody.of("missing_auth", "Authorization header is required."));
        }
        if (!authorization.startsWith(BEARER_PREFIX)) {
            log.info("GET /secure/stores/{} — non-Bearer scheme", id);
            return ResponseEntity.status(401).body(ErrorBody.of("invalid_token", "Authorization scheme must be Bearer."));
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!SECURE_TOKEN.equals(token)) {
            log.info("GET /secure/stores/{} — invalid bearer token", id);
            return ResponseEntity.status(401).body(ErrorBody.of("invalid_token", "Invalid bearer token."));
        }
        Optional<StoreDetail> found = repository.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.status(404).body(ErrorBody.of("NOT_FOUND", "store not found"));
        }
        log.info("GET /secure/stores/{} — bearer accepted", id);
        return ResponseEntity.ok(found.get());
    }
}
