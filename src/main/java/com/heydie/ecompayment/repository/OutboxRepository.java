package com.heydie.ecompayment.repository;

import com.heydie.ecompayment.entity.OutboxEvent;
import com.heydie.ecompayment.entity.enumeration.OutboxStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    // versi sederhana; FOR UPDATE SKIP LOCKED ditambah di step 8
    List<OutboxEvent> findByStatusOrderByCreatedDateAsc(OutboxStatus status, Limit limit);
}
