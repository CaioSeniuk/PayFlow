package br.com.payflow.payment.config;

import br.com.payflow.payment.application.ProvisionDemoMerchant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "payflow.demo-merchant.enabled", havingValue = "true")
@EnableConfigurationProperties(DemoMerchantProperties.class)
public class DemoMerchantConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DemoMerchantConfiguration.class);

    @Bean
    ApplicationRunner provisionDemoMerchant(ProvisionDemoMerchant useCase,
            DemoMerchantProperties properties) {
        return args -> {
            var merchant = useCase.execute(properties.apiKey());
            log.info("Demo merchant provisioned: {}", merchant.id());
        };
    }
}
