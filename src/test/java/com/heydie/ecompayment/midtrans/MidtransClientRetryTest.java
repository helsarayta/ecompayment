package com.heydie.ecompayment.midtrans;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.heydie.ecompayment.config.MidtransProperties;
import com.heydie.ecompayment.config.RestClientConfig;
import com.heydie.ecompayment.midtrans.dto.SnapChargeRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.HttpServerErrorException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Retry aktif karena ada proxy Spring (@EnableResilientMethods).
 */
@SpringJUnitConfig({RestClientConfig.class, MidtransClient.class})
@EnableConfigurationProperties(MidtransProperties.class)
@EnableResilientMethods
class MidtransClientRetryTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("midtrans.server-key", () -> "SB-Mid-server-test");
        registry.add("midtrans.snap-base-url", wm::baseUrl);
        registry.add("midtrans.api-base-url", wm::baseUrl);
    }

    @Autowired
    MidtransClient client;

    @Test
    void retries5xxThenPropagates() {
        wm.stubFor(post("/snap/v1/transactions").willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> client.createSnapTransaction(SnapChargeRequest.of("ORD-1-1", 1, 0, null)))
                .isInstanceOf(HttpServerErrorException.class);

        // 1 awal + 2 retry
        wm.verify(exactly(3), postRequestedFor(urlEqualTo("/snap/v1/transactions")));
    }

    @Test
    void doesNotRetry4xx() {
        wm.stubFor(post("/snap/v1/transactions").willReturn(aResponse().withStatus(400)));

        assertThatThrownBy(() -> client.createSnapTransaction(SnapChargeRequest.of("ORD-1-1", 1, 0, null)))
                .isInstanceOf(MidtransException.class);

        wm.verify(exactly(1), postRequestedFor(urlEqualTo("/snap/v1/transactions")));
    }
}
