package br.com.payflow.payment.merchants.authenticate;

import br.com.payflow.payment.merchants.domain.ApiKeyHasher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Composition of the "authenticate merchant" slice.
@Configuration(proxyBeanMethods = false)
public class AuthenticateMerchantSlice {

    @Bean
    AuthenticateMerchant authenticateMerchantUseCase(MerchantLookup merchants, ApiKeyHasher hasher) {
        return new AuthenticateMerchant(merchants, hasher);
    }
}
