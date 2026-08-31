package com.heydie.ecompayment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient midtransRestClient(MidtransProperties props) {
        if(props.serverKey() == null || props.serverKey().isBlank()) {
            throw new IllegalStateException(
                    "midtrans.server-key belum di-set (env MIDTRANS_SERVER_KEY / config/local.properties)"
            );
        }
            HttpClient httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(props.connectTimeOut())
                    .build();

            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
            factory.setReadTimeout(props.readTimeOut());

            String basicToken = Base64.getEncoder()
                    .encodeToString((props.serverKey() + ":").getBytes(StandardCharsets.UTF_8));

            return RestClient.builder()
                    .requestFactory(factory)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basicToken)
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

    }
}
