package br.com.payflow.payment.merchants.authenticate;

import java.util.UUID;

// Use case output: the merchant identity without credentials.
public record AuthenticatedMerchant(UUID id, String name) {
}
