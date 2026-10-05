package br.com.payflow.payment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import br.com.payflow.payment.repository.MerchantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentApiApplicationTests {

    private static final String API_KEY = UUID.randomUUID().toString();

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("payflow.demo-merchant.enabled", () -> true);
        registry.add("payflow.demo-merchant.api-key", () -> API_KEY);
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MerchantRepository merchants;

    @Autowired
    Environment environment;

    @Test
    void migrationAndBootstrapPersistOnlyTheApiKeyHash() throws Exception {
        String expectedHash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(API_KEY.getBytes(StandardCharsets.UTF_8)));

        assertThat(merchants.count()).isEqualTo(1);
        assertThat(merchants.findByApiKeyHash(expectedHash)).isPresent();
        assertThat(jdbc.queryForObject("SELECT api_key_hash FROM merchants", String.class))
                .isEqualTo(expectedHash)
                .isNotEqualTo(API_KEY);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void healthCheckIsUpWithoutExposingDetails() throws Exception {
        String port = environment.getRequiredProperty("local.server.port");
        try (HttpClient client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(
                    URI.create("http://localhost:" + port + "/actuator/health")).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            var health = new ObjectMapper().readTree(response.body());
            assertThat(health.get("status").asText()).isEqualTo("UP");
            assertThat(health.has("components")).isFalse();
            assertThat(health.has("details")).isFalse();
        }
    }
}
