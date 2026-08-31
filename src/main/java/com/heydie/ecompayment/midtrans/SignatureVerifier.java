package com.heydie.ecompayment.midtrans;

import com.heydie.ecompayment.config.MidtransProperties;
import com.heydie.ecompayment.midtrans.dto.MidtransNotification;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class SignatureVerifier {

    private final MidtransProperties props;

    public SignatureVerifier(MidtransProperties props) {
        this.props = props;
    }

    public boolean isValid(MidtransNotification n) {
        return isValid(n.orderId(), n.statusCode(), n.grossAmount(), n.signatureKey());
    }

    private boolean isValid(String orderId, String statusCode, String grossAmount, String receiveSignature) {
        if(receiveSignature == null || orderId == null || statusCode == null || grossAmount == null) {
            return false;
        }

        String expected = sha512Hex(orderId + statusCode + grossAmount + props.serverKey());

        // constant-time compare
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                receiveSignature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    private String sha512Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-512")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 tidak tersedia", e);
        }
    }
}
