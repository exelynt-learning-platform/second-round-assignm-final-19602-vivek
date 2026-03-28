package com.ecommerce.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {

    private String secretKey;
    /** Optional; for Stripe.js / frontend. Not used by server checkout flow. */
    private String publishableKey;
    private String webhookSecret;
    private String successUrl;
    private String cancelUrl;
}
