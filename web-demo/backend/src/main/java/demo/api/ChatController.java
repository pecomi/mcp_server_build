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
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final RestClient restClient;
    private final String hostUrl;

    public ChatController(
            RestClient.Builder restClientBuilder,
            @Value("${demo.host.url}") String hostUrl
    ) {
        this.restClient = restClientBuilder.build();
        this.hostUrl = hostUrl;
    }

    @PostMapping(value = "/api/chat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> chat(@RequestBody String body) {
        log.info("POST /api/chat → {}/run", hostUrl);
        ResponseEntity<String> resp = restClient.post()
                .uri(hostUrl + "/run")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);
        return ResponseEntity.status(resp.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(resp.getBody());
    }
}
