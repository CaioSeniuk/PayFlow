package br.com.payflow.payment.config;

import java.time.Clock;

import br.com.payflow.payment.application.MerchantStore;
import br.com.payflow.payment.application.ProvisionDemoMerchant;
import br.com.payflow.payment.infrastructure.persistence.mongodb.MongoMerchantStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration(proxyBeanMethods = false)
public class ApplicationConfiguration {

    @Bean
    MerchantStore merchantStore(MongoTemplate mongo) {
        return new MongoMerchantStore(mongo);
    }

    @Bean
    ProvisionDemoMerchant provisionDemoMerchantUseCase(MerchantStore merchants) {
        return new ProvisionDemoMerchant(merchants, Clock.systemUTC());
    }
}
