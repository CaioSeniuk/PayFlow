package br.com.payflow.payment.merchants.domain;

import java.util.Objects;

// Raw merchant credential. It never leaves the use cases: only its hash is stored or compared.
public final class ApiKey {

    public static final int MIN_LENGTH = 32;
    public static final int MAX_LENGTH = 256;

    private final String value;

    private ApiKey(String value) {
        this.value = value;
    }

    public static ApiKey of(String value) {
        if (!isWellFormed(value)) {
            throw new IllegalArgumentException(
                    "API key must contain between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters");
        }
        return new ApiKey(value);
    }

    public static boolean isWellFormed(String value) {
        return value != null && !value.isBlank()
                && value.length() >= MIN_LENGTH && value.length() <= MAX_LENGTH;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ApiKey key && value.equals(key.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "ApiKey[****]";
    }
}
