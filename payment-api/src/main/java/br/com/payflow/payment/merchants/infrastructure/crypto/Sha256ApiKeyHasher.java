package br.com.payflow.payment.merchants.infrastructure.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import br.com.payflow.payment.merchants.domain.ApiKey;
import br.com.payflow.payment.merchants.domain.ApiKeyHasher;

public final class Sha256ApiKeyHasher implements ApiKeyHasher {

    @Override
    public String hash(ApiKey apiKey) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(apiKey.value().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
