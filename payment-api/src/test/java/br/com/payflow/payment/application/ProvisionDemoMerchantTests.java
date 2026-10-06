package br.com.payflow.payment.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import br.com.payflow.payment.domain.Merchant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProvisionDemoMerchantTests {

    private static final String KEY = "a".repeat(32);
    private static final Instant NOW = Instant.parse("2026-10-06T00:00:00Z");
    private final AtomicReference<Merchant> stored = new AtomicReference<>();
    private final ProvisionDemoMerchant useCase = new ProvisionDemoMerchant(
            candidate -> {
                stored.compareAndSet(null, candidate);
                return stored.get();
            }, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void createsMerchantWithHashAndFixedTime() {
        Merchant merchant = useCase.execute(KEY);
        assertThat(merchant.id()).isEqualTo(ProvisionDemoMerchant.MERCHANT_ID);
        assertThat(merchant.name()).isEqualTo("Demo Merchant");
        assertThat(merchant.createdAt()).isEqualTo(NOW);
        assertThat(merchant.apiKeyHash())
                .isEqualTo("3ba3f5f43b92602683c19aee62a20342b084dd5971ddd33808d81a328879a547");
    }

    @Test
    void repetitionKeepsOriginalMerchantAndTimestamp() {
        Merchant original = useCase.execute(KEY);
        var later = new ProvisionDemoMerchant(candidate -> stored.get(),
                Clock.fixed(NOW.plusSeconds(3600), ZoneOffset.UTC));
        assertThat(later.execute(KEY)).isSameAs(original);
    }

    @Test
    void changedKeyFailsWithoutOverwritingExistingMerchant() {
        Merchant original = useCase.execute(KEY);
        assertThatThrownBy(() -> useCase.execute("b".repeat(32)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("different API key");
        assertThat(stored.get()).isSameAs(original);
    }

    @Test
    void invalidKeyNeverReachesPersistence() {
        for (String key : new String[]{null, "", " ".repeat(32), "short", "a".repeat(257)}) {
            assertThatThrownBy(() -> useCase.execute(key)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(stored.get()).isNull();
    }

    @Test
    void persistenceFailureIsNotHidden() {
        var failing = new ProvisionDemoMerchant(candidate -> {
            throw new IllegalStateException("Storage unavailable");
        }, Clock.fixed(NOW, ZoneOffset.UTC));
        assertThatThrownBy(() -> failing.execute(KEY)).hasMessage("Storage unavailable");
    }
}
