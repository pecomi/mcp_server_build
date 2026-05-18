package research_server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import research_server.tool.ResearchTools;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Configuration
public class McpServerConfig {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${research.tool.lookup-term.description:주어진 용어(term)에 대한 사전적 정의를 반환한다.}")
    private String lookupTermDescription;

    @Bean
    public HttpServletStreamableServerTransportProvider mcpStreamableTransportProvider() {
        return HttpServletStreamableServerTransportProvider.builder()
                .jsonMapper(new JacksonMcpJsonMapper(objectMapper))
                .mcpEndpoint("/mcp")
                .disallowDelete(true)
                .keepAliveInterval(Duration.ofSeconds(30))
                .build();
    }

    @Bean
    public McpSyncServer mcpSyncServer(
            HttpServletStreamableServerTransportProvider transportProvider,
            ResearchTools researchTools
    ) {
        McpSchema.Tool lookupTermTool = createLookupTermTool();

        return McpServer.sync(transportProvider)
                .serverInfo("research-server", "0.0.1")
                .instructions("사전 검색 도구를 제공하는 mock research 서버. M7 — 페더레이션 vehicle.")
                .requestTimeout(Duration.ofSeconds(10))
                .strictToolNameValidation(true)
                .toolCall(lookupTermTool, (exchange, request) -> {
                    try {
                        Map<String, Object> args = request.arguments();
                        String term = toStringValue(args.get("term"));

                        String content = researchTools.lookupTerm(term);

                        return McpSchema.CallToolResult.builder()
                                .addTextContent(content)
                                .isError(false)
                                .build();

                    } catch (Exception e) {
                        return McpSchema.CallToolResult.builder()
                                .addTextContent("lookup_term 호출 실패: " + e.getMessage())
                                .isError(true)
                                .build();
                    }
                })
                .build();
    }

    private McpSchema.Tool createLookupTermTool() {
        return McpSchema.Tool.builder()
                .name("lookup_term")
                .title("용어 사전 검색")
                .description(lookupTermDescription)
                .inputSchema(createLookupTermInputSchema())
                .build();
    }

    private McpSchema.JsonSchema createLookupTermInputSchema() {
        Map<String, Object> properties = Map.of(
                "term", Map.of(
                        "type", "string",
                        "description", "검색할 용어."
                )
        );

        return new McpSchema.JsonSchema(
                "object",
                properties,
                List.of("term"),
                false,
                null,
                null
        );
    }

    private String toStringValue(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }
}
