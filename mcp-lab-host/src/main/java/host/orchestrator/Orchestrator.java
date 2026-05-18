package host.orchestrator;

import host.dto.RunRequest;
import host.dto.RunResponse;
import host.dto.ToolCallSummary;
import host.llm.LlmClient;
import host.llm.LlmDecision;
import host.llm.ToolDescriptor;
import host.mcp.McpClientFacade;
import host.mcp.ToolCallResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class Orchestrator {

    private static final Logger log = LoggerFactory.getLogger(Orchestrator.class);

    private final LlmClient llm;
    private final McpClientFacade mcp;
    private final String llmMode;

    public Orchestrator(
            LlmClient llm,
            McpClientFacade mcp,
            @Value("${host.llm.mode}") String llmMode
    ) {
        this.llm = llm;
        this.mcp = mcp;
        this.llmMode = llmMode;
    }

    public RunResponse run(RunRequest request) {
        log.info("orchestrator.run scenario={} prompt='{}'", request.scenarioId(), request.prompt());

        String sessionId = mcp.initialize();
        List<ToolDescriptor> tools = mcp.listTools(sessionId);
        log.info("tools listed: {} entries", tools.size());

        List<LlmDecision> plan = llm.decideMultiStep(request.prompt(), request.scenarioId(), tools);
        log.info("llm plan size={}", plan.size());

        List<ToolCallSummary> summaries = new ArrayList<>();
        StringBuilder finalTextBuilder = new StringBuilder();

        for (int i = 0; i < plan.size(); i++) {
            LlmDecision decision = plan.get(i);
            log.info("step {}/{} → tool={} args={}", i + 1, plan.size(),
                    decision.toolName(), decision.arguments());
            ToolCallResult result = mcp.callTool(sessionId, decision.toolName(), decision.arguments());

            summaries.add(new ToolCallSummary(
                    decision.toolName(),
                    decision.arguments(),
                    result.textContent(),
                    result.isError()
            ));

            if (finalTextBuilder.length() > 0) {
                finalTextBuilder.append("\n---\n");
            }
            if (result.isError()) {
                finalTextBuilder.append("Tool ").append(decision.toolName())
                        .append(" error: ").append(result.textContent());
            } else {
                finalTextBuilder.append(result.textContent());
            }
        }

        return new RunResponse(
                request.scenarioId(),
                finalTextBuilder.toString(),
                summaries,
                llmMode,
                plan.size()
        );
    }
}
