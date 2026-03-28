package com.ecommerce.controller;

import com.ecommerce.dto.PaymentSessionRequest;
import com.ecommerce.dto.PaymentSessionResponse;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.security.SecurityUtils;
import com.ecommerce.service.PaymentService;
import com.ecommerce.service.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final StripeWebhookService stripeWebhookService;

    public PaymentController(PaymentService paymentService, StripeWebhookService stripeWebhookService) {
        this.paymentService = paymentService;
        this.stripeWebhookService = stripeWebhookService;
    }

    @PostMapping("/create-session")
    public PaymentSessionResponse createSession(@Valid @RequestBody PaymentSessionRequest request) {
        try {
            return paymentService.createCheckoutSession(SecurityUtils.currentUserId(), request);
        } catch (StripeException e) {
            throw new BusinessException("Payment session could not be created: " + e.getMessage());
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String stripeSignature) {
        try {
            stripeWebhookService.handleWebhook(payload, stripeSignature);
            return ResponseEntity.ok().build();
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
