package br.com.payflow.payment.merchants.authenticate;

import java.util.Optional;

import br.com.payflow.payment.merchants.domain.Merchant;

// Output port used only by the authentication slice.
public interface MerchantLookup {

    Optional<Merchant> findByApiKeyHash(String apiKeyHash);
}
