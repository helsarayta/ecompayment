package com.heydie.ecompayment.midtrans;

import com.heydie.ecompayment.config.MidtransProperties;
import com.heydie.ecompayment.midtrans.dto.SnapChargeRequest;
import com.heydie.ecompayment.midtrans.dto.SnapChargeResponse;
import com.heydie.ecompayment.midtrans.dto.TransactionStatusResponse;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class MidtransClient {

    private final RestClient midtransRestClient;
    private final MidtransProperties props;

    public MidtransClient(RestClient midtransRestClient, MidtransProperties props) {
        this.midtransRestClient = midtransRestClient;
        this.props = props;
    }

    @Retryable(includes = {HttpServerErrorException.class, ResourceAccessException.class},
    maxRetries = 2, delayString = "500ms", multiplier = 2.0)
    public SnapChargeResponse createSnapTransaction(SnapChargeRequest request) {
        try {
            return midtransRestClient.post()
                    .uri(props.snapTransactionUrl())
                    .body(request)
                    .retrieve()
                    .body(SnapChargeResponse.class);
        } catch (HttpClientErrorException e) {
            throw new MidtransException(
                    "Snap charge ditolak Midtrans (" + e.getStatusCode() + "):" +
                            e.getResponseBodyAsString(), e
            );
        }
    }

    @Retryable(includes = {HttpServerErrorException.class, ResourceAccessException.class},
            maxRetries = 2, delayString = "500ms", multiplier = 2.0)
    public TransactionStatusResponse getTransactionStatus(String midtransOrderId) {
        try {
            return midtransRestClient.get()
                    .uri(props.transactionStatusUrl(midtransOrderId))
                    .retrieve()
                    .body(TransactionStatusResponse.class);

        } catch (HttpClientErrorException.NotFound e) {
            throw new MidtransException(
                    "Transaksi tidak ada di Midtrans: "+ midtransOrderId, e);
        } catch (HttpClientErrorException e) {
            throw new MidtransException(
                    "Get status ditolak Midtrans (" + e.getStatusCode() + "): " +
                            e.getResponseBodyAsString(), e);
        }
    }
}
