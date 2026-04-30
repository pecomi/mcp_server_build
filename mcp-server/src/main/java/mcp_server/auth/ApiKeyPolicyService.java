package mcp_server.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ApiKeyPolicyService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ApiKeyPolicyService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<AuthenticatedClient> findByApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        String redisKey = "api-key:" + apiKey;
        String json = redisTemplate.opsForValue().get(redisKey);

        if (json == null || json.isBlank()) {
            return Optional.empty();
        }

        try {
            AuthenticatedClient client =
                    objectMapper.readValue(json, AuthenticatedClient.class);

            return Optional.of(client);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}