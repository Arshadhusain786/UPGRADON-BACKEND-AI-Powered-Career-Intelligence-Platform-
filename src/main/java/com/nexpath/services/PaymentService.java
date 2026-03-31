package com.nexpath.services;

import com.nexpath.dtos.response.PaymentOrderResponse;
import com.nexpath.enums.CreditPackage;
import com.nexpath.enums.CreditTransactionType;
import com.nexpath.enums.PaymentStatus;
import com.nexpath.exceptions.BadRequestException;
import com.nexpath.exceptions.PaymentException;
import com.nexpath.models.Payment;
import com.nexpath.models.User;
import com.nexpath.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CreditService creditService;

    @Value("${app.razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${app.razorpay.key-secret}")
    private String razorpayKeySecret;

    // ─────────────────────────────────────────
    // Create Razorpay order
    // ─────────────────────────────────────────
    public PaymentOrderResponse createOrder(User user, String packageName) {
        CreditPackage pkg;
        try {
            pkg = CreditPackage.valueOf(packageName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid package. Choose STARTER, PRO, or POWER.");
        }

        try {
            log.info("💳 [V2-FIX] Entering PaymentService.createOrder for package: {}", packageName);
            
            // Trim keys to avoid hidden character issues with environment variables
            String trimmedId = razorpayKeyId != null ? razorpayKeyId.trim() : "";
            String trimmedSecret = razorpayKeySecret != null ? razorpayKeySecret.trim() : "";

            // [SMART-FIX] Common typo correction: if Key ID is "zp_test_...", it should be "rzp_test_..."
            if (trimmedId.startsWith("zp_test_")) {
                log.warn("⚠️ [V2-SMART-FIX] Detected potential typo in Razorpay Key ID ('zp_' prefix). Correcting to 'rzp_'.");
                trimmedId = "r" + trimmedId;
            }

            // Masked logging for debugging key injection (safe)
            String maskedId = trimmedId.length() > 8 ? trimmedId.substring(0, 8) + "..." : "invalid";
            log.info("Razorpay Config Check -> KeyID: {} (len: {}), Secret: [MASKED]", 
                    maskedId, trimmedId.length());

            RazorpayClient client = new RazorpayClient(trimmedId, trimmedSecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", pkg.getAmountPaise());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "order_" + user.getId() + "_" + System.currentTimeMillis());

            Order order = client.orders.create(orderRequest);
            String orderId = order.get("id");

            Payment payment = Payment.builder()
                    .user(user)
                    .razorpayOrderId(orderId)
                    .amountPaise(pkg.getAmountPaise())
                    .creditsToAdd(pkg.getCredits())
                    .status(PaymentStatus.PENDING)
                    .build();

            paymentRepository.save(payment);

            log.info("Razorpay order created: {} for user: {} package: {}",
                    orderId, user.getEmail(), pkg.getDisplayName());

            return new PaymentOrderResponse(
                    orderId,
                    pkg.getAmountPaise(),
                    "INR",
                    trimmedId, // Send corrected ID to frontend
                    pkg.getCredits(),
                    pkg.getDisplayName()
            );

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create Razorpay order", e);
            throw new PaymentException("Failed to create payment order: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────
    // Verify Razorpay payment signature and credit user
    // ─────────────────────────────────────────
    public void verifyPayment(User user, String razorpayOrderId,
                              String razorpayPaymentId, String razorpaySignature) {

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new PaymentException("Payment order not found"));

        // Verify HMAC-SHA256 signature
        String payload = razorpayOrderId + "|" + razorpayPaymentId;
        String trimmedSecret = razorpayKeySecret != null ? razorpayKeySecret.trim() : "";
        String generatedSignature = hmacSha256(payload, trimmedSecret);

        if (!generatedSignature.equals(razorpaySignature)) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new PaymentException("Invalid payment signature. Possible fraud attempt.");
        }

        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setRazorpaySignature(razorpaySignature);
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        if (payment.getAmountPaise() == 2000 && payment.getCreditsToAdd() == 3) {
            // Native Connection Refill
            creditService.addFreeConnections(user, 3, razorpayPaymentId);
            log.info("Payment verified for user {} — 3 Free Connections Refill applied", user.getEmail());
        } else {
            // General Credit Addition
            creditService.addCredits(
                    user,
                    payment.getCreditsToAdd(),
                    CreditTransactionType.PURCHASE,
                    "Credits purchased — " + payment.getCreditsToAdd() + " credits",
                    razorpayPaymentId
            );
            log.info("Payment verified for user {} — {} credits added",
                    user.getEmail(), payment.getCreditsToAdd());
        }
    }

    // ─────────────────────────────────────────
    // HMAC-SHA256 helper
    // ─────────────────────────────────────────
    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (Exception e) {
            throw new PaymentException("Failed to compute payment signature");
        }
    }
}
