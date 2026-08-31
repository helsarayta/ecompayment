package com.heydie.ecompayment.payment.service;

import com.heydie.ecompayment.entity.enumeration.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentStatusMapperTest {

    private final PaymentStatusMapper mapper = new PaymentStatusMapper();

    // Kolom fraud yang kosong (di antara dua koma) = null. WAJIB tetap 3 kolom
    // supaya `expected` ada di posisi ke-3.
    @ParameterizedTest
    @CsvSource({
            "capture,        accept,    PAID",
            "capture,        ,          PAID",
            "CAPTURE,        ACCEPT,    PAID",
            "capture,        challenge, CHALLENGE",
            "settlement,     ,          PAID",
            "pending,        ,          PENDING",
            "deny,           ,          FAILED",
            "failure,        ,          FAILED",
            "cancel,         ,          CANCELLED",
            "expire,         ,          EXPIRED",
            "refund,         ,          REFUNDED",
            "partial_refund, ,          REFUNDED",
    })
    void mapsMidtransStatus(String tx, String fraud, Status expected) {
        assertThat(mapper.fromMidtrans(tx, fraud)).contains(expected);
    }

    @Test
    void unknownOrNullIsEmpty() {
        assertThat(mapper.fromMidtrans("brand_new_status", null)).isEmpty();
        assertThat(mapper.fromMidtrans(null, null)).isEmpty();
        assertThat(mapper.fromMidtrans("  ", null)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "INITIATED, PENDING,  true",
            "PENDING,   PAID,     true",
            "PENDING,   EXPIRED,  true",
            "CHALLENGE, PAID,     true",
            "CHALLENGE, FAILED,   true",
            "PENDING,   PENDING,  false",
            "PAID,      PENDING,  false",
            "PAID,      EXPIRED,  false",
            "PAID,      REFUNDED, true",
            "EXPIRED,   PAID,     false",
            "CANCELLED, PAID,     false",
            "REFUNDED,  PAID,     false",
    })
    void guardsTransitions(Status current, Status incoming, boolean allowed) {
        assertThat(mapper.canApply(current, incoming)).isEqualTo(allowed);
    }
}
