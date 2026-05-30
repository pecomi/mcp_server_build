package demo.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class ScanController {

    private static final Logger log = LoggerFactory.getLogger(ScanController.class);

    private final RestClient restClient;
    private final String scannerUrl;

    public ScanController(
            RestClient.Builder restClientBuilder,
            @Value("${demo.scanner.url}") String scannerUrl
    ) {
        this.restClient = restClientBuilder.build();
        this.scannerUrl = scannerUrl;
    }

    @PostMapping(value = "/api/scan", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> scan(@RequestBody(required = false) String body) {
        String payload = (body == null || body.isBlank()) ? "{}" : body;
        log.info("POST /api/scan → {}/scan", scannerUrl);
        try {
            ResponseEntity<String> resp = restClient.post()
                    .uri(scannerUrl + "/scan")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toEntity(String.class);
            return ResponseEntity.status(resp.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(resp.getBody());
        } catch (Exception e) {
            log.warn("scan failed: {}", e.getMessage());
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body("{\"targetUrl\":\"" + scannerUrl + "\",\"scannedTools\":0,\"findings\":[]}");
        }
    }
}
