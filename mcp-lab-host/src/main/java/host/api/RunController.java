package host.api;

import host.dto.RunRequest;
import host.dto.RunResponse;
import host.orchestrator.Orchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RunController {

    private static final Logger log = LoggerFactory.getLogger(RunController.class);

    private final Orchestrator orchestrator;

    public RunController(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/run")
    public RunResponse run(@RequestBody RunRequest request) {
        return orchestrator.run(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadInput(IllegalArgumentException e) {
        log.warn("bad input: {}", e.getMessage());
        return ResponseEntity.status(400).body(Map.of(
                "error", Map.of(
                        "code", "BAD_REQUEST",
                        "message", e.getMessage()
                )
        ));
    }
}
