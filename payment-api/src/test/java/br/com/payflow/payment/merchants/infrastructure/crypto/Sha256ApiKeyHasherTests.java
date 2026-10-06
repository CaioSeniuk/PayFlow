package br.com.payflow.payment.merchants.infrastructure.crypto;

import br.com.payflow.payment.merchants.domain.ApiKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Sha256ApiKeyHasherTests {

    private final Sha256ApiKeyHasher hasher = new Sha256ApiKeyHasher();

    @Test
    void producesLowercaseSha256Digest() {
        assertThat(hasher.hash(ApiKey.of("a".repeat(32))))
                .isEqualTo("3ba3f5f43b92602683c19aee62a20342b084dd5971ddd33808d81a328879a547");
    }

    @Test
    void rawKeyIsMaskedInText() {
        assertThat(ApiKey.of("a".repeat(32)).toString()).doesNotContain("a".repeat(32));
    }
}
