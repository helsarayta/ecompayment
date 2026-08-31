package com.heydie.ecompayment.midtrans;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.heydie.ecompayment.config.MidtransProperties;
import com.heydie.ecompayment.midtrans.dto.SnapChargeRequest;
import com.heydie.ecompayment.midtrans.dto.SnapChargeResponse;
import com.heydie.ecompayment.midtrans.dto.TransactionStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Mapping HTTP <-> DTO. Tanpa Spring context, jadi @Retryable TIDAK aktif.
 */
class MidtransClientTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private MidtransClient client;

    @BeforeEach
    void setUp() {
        MidtransProperties props = new MidtransProperties(
                false, "SB-Mid-server-test", wm.baseUrl(), wm.baseUrl(),
                Duration.ofSeconds(2), Duration.ofSeconds(2), null);

        // HTTP/1.1 — JDK HttpClient default HTTP/2 bentrok dgn WireMock (RST_STREAM)
        HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        RestClient restClient = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(http))
                .build();
        client = new MidtransClient(restClient, props);
    }

    @Test
    void createSnapTransaction_success_mapsResponse() {
        wm.stubFor(post("/snap/v1/transactions").willReturn(okJson("""
                {"token":"abc-123","redirect_url":"https://app.sandbox.midtrans.com/snap/v2/vtweb/abc-123"}
                """)));

        SnapChargeResponse res = client.createSnapTransaction(
                SnapChargeRequest.of("ORD-1-1", 250_000, 60, null));

        assertThat(res.token()).isEqualTo("abc-123");
        assertThat(res.redirectUrl()).contains("vtweb/abc-123");
    }

    @Test
    void createSnapTransaction_4xx_throwsMidtransException_noRetry() {
        wm.stubFor(post("/snap/v1/transactions")
                .willReturn(aResponse().withStatus(401).withBody("{\"error_messages\":[\"unauthorized\"]}")));

        assertThatThrownBy(() -> client.createSnapTransaction(SnapChargeRequest.of("ORD-1-1", 1, 0, null)))
                .isInstanceOf(MidtransException.class);

        wm.verify(exactly(1), postRequestedFor(urlEqualTo("/snap/v1/transactions")));
    }

    @Test
    void getTransactionStatus_success_mapsSnakeCaseFields() {
        wm.stubFor(get("/v2/ORD-1-1/status").willReturn(okJson("""
                {"order_id":"ORD-1-1","transaction_status":"settlement","fraud_status":"accept",
                 "status_code":"200","gross_amount":"250000.00","payment_type":"bank_transfer",
                 "signature_key":"abc"}
                """)));

        TransactionStatusResponse res = client.getTransactionStatus("ORD-1-1");

        assertThat(res.orderId()).isEqualTo("ORD-1-1");
        assertThat(res.transactionStatus()).isEqualTo("settlement");
        assertThat(res.grossAmount()).isEqualTo("250000.00");
        assertThat(res.paymentType()).isEqualTo("bank_transfer");
    }

    @Test
    void getTransactionStatus_404_throwsMidtransException() {
        wm.stubFor(get(urlPathMatching("/v2/.*/status"))
                .willReturn(aResponse().withStatus(404).withBody("{\"status_code\":\"404\"}")));

        assertThatThrownBy(() -> client.getTransactionStatus("ORD-1-1"))
                .isInstanceOf(MidtransException.class);
    }
}
