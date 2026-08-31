package com.heydie.ecompayment.entity;

import com.heydie.ecompayment.entity.baseentity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
public class InboxMessage extends BaseEntity {

    @Id
    private String eventId;

    private String topic;
}
