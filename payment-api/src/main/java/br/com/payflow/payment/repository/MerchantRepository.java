package br.com.payflow.payment.repository;

import java.util.Optional;
import java.util.UUID;

import br.com.payflow.payment.entity.Merchant;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MerchantRepository extends MongoRepository<Merchant, UUID> {

    Optional<Merchant> findByApiKeyHash(String apiKeyHash);
}
