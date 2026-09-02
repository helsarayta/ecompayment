package com.heydie.ecompayment.outbox;

import com.heydie.ecompayment.entity.OutboxEvent;
import com.heydie.ecompayment.entity.enumeration.OutboxStatus;
import com.heydie.ecompayment.repository.OutboxRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OutboxService {

    private static final String AGREGATE_TYPE = "PAYMENT";

    private final OutboxRepository outboxRepository;
    private final JsonMapper jsonMapper;

    public OutboxService(OutboxRepository outboxRepository, JsonMapper jsonMapper) {
        this.outboxRepository = outboxRepository;
        this.jsonMapper = jsonMapper;
    }

    public void record(String aggregateId, String eventType, String topic,
                       String messageKey, Object payload) {

        OutboxEvent event = OutboxEvent.builder()
                .aggregateType(AGREGATE_TYPE)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .topic(topic)
                .messageKey(messageKey)
                .payload(jsonMapper.writeValueAsString(payload))
                .status(OutboxStatus.PENDING)
                .attempts(0)
                .build();

        outboxRepository.save(event);
    }
}
