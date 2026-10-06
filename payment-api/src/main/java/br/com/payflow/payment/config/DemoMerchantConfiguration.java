package br.com.payflow.payment.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.time.Instant;

import br.com.payflow.payment.entity.Merchant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "payflow.demo-merchant.enabled", havingValue = "true")
@EnableConfigurationProperties(DemoMerchantProperties.class)
public class DemoMerchantConfiguration {

    public static final UUID MERCHANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Logger log = LoggerFactory.getLogger(DemoMerchantConfiguration.class);

    @Bean
    ApplicationRunner provisionDemoMerchant(MongoTemplate mongo,
            DemoMerchantProperties properties) {
        return args -> {
            String hash = hashApiKey(properties.apiKey());
            Merchant merchant = mongo.findAndModify(
                    Query.query(Criteria.where("_id").is(MERCHANT_ID)),
                    new Update().setOnInsert("name", "Demo Merchant")
                            .setOnInsert("apiKeyHash", hash).setOnInsert("createdAt", Instant.now()),
                    FindAndModifyOptions.options().upsert(true).returnNew(true), Merchant.class);
            if (merchant == null || !hash.equals(merchant.getApiKeyHash())) {
                throw new IllegalStateException(
                        "Demo merchant already exists with a different API key. "
                        + "Use the original key; changing the environment does not rotate credentials.");
            }
            log.info("Demo merchant provisioned: {}", MERCHANT_ID);
        };
    }

    private static String hashApiKey(String apiKey) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(apiKey.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
