package br.com.payflow.payment.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "payflow.demo-merchant.enabled", havingValue = "true")
@EnableConfigurationProperties(DemoMerchantProperties.class)
public class DemoMerchantConfiguration {

    public static final UUID MERCHANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Logger log = LoggerFactory.getLogger(DemoMerchantConfiguration.class);

    @Bean
    ApplicationRunner provisionDemoMerchant(JdbcTemplate jdbc, TransactionTemplate transactions,
            DemoMerchantProperties properties) {
        return args -> {
            String hash = hashApiKey(properties.apiKey());
            transactions.executeWithoutResult(transaction -> {
                jdbc.update("""
                        INSERT INTO merchants (id, name, api_key_hash)
                        VALUES (?, 'Demo Merchant', ?)
                        ON CONFLICT (id) DO NOTHING
                        """, MERCHANT_ID, hash);
                String storedHash = jdbc.queryForObject(
                        "SELECT api_key_hash FROM merchants WHERE id = ?", String.class, MERCHANT_ID);
                if (!hash.equals(storedHash)) {
                    throw new IllegalStateException(
                            "Demo merchant already exists with a different API key. "
                            + "Use the original key; changing the environment does not rotate credentials.");
                }
            });
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
