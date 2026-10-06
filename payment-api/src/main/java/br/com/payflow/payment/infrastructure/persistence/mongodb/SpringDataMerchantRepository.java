package br.com.payflow.payment.infrastructure.persistence.mongodb;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataMerchantRepository extends MongoRepository<MerchantDocument, UUID> {

    Optional<MerchantDocument> findByApiKeyHash(String apiKeyHash);
}
