package br.com.payflow.payment.merchants.authenticate;

// Same failure for missing, malformed and unknown keys, so callers cannot probe which one happened.
public final class InvalidApiKeyException extends RuntimeException {

    public InvalidApiKeyException() {
        super("Missing or invalid API key");
    }
}
