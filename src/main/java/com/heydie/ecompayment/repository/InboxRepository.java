package com.heydie.ecompayment.repository;

import com.heydie.ecompayment.entity.InboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InboxRepository extends JpaRepository<InboxMessage, String> {
    // @Id = eventId → existsById(eventId) & save() sudah cukup untuk dedupe (step 6)

}
