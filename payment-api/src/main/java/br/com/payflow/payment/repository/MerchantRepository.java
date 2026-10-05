package br.com.payflow.payment.repository;

import java.util.Optional;
import java.util.UUID;

import br.com.payflow.payment.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    Optional<Merchant> findByApiKeyHash(String apiKeyHash);
}
