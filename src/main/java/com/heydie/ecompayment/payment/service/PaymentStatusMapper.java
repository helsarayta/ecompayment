package com.heydie.ecompayment.payment.service;

import com.heydie.ecompayment.entity.enumeration.Status;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Optional;

@Component
public class PaymentStatusMapper {

    private static final EnumSet<Status> FINAL_STATES = EnumSet.of(
            Status.PAID, Status.FAILED, Status.EXPIRED, Status.CANCELLED, Status.REFUNDED
    );

    public Optional<Status> fromMidtrans(String transactionStatus, String fraudStatus) {
        if(transactionStatus == null || transactionStatus.isBlank()) {
            return Optional.empty();
        }

        String tx = transactionStatus.trim().toLowerCase();
        String fraud = fraudStatus == null ? null : fraudStatus.trim().toLowerCase();

        return switch (tx) {
            case "capture" -> Optional.of("challenge".equals(fraud) ? Status.CHALLENGE : Status.PAID);
            case "settlement" -> Optional.of(Status.PAID);
            case "pending" -> Optional.of(Status.PENDING);
            case "deny", "failure" -> Optional.of(Status.FAILED);
            case "cancel" -> Optional.of(Status.CANCELLED);
            case "expire" -> Optional.of(Status.EXPIRED);
            case "refund", "partial_refund" -> Optional.of(Status.REFUNDED);
            default -> Optional.empty();
        };
    }

    /**
     * Boleh menulis `incoming` ke DB?
     * - current == incoming            → false (idempoten, tidak ada perubahan)
     * - current belum final            → true  (bebas maju)
     * - current sudah final            → false, KECUALI PAID → REFUNDED
     */
    public boolean canApply(Status current, Status incoming) {
        if(current == incoming) {
            return false;
        }

        if(!FINAL_STATES.contains(current)) {
            return true;
        }

        return current == Status.PAID && incoming == Status.REFUNDED;
    }
}
