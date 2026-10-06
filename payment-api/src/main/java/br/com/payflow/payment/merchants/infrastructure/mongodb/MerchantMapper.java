package br.com.payflow.payment.merchants.infrastructure.mongodb;

import java.util.Objects;
import br.com.payflow.payment.merchants.domain.Merchant;

final class MerchantMapper {

    private MerchantMapper() {
    }

    static Merchant toDomain(MerchantDocument document) {
        Objects.requireNonNull(document, "MongoDB returned no merchant");
        return new Merchant(document.getId(), document.getName(),
                document.getApiKeyHash(), document.getCreatedAt());
    }
}
