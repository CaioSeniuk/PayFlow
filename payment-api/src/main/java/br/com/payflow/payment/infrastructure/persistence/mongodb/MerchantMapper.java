package br.com.payflow.payment.infrastructure.persistence.mongodb;

import java.util.Objects;
import br.com.payflow.payment.domain.Merchant;

final class MerchantMapper {

    private MerchantMapper() {
    }

    static Merchant toDomain(MerchantDocument document) {
        Objects.requireNonNull(document, "MongoDB returned no merchant");
        return new Merchant(document.getId(), document.getName(),
                document.getApiKeyHash(), document.getCreatedAt());
    }
}
