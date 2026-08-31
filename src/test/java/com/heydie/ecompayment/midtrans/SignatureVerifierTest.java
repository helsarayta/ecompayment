package com.heydie.ecompayment.midtrans;

import com.heydie.ecompayment.config.MidtransProperties;
import com.heydie.ecompayment.midtrans.dto.MidtransNotification;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SignatureVerifierTest {

    // SHA512("ORD-1-1" + "200" + "250000.00" + "SB-Mid-server-test")
    // regen: printf '%s' 'ORD-1-1200250000.00SB-Mid-server-test' | shasum -a 512 | cut -d' ' -f1
    private static final String VALID_SIG =
            "6854f9c7778929a83b1946833dedcd604f89f35545cccddfd896cedde281561420811332b5090cd3ee3d8ab11f27544919f7ff7fdccaafd183f8eca323325dd2";

    private final MidtransProperties props = new MidtransProperties(
            false, "SB-Mid-server-test",
            "https://app.sandbox.midtrans.com", "https://api.sandbox.midtrans.com",
            Duration.ofSeconds(5), Duration.ofSeconds(15), null);

    private final SignatureVerifier verifier = new SignatureVerifier(props);

    private static MidtransNotification notif(String orderId, String statusCode,
                                              String grossAmount, String signatureKey) {
        return new MidtransNotification(orderId, statusCode, grossAmount, signatureKey,
                "settlement", "accept", "txn-1", "bank_transfer", null, null, null);
    }

    @Test
    void acceptsValidSignature() {
        assertThat(verifier.isValid(notif("ORD-1-1", "200", "250000.00", VALID_SIG))).isTrue();
    }

    @Test
    void acceptsValidSignatureCaseInsensitive() {
        assertThat(verifier.isValid(notif("ORD-1-1", "200", "250000.00", VALID_SIG.toUpperCase()))).isTrue();
    }

    @Test
    void rejectsTamperedAmount() {
        assertThat(verifier.isValid(notif("ORD-1-1", "200", "999999.00", VALID_SIG))).isFalse();
    }

    @Test
    void rejectsTamperedStatusCode() {
        assertThat(verifier.isValid(notif("ORD-1-1", "201", "250000.00", VALID_SIG))).isFalse();
    }

    @Test
    void rejectsGarbageSignature() {
        assertThat(verifier.isValid(notif("ORD-1-1", "200", "250000.00", "deadbeef"))).isFalse();
    }

    @Test
    void rejectsNullFields() {
        assertThat(verifier.isValid(notif("ORD-1-1", "200", "250000.00", null))).isFalse();
        assertThat(verifier.isValid(notif(null, "200", "250000.00", VALID_SIG))).isFalse();
        assertThat(verifier.isValid(notif("ORD-1-1", null, "250000.00", VALID_SIG))).isFalse();
        assertThat(verifier.isValid(notif("ORD-1-1", "200", null, VALID_SIG))).isFalse();
    }
}
