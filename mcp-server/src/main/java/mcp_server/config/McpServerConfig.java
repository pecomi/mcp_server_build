package mcp_server.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import mcp_server.dto.StoreListResponse;
import mcp_server.tool.GongGongNuriTools;
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
            GongGongNuriTools gongGongNuriTools
    ) {
        McpSchema.Tool getStoreListTool = createGetStoreListTool();

        return McpServer.sync(transportProvider)
                .serverInfo("mcp-lab-server", "0.0.1")
                .instructions("공유누리 예약 서비스 API를 MCP Tool로 제공하는 로컬 테스트 서버입니다.")
                .requestTimeout(Duration.ofSeconds(10))
                .strictToolNameValidation(true)
                .toolCall(getStoreListTool, (exchange, request) -> {
                try {
                        // AuthenticatedClient client = ApiKeyContext.get();

                        // if (client == null) {
                        // return McpSchema.CallToolResult.builder()
                        //         .addTextContent("인증 정보가 없습니다.")
                        //         .isError(true)
                        //         .build();
                        // }

                        // if (!client.canCallTool("getStoreList")) {
                        // return McpSchema.CallToolResult.builder()
                        //         .addTextContent("FORBIDDEN_TOOL: 이 API Key는 getStoreList Tool을 호출할 권한이 없습니다.")
                        //         .isError(true)
                        //         .build();
                        // }

                        Map<String, Object> args = request.arguments();

                        StoreListResponse response = gongGongNuriTools.getStoreList(
                                toInteger(args.get("page")),
                                toInteger(args.get("size")),
                                toStringValue(args.get("consumerCd")),
                                toStringValue(args.get("sido")),
                                toStringValue(args.get("sigungu")),
                                toStringValue(args.get("searchFreeYn")),
                                toStringValue(args.get("searchSbclsCd")),
                                toStringValue(args.get("searchMnclsCd"))
                        );

                        String responseJson = objectMapper.writeValueAsString(response);

                        return McpSchema.CallToolResult.builder()
                                .addTextContent(responseJson)
                                .structuredContent(response)
                                .isError(false)
                                .build();

                } catch (Exception e) {
                        return McpSchema.CallToolResult.builder()
                                .addTextContent("getStoreList 호출 실패: " + e.getMessage())
                                .isError(true)
                                .build();
                }
                })
                .build();
    }

    private McpSchema.Tool createGetStoreListTool() {
        return McpSchema.Tool.builder()
                .name("getStoreList")
                .title("공유누리 장소 목록 조회")
                .description("지역, 무료 여부, 자원 분류 조건을 기반으로 공유누리 장소 목록을 조회한다.")
                .inputSchema(createGetStoreListInputSchema())
                .build();
    }

    private McpSchema.JsonSchema createGetStoreListInputSchema() {
        Map<String, Object> properties = Map.of(
                "page", Map.of(
                        "type", "integer",
                        "description", "조회할 페이지 번호. 1부터 시작한다.",
                        "minimum", 1
                ),
                "size", Map.of(
                        "type", "integer",
                        "description", "페이지당 결과 수. 1 이상 200 이하.",
                        "minimum", 1,
                        "maximum", 200
                ),
                "consumerCd", Map.of(
                        "type", "string",
                        "description", "수요기관 코드 또는 연동 서비스 식별자."
                ),
                "sido", Map.of(
                        "type", "string",
                        "description", "시도 코드. 예: 서울=11."
                ),
                "sigungu", Map.of(
                        "type", "string",
                        "description", "시군구 이름 또는 코드."
                ),
                "searchFreeYn", Map.of(
                        "type", "string",
                        "description", "무료 여부. Y 또는 N.",
                        "enum", List.of("Y", "N")
                ),
                "searchSbclsCd", Map.of(
                        "type", "string",
                        "description", "자원 소분류 코드. 예: 회의실, 체육시설 등."
                ),
                "searchMnclsCd", Map.of(
                        "type", "string",
                        "description", "자원 중분류 코드."
                )
        );

        return new McpSchema.JsonSchema(
                "object",
                properties,
                List.of("page", "size", "consumerCd"),
                false,
                null,
                null
        );
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Integer integer) {
            return integer;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return Integer.parseInt(stringValue);
        }

        return null;
    }

    private String toStringValue(Object value) {
        if (value == null) {
            return null;
        }

        return String.valueOf(value);
    }
}