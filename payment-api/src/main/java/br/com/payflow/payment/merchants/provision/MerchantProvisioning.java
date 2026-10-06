package br.com.payflow.payment.merchants.provision;

import br.com.payflow.payment.merchants.domain.Merchant;

// Output port used only by the provisioning slice.
public interface MerchantProvisioning {

    // Atomically inserts the candidate or returns the existing merchant without changing it.
    Merchant insertIfAbsent(Merchant candidate);
}
