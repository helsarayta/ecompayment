package com.heydie.ecompayment.entity;

import com.heydie.ecompayment.entity.baseentity.BaseEntity;
import com.heydie.ecompayment.entity.enumeration.OutboxStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
public class OutboxEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateType;

    private String aggregateId;

    private String eventType;

    private String topic;

    private String payload;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    private LocalDateTime publishedAt;

    private Integer attempts;

    private String messageKey;
}
