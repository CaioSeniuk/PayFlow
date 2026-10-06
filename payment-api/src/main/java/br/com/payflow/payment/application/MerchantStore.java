package br.com.payflow.payment.application;

import br.com.payflow.payment.domain.Merchant;

public interface MerchantStore {

    // Atomically inserts the candidate or returns the existing merchant without changing it.
    Merchant insertIfAbsent(Merchant candidate);
}
