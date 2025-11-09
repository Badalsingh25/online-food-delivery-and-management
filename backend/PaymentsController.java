package com.hungerexpress.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Refund;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments")
public class PaymentsController {

    @Value("${app.razorpay.key-id:rzp_test_xxxxx}")
    private String keyId;
    @Value("${app.razorpay.key-secret:xxxx}")
    private String keySecret;
    @Value("${app.razorpay.webhook-secret:xxxx}")
    private String webhookSecret;

    private static final Logger log = LoggerFactory.getLogger(PaymentsController.class);
    private final PaymentRepository payments;
    private final PaymentWebhookEventRepository webhookEvents;

    public PaymentsController(PaymentRepository payments, PaymentWebhookEventRepository webhookEvents){
        this.payments = payments;
        this.webhookEvents = webhookEvents;
    }

    @PostMapping("/order")
    public Map<String,Object> createOrder(@RequestBody Map<String,Object> req) throws Exception {
        long amount = ((Number)req.getOrDefault("amount", 0)).longValue(); // in paise
        String receipt = (String) req.getOrDefault("receipt", ("rcpt_"+System.currentTimeMillis()));

        RazorpayClient client = new RazorpayClient(keyId, keySecret);
        JSONObject options = new JSONObject();
        options.put("amount", amount);
        options.put("currency", "INR");
        options.put("receipt", receipt);

        Order order = client.orders.create(options);

        // persist a payment stub (order link will be attached after checkout when FE posts /api/orders)
        PaymentEntity p = PaymentEntity.builder()
                .provider("RAZORPAY")
                .providerOrderId(order.get("id"))
                .status("CREATED")
                .amount(new java.math.BigDecimal(order.get("amount").toString()).divide(new java.math.BigDecimal("100")))
                .build();
        payments.save(p);

        Map<String,Object> res = new HashMap<>();
        res.put("orderId", order.get("id"));
        res.put("amount", order.get("amount"));
        res.put("currency", order.get("currency"));
        res.put("keyId", keyId);
        return res;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload, @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature){
        if (signature == null || signature.isBlank()) return ResponseEntity.status(400).build();
        try {
            String computed = hmacSha256(payload, webhookSecret);
            if (!computed.equals(signature)) {
                log.warn("Invalid Razorpay webhook signature");
                return ResponseEntity.status(400).build();
            }
            JSONObject evt = new JSONObject(payload);
            String type = evt.optString("event", "");
            String eventId = evt.optString("id", null);
            JSONObject pl = evt.optJSONObject("payload");
            log.info("Webhook verified, type={}, keys={} ", type, pl != null ? pl.keySet() : "{}");

            // Idempotency: skip if event already processed
            if (eventId != null && webhookEvents.findByEventId(eventId).isPresent()){
                return ResponseEntity.ok().build();
            }
            // Persist receipt
            String sha = sha256Hex(payload);
            webhookEvents.save(PaymentWebhookEvent.builder()
                    .eventId(eventId)
                    .eventType(type)
                    .signature(signature)
                    .payloadSha256(sha)
                    .build());

            // Handle payment events
            if (pl != null && pl.has("payment")) {
                try {
                    JSONObject pay = pl.getJSONObject("payment").optJSONObject("entity");
                    if (pay != null) {
                        String orderId = pay.optString("order_id", null);
                        String paymentId = pay.optString("id", null);
                        String pstatus = pay.optString("status", "").toUpperCase();
                        if (orderId != null) {
                            payments.findByProviderOrderId(orderId).ifPresent(p -> {
                                if (paymentId != null && !paymentId.isBlank()) p.setProviderPaymentId(paymentId);
                                // Map Razorpay status to our status
                                switch (pstatus) {
                                    case "CAPTURED" -> p.setStatus("CAPTURED");
                                    case "AUTHORIZED" -> p.setStatus("AUTHORIZED");
                                    case "FAILED" -> p.setStatus("FAILED");
                                    case "REFUNDED" -> p.setStatus("REFUNDED");
                                    default -> { /* keep existing */ }
                                }
                                payments.save(p);
                            });
                        }
                    }
                } catch (Exception ex){ log.warn("Failed to reconcile payment event", ex); }
            }

            // Handle order events
            if (pl != null && pl.has("order")) {
                try {
                    JSONObject ord = pl.getJSONObject("order").optJSONObject("entity");
                    if (ord != null) {
                        String orderId = ord.optString("id", null);
                        String status = ord.optString("status", "").toUpperCase();
                        if (orderId != null) {
                            payments.findByProviderOrderId(orderId).ifPresent(p -> {
                                if ("PAID".equals(status)) p.setStatus("CAPTURED");
                                payments.save(p);
                            });
                        }
                    }
                } catch (Exception ex){ log.warn("Failed to reconcile order event", ex); }
            }

            return ResponseEntity.ok().build();
        } catch (Exception e){
            log.error("Webhook verification failed", e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/refund/{orderId}")
    public ResponseEntity<?> refund(@PathVariable Long orderId){
        return payments.findTopByOrder_IdOrderByCreatedAtDesc(orderId).map(p -> {
            try {
                if (p.getProviderPaymentId() == null || p.getProviderPaymentId().isBlank()){
                    p.setStatus("REFUND_REQUESTED");
                    payments.save(p);
                    Map<String,Object> res = new HashMap<>();
                    res.put("status", p.getStatus());
                    return ResponseEntity.accepted().body(res);
                }
                RazorpayClient client = new RazorpayClient(keyId, keySecret);
                JSONObject opts = new JSONObject();
                // Refund full captured amount
                BigDecimal amt = p.getAmount();
                if (amt != null) opts.put("amount", amt.multiply(new BigDecimal("100")).intValue()); // paise
                Refund r = client.payments.refund(p.getProviderPaymentId(), opts);
                p.setStatus("REFUNDED");
                payments.save(p);
                Map<String,Object> res = new HashMap<>();
                res.put("refundId", r.get("id"));
                res.put("status", p.getStatus());
                return ResponseEntity.ok(res);
            } catch (Exception e){
                log.error("Refund failed", e);
                return ResponseEntity.status(500).build();
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    private static String hmacSha256(String data, String secret) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] hash = sha256_HMAC.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static String sha256Hex(String data) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
