package com.heydie.ecompayment.entity;

import com.heydie.ecompayment.entity.baseentity.BaseEntity;
import com.heydie.ecompayment.entity.enumeration.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
public class Payment extends BaseEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(unique = true, nullable = false)
        private String orderId;

        @Column(unique = true)
        private String midtransOrderId;

        @Column(nullable = false)
        private BigInteger grossAmount;

        @Column(nullable = false)
        private String currency;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private Status status;

        private String snapToken;

        private String snapRedirectUrl;

        private String midtransTransactionId;

        private String paymentType;

        private String fraudStatus;

        private String statusCode;

        private LocalDateTime expiryAt;

        private LocalDateTime paidAt;

        @Version
        private Long version;

        private Integer attempt;

        private String rawTransactionStatus;

        private LocalDateTime transactionTime;

        private String statusMessage;

}
