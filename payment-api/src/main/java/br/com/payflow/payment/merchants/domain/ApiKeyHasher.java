package br.com.payflow.payment.merchants.domain;

// Port shared by the merchant slices; the algorithm lives in infrastructure.
public interface ApiKeyHasher {

    // Returns a lowercase SHA-256 hexadecimal digest, as required by Merchant.
    String hash(ApiKey apiKey);
}
