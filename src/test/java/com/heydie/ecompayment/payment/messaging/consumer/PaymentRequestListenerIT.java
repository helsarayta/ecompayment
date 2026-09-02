package com.heydie.ecompayment.payment.messaging.consumer;

import com.heydie.ecompayment.midtrans.MidtransClient;
import com.heydie.ecompayment.midtrans.MidtransException;
import com.heydie.ecompayment.midtrans.dto.SnapChargeResponse;
import com.heydie.ecompayment.payment.messaging.dto.PaymentRequestedEvent;
import com.heydie.ecompayment.repository.InboxRepository;
import com.heydie.ecompayment.repository.OutboxRepository;
import com.heydie.ecompayment.repository.PaymentRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "midtrans.server-key=SB-Mid-server-test")
@Testcontainers
@EmbeddedKafka(
        topics = {"payment.request.v1", "payment.request.v1.DLT"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class PaymentRequestListenerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @MockitoBean
    MidtransClient midtransClient;

    @Autowired KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired PaymentRepository paymentRepository;
    @Autowired OutboxRepository outboxRepository;
    @Autowired InboxRepository inboxRepository;
    @Autowired EmbeddedKafkaBroker broker;

    @BeforeEach
    void clean() {
        outboxRepository.deleteAll();
        paymentRepository.deleteAll();
        inboxRepository.deleteAll();
        reset(midtransClient);
    }

    @AfterEach
    void resetMock() {
        reset(midtransClient);
    }

    @Test
    void consumesEvent_createsPaymentInboxAndOutbox() {
        when(midtransClient.createSnapTransaction(any()))
                .thenReturn(new SnapChargeResponse("tok-1", "https://sandbox/vtweb/tok-1"));

        kafkaTemplate.send("payment.request.v1", "ORD-9",
                new PaymentRequestedEvent("evt-9", "ORD-9", BigInteger.valueOf(250_000), "IDR", 30));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(paymentRepository.findByOrderId("ORD-9")).isPresent();
            assertThat(outboxRepository.findAll()).hasSize(1);
            assertThat(inboxRepository.existsById("evt-9")).isTrue();
        });
    }

    @Test
    void duplicateEventId_isProcessedOnce() {
        when(midtransClient.createSnapTransaction(any()))
                .thenReturn(new SnapChargeResponse("tok-1", "url"));

        var e = new PaymentRequestedEvent("evt-dup", "ORD-DUP", BigInteger.TEN, "IDR", 30);
        kafkaTemplate.send("payment.request.v1", "ORD-DUP", e);
        kafkaTemplate.send("payment.request.v1", "ORD-DUP", e);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(paymentRepository.findByOrderId("ORD-DUP")).isPresent());

        verify(midtransClient, timeout(3_000).times(1)).createSnapTransaction(any());
        assertThat(outboxRepository.findAll()).hasSize(1);
    }

    @Test
    void whenProcessingFails_messageGoesToDlt() {
        when(midtransClient.createSnapTransaction(any()))
                .thenThrow(new MidtransException("Snap ditolak (401)"));

        kafkaTemplate.send("payment.request.v1", "ORD-BAD",
                new PaymentRequestedEvent("evt-bad", "ORD-BAD", BigInteger.TEN, "IDR", 30));

        try (Consumer<String, String> dlt = dltConsumer()) {
            ConsumerRecord<String, String> rec = KafkaTestUtils.getSingleRecord(
                    dlt, "payment.request.v1.DLT", Duration.ofSeconds(25));
            assertThat(rec.key()).isEqualTo("ORD-BAD");
        }

        // 3 percobaan lalu DLT
        verify(midtransClient, timeout(25_000).times(3)).createSnapTransaction(any());
        // seluruh tx rollback → tidak ada row
        assertThat(paymentRepository.findByOrderId("ORD-BAD")).isEmpty();
        assertThat(inboxRepository.existsById("evt-bad")).isFalse();
    }

    private Consumer<String, String> dltConsumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps("dlt-test-group", "true", broker);
        DefaultKafkaConsumerFactory<String, String> cf =
                new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer());
        Consumer<String, String> consumer = cf.createConsumer();
        broker.consumeFromAnEmbeddedTopic(consumer, "payment.request.v1.DLT");
        return consumer;
    }
}
