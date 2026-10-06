package br.com.payflow.payment.merchants.infrastructure;

import br.com.payflow.payment.merchants.domain.ApiKeyHasher;
import br.com.payflow.payment.merchants.infrastructure.crypto.Sha256ApiKeyHasher;
import br.com.payflow.payment.merchants.infrastructure.mongodb.MerchantDocument;
import br.com.payflow.payment.merchants.infrastructure.mongodb.MongoMerchantStore;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

// Binds the merchant ports (MerchantProvisioning, MerchantLookup, ApiKeyHasher) to their adapters.
@Configuration(proxyBeanMethods = false)
public class MerchantInfrastructureConfiguration {

    @Bean
    MongoMerchantStore merchantStore(MongoTemplate mongo) {
        return new MongoMerchantStore(mongo);
    }

    @Bean
    ApiKeyHasher apiKeyHasher() {
        return new Sha256ApiKeyHasher();
    }

    @Bean
    InitializingBean merchantIndexes(MongoTemplate mongo) {
        return () -> mongo.indexOps(MerchantDocument.class).createIndex(
                new Index().on("apiKeyHash", Sort.Direction.ASC).unique().named("merchant_api_key_unique"));
    }
}
