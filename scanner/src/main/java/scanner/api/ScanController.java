package scanner.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import scanner.dto.ScanRequest;
import scanner.dto.ScanResponse;
import scanner.service.ScannerService;

@RestController
public class ScanController {

    private final ScannerService scannerService;
    private final String defaultTarget;
    private final String defaultApiKey;

    public ScanController(
            ScannerService scannerService,
            @Value("${scanner.default-target}") String defaultTarget,
            @Value("${scanner.default-api-key}") String defaultApiKey
    ) {
        this.scannerService = scannerService;
        this.defaultTarget = defaultTarget;
        this.defaultApiKey = defaultApiKey;
    }

    @PostMapping("/scan")
    public ScanResponse scan(@RequestBody(required = false) ScanRequest request) {
        String target = (request == null || isBlank(request.targetUrl())) ? defaultTarget : request.targetUrl();
        String key = (request == null || isBlank(request.apiKey())) ? defaultApiKey : request.apiKey();
        return scannerService.scan(target, key);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
