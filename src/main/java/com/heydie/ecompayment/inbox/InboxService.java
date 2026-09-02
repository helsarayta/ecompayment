package com.heydie.ecompayment.inbox;

import com.heydie.ecompayment.entity.InboxMessage;
import com.heydie.ecompayment.repository.InboxRepository;
import org.springframework.stereotype.Component;

@Component
public class InboxService {

    private final InboxRepository inboxRepository;

    public InboxService(InboxRepository inboxRepository) {
        this.inboxRepository = inboxRepository;
    }

    public boolean alreadyProcessed(String eventId) {
        return eventId == null || inboxRepository.existsById(eventId);
    }

    public void markProcessed(String eventId, String topic) {
        inboxRepository.save(
                InboxMessage.builder()
                        .eventId(eventId)
                        .topic(topic)
                        .build());
    }
}
