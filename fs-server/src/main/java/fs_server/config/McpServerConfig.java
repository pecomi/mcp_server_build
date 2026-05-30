package fs_server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import fs_server.tool.FsTools;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Configuration
public class McpServerConfig {

    private final ObjectMapper objectMapper = new ObjectMapper();

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
            FsTools fsTools
    ) {
        McpSchema.Tool readFileTool = createReadFileTool();

        return McpServer.sync(transportProvider)
                .serverInfo("fs-server", "0.0.1")
                .instructions("파일 읽기 도구를 제공하는 mock 파일 서비스. M6 — sink server (path 검증 없음).")
                .requestTimeout(Duration.ofSeconds(10))
                .strictToolNameValidation(true)
                .toolCall(readFileTool, (exchange, request) -> {
                    try {
                        Map<String, Object> args = request.arguments();
                        String path = toStringValue(args.get("path"));

                        String content = fsTools.readFile(path);

                        return McpSchema.CallToolResult.builder()
                                .addTextContent(content)
                                .isError(false)
                                .build();

                    } catch (Exception e) {
                        return McpSchema.CallToolResult.builder()
                                .addTextContent("read_file 호출 실패: " + e.getMessage())
                                .isError(true)
                                .build();
                    }
                })
                .build();
    }

    private McpSchema.Tool createReadFileTool() {
        return McpSchema.Tool.builder()
                .name("read_file")
                .title("파일 읽기")
                .description("주어진 path의 파일 내용을 텍스트로 반환한다.")
                .inputSchema(createReadFileInputSchema())
                .build();
    }

    private McpSchema.JsonSchema createReadFileInputSchema() {
        Map<String, Object> properties = Map.of(
                "path", Map.of(
                        "type", "string",
                        "description", "파일 경로."
                )
        );

        return new McpSchema.JsonSchema(
                "object",
                properties,
                List.of("path"),
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
