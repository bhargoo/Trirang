package com.trirang.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.trirang.model.dto.response.QrGenerationResponse;
import com.trirang.model.dto.response.QrVerificationResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class QrService {

    private final SecretKey secretKey;
    private final long qrExpirationMs; // Default 24 hours

    public QrService(
            @Value("${security.jwt.qr-secret:dHJpcmFuZy1zZWNyZXQtc2lnbmluZy1rZXktZm9yLXFyLXZlcmlmaWNhdGlvbi1zZWN1cml0eQ==}") String secret,
            @Value("${security.jwt.qr-expiration-ms:86400000}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.qrExpirationMs = expirationMs;
    }

    public QrGenerationResponse generateQrForMatch(UUID matchId, UUID generatorUserId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(qrExpirationMs, ChronoUnit.MILLIS);

        String token = Jwts.builder()
                .claim("matchId", matchId.toString())
                .claim("generatorId", generatorUserId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();

        String qrImageBase64 = generateQrImage(token);

        return QrGenerationResponse.builder()
                .qrImageBase64(qrImageBase64)
                .token(token)
                .build();
    }

    public QrVerificationResponse verifyQrForMatch(String token, UUID expectedMatchId) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID tokenMatchId = UUID.fromString(claims.get("matchId", String.class));
            
            if (!tokenMatchId.equals(expectedMatchId)) {
                return QrVerificationResponse.builder()
                        .isValid(false)
                        .message("QR Code does not belong to this match")
                        .build();
            }

            return QrVerificationResponse.builder()
                    .isValid(true)
                    .matchId(tokenMatchId)
                    .message("QR Code verified successfully")
                    .verifiedAt(Instant.now())
                    .build();

        } catch (Exception e) {
            log.error("QR Verification failed: {}", e.getMessage());
            return QrVerificationResponse.builder()
                    .isValid(false)
                    .message("Invalid or expired QR Code")
                    .build();
        }
    }

    private String generateQrImage(String text) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 250, 250);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();
            
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngData);
        } catch (Exception e) {
            log.error("Error generating QR code image", e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }
}
