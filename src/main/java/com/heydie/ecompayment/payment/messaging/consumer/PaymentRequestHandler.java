package com.heydie.ecompayment.payment.messaging.consumer;

import com.heydie.ecompayment.inbox.InboxService;
import com.heydie.ecompayment.payment.messaging.dto.PaymentRequestedEvent;
import com.heydie.ecompayment.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentRequestHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentRequestHandler.class);

    private final InboxService inboxService;
    private final PaymentService paymentService;

    public PaymentRequestHandler(InboxService inboxService, PaymentService paymentService) {
        this.inboxService = inboxService;
        this.paymentService = paymentService;
    }

    @Transactional
    public void handle(PaymentRequestedEvent event, String topic) {
        if(inboxService.alreadyProcessed(event.eventId())) {
            log.info("Skip: eventId null / sudah diproses ({})", event.eventId());
            return;
        }

        inboxService.markProcessed(event.eventId(), topic);
        paymentService.initiate(event);
    }
}
