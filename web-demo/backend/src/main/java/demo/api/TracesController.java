package demo.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class TracesController {

    private static final Logger log = LoggerFactory.getLogger(TracesController.class);

    private final RestClient restClient;
    private final String jaegerUrl;

    public TracesController(
            RestClient.Builder restClientBuilder,
            @Value("${demo.jaeger.url}") String jaegerUrl
    ) {
        this.restClient = restClientBuilder.build();
        this.jaegerUrl = jaegerUrl;
    }

    @GetMapping(value = "/api/traces", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> traces(
            @RequestParam(defaultValue = "mcp-lab-host") String service,
            @RequestParam(defaultValue = "10") int limit
    ) {
        String url = jaegerUrl + "/api/traces?service=" + service + "&limit=" + limit;
        log.debug("GET /api/traces → {}", url);
        try {
            String body = restClient.get().uri(url).retrieve().body(String.class);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
        } catch (Exception e) {
            log.warn("failed to fetch traces: {}", e.getMessage());
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("{\"data\":[]}");
        }
    }
}
