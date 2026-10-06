package br.com.payflow.payment.config;

import br.com.payflow.payment.infrastructure.persistence.mongodb.MerchantDocument;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration(proxyBeanMethods = false)
public class MongoConfiguration {

    @Bean
    InitializingBean merchantIndexes(MongoTemplate mongo) {
        return () -> mongo.indexOps(MerchantDocument.class).createIndex(
                new Index().on("apiKeyHash", Sort.Direction.ASC).unique().named("merchant_api_key_unique"));
    }
}
