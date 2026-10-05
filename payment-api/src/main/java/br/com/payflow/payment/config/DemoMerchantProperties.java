package br.com.payflow.payment.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("payflow.demo-merchant")
public record DemoMerchantProperties(
        @NotBlank @Size(min = 32, max = 256) String apiKey) {
}
