package br.com.payflow.payment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import br.com.payflow.payment.merchants.infrastructure.mongodb.MerchantDocument;
import br.com.payflow.payment.merchants.infrastructure.mongodb.SpringDataMerchantRepository;
import br.com.payflow.payment.merchants.provision.ProvisionDemoMerchant;
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
    SpringDataMerchantRepository merchants;

    @Autowired
    MongoTemplate mongo;

    @Autowired
    ApplicationRunner provisionDemoMerchant;

    @Autowired
    ProvisionDemoMerchant useCase;

    @Autowired
    Environment environment;

    @Test
    void bootstrapStoresOnlyHashAndIsRepeatable() throws Exception {
        String hash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(API_KEY.getBytes(StandardCharsets.UTF_8)));
        provisionDemoMerchant.run(new DefaultApplicationArguments(new String[0]));

        assertThat(merchants.count()).isEqualTo(1);
        MerchantDocument merchant = merchants.findByApiKeyHash(hash).orElseThrow();
        assertThat(merchant.getId()).isEqualTo(ProvisionDemoMerchant.MERCHANT_ID);
        assertThat(merchant.getApiKeyHash()).isNotEqualTo(API_KEY);
        assertThat(merchant.getCreatedAt()).isNotNull();
        var stored = mongo.getCollection("merchants").find().first();
        assertThat(stored).isNotNull();
        assertThat(stored.toJson()).doesNotContain(API_KEY);
    }

    @Test
    void duplicateApiKeyHashIsRejected() {
        String hash = merchants.findById(ProvisionDemoMerchant.MERCHANT_ID).orElseThrow().getApiKeyHash();
        var duplicate = new org.bson.Document("_id", "another-merchant").append("apiKeyHash", hash);
        assertThatThrownBy(() -> mongo.insert(duplicate, "merchants"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void changedKeyDoesNotOverwriteExistingDocument() {
        MerchantDocument original = merchants.findById(ProvisionDemoMerchant.MERCHANT_ID).orElseThrow();
        assertThatThrownBy(() -> useCase.execute("different-key-for-integration-test"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("different API key");
        MerchantDocument unchanged = merchants.findById(original.getId()).orElseThrow();
        assertThat(unchanged.getApiKeyHash()).isEqualTo(original.getApiKeyHash());
        assertThat(unchanged.getCreatedAt()).isEqualTo(original.getCreatedAt());
    }

    @Test
    void concurrentProvisioningPreservesSingleMerchant() throws Exception {
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(4)) {
            var tasks = new java.util.ArrayList<java.util.concurrent.Callable<java.util.UUID>>();
            for (int index = 0; index < 12; index++) {
                tasks.add(() -> useCase.execute(API_KEY).id());
            }
            for (var result : executor.invokeAll(tasks)) {
                assertThat(result.get()).isEqualTo(ProvisionDemoMerchant.MERCHANT_ID);
            }
        }
        assertThat(merchants.count()).isEqualTo(1);
    }

    @Test
    void currentMerchantIsReturnedForValidApiKey() throws Exception {
        var response = getCurrentMerchant(API_KEY);
        assertThat(response.statusCode()).isEqualTo(200);
        var body = new ObjectMapper().readTree(response.body());
        assertThat(body.get("id").asText()).isEqualTo(ProvisionDemoMerchant.MERCHANT_ID.toString());
        assertThat(body.get("name").asText()).isEqualTo("Demo Merchant");
        assertThat(response.body()).doesNotContain(API_KEY).doesNotContain("apiKeyHash");
    }

    @Test
    void missingOrUnknownApiKeyIsUnauthorizedWithProblemDetails() throws Exception {
        for (String key : new String[]{null, "short", "x".repeat(40)}) {
            var response = getCurrentMerchant(key);
            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.headers().firstValue("WWW-Authenticate")).hasValue("ApiKey header=\"X-Api-Key\"");
            var problem = new ObjectMapper().readTree(response.body());
            assertThat(problem.get("status").asInt()).isEqualTo(401);
            assertThat(problem.get("detail").asText()).isEqualTo("Missing or invalid API key");
        }
    }

    @Test
    void healthCheckIsUpWithoutExposingDetails() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(uri("/actuator/health")).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            var health = new ObjectMapper().readTree(response.body());
            assertThat(health.get("status").asText()).isEqualTo("UP");
            assertThat(health.has("components")).isFalse();
            assertThat(health.has("details")).isFalse();
        }
    }

    private HttpResponse<String> getCurrentMerchant(String apiKey) throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(uri("/v1/merchants/me")).GET();
            if (apiKey != null) {
                request.header("X-Api-Key", apiKey);
            }
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + environment.getRequiredProperty("local.server.port") + path);
    }
}
