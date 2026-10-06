package br.com.payflow.payment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import br.com.payflow.payment.config.DemoMerchantConfiguration;
import br.com.payflow.payment.entity.Merchant;
import br.com.payflow.payment.repository.MerchantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentApiApplicationTests {

    private static final String API_KEY = UUID.randomUUID().toString();

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:8.0");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", MONGO::getReplicaSetUrl);
        registry.add("spring.mongodb.database", () -> "payflow_test");
        registry.add("payflow.demo-merchant.enabled", () -> true);
        registry.add("payflow.demo-merchant.api-key", () -> API_KEY);
    }

    @Autowired
    MerchantRepository merchants;

    @Autowired
    MongoTemplate mongo;

    @Autowired
    ApplicationRunner provisionDemoMerchant;

    @Autowired
    Environment environment;

    @Test
    void bootstrapStoresOnlyHashAndIsRepeatable() throws Exception {
        String hash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(API_KEY.getBytes(StandardCharsets.UTF_8)));
        provisionDemoMerchant.run(new DefaultApplicationArguments(new String[0]));

        assertThat(merchants.count()).isEqualTo(1);
        Merchant merchant = merchants.findByApiKeyHash(hash).orElseThrow();
        assertThat(merchant.getId()).isEqualTo(DemoMerchantConfiguration.MERCHANT_ID);
        assertThat(merchant.getApiKeyHash()).isNotEqualTo(API_KEY);
        assertThat(merchant.getCreatedAt()).isNotNull();
        var stored = mongo.getCollection("merchants").find().first();
        assertThat(stored).isNotNull();
        assertThat(stored.toJson()).doesNotContain(API_KEY);
    }

    @Test
    void duplicateApiKeyHashIsRejected() {
        String hash = merchants.findById(DemoMerchantConfiguration.MERCHANT_ID).orElseThrow().getApiKeyHash();
        var duplicate = new org.bson.Document("_id", "another-merchant").append("apiKeyHash", hash);
        assertThatThrownBy(() -> mongo.insert(duplicate, "merchants"))
                .isInstanceOf(DuplicateKeyException.class);
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
