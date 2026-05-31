package host.config;

import host.llm.CrossServerPoisonedMockLlmClient;
import host.llm.LlmClient;
import host.llm.MockLlmClient;
import host.llm.OutputPoisonedMockLlmClient;
import host.llm.PoisonedMockLlmClient;
import host.llm.RealLlmClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class LlmConfig {

    @Bean
    public LlmClient llmClient(
            @Value("${host.llm.mode}") String mode,
            @Value("${host.llm.anthropic.api-key:}") String anthropicApiKey,
            @Value("${host.llm.anthropic.model:claude-sonnet-4-5}") String anthropicModel,
            @Value("${host.llm.anthropic.url:https://api.anthropic.com/v1/messages}") String anthropicUrl,
            RestClient.Builder restClientBuilder
    ) {
        return switch (mode) {
            case "mock" -> new MockLlmClient();
            case "mock_poisoned" -> new PoisonedMockLlmClient();
            case "mock_cross_poisoned" -> new CrossServerPoisonedMockLlmClient();
            case "mock_output_poisoned" -> new OutputPoisonedMockLlmClient();
            case "real_deterministic" -> new RealLlmClient(restClientBuilder, anthropicApiKey, anthropicModel, anthropicUrl);
            default -> throw new IllegalStateException("Unknown LLM_MODE: " + mode);
        };
    }
}
