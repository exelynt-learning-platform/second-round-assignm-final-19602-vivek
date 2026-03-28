package com.ecommerce.service;

import com.ecommerce.config.StripeProperties;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.Product;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

    private final StripeProperties stripeProperties;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public StripeWebhookService(
            StripeProperties stripeProperties,
            OrderRepository orderRepository,
            ProductRepository productRepository) {
        this.stripeProperties = stripeProperties;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public void handleWebhook(String payload, String stripeSignature) throws SignatureVerificationException {
        Event event = Webhook.constructEvent(
                payload,
                stripeSignature,
                stripeProperties.getWebhookSecret());
        log.info("Stripe webhook type={}", event.getType());
        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutSessionCompleted(event);
            case "checkout.session.async_payment_failed" -> handleCheckoutSessionFailed(event);
            case "payment_intent.payment_failed" -> handlePaymentIntentFailed(event);
            default -> log.debug("Ignoring Stripe event type {}", event.getType());
        }
    }

    private void handleCheckoutSessionCompleted(Event event) {
        Session session = deserializeSession(event);
        if (session == null) {
            return;
        }
        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            log.info("Session {} payment status is {}, skipping PAID", session.getId(), session.getPaymentStatus());
            return;
        }
        Order order = resolveOrder(session);
        if (order == null) {
            return;
        }
        if (order.getStatus() == OrderStatus.PAID) {
            log.info("Order {} already PAID", order.getId());
            return;
        }
        if (order.getStatus() != OrderStatus.CREATED) {
            log.warn("Order {} not in CREATED state, skipping", order.getId());
            return;
        }
        Order full = orderRepository.findByIdWithDetails(order.getId()).orElse(order);
        for (OrderItem oi : full.getItems()) {
            Product p = productRepository.findByIdForUpdate(oi.getProduct().getId())
                    .orElseThrow(() -> new IllegalStateException("Product missing: " + oi.getProduct().getId()));
            if (p.getStockQuantity() < oi.getQuantity()) {
                log.error("Insufficient stock for order {} product {}", full.getId(), p.getId());
                full.setStatus(OrderStatus.FAILED);
                orderRepository.save(full);
                return;
            }
            p.setStockQuantity(p.getStockQuantity() - oi.getQuantity());
            productRepository.save(p);
        }
        full.setStatus(OrderStatus.PAID);
        orderRepository.save(full);
        log.info("Order {} marked PAID", full.getId());
    }

    private void handleCheckoutSessionFailed(Event event) {
        Session session = deserializeSession(event);
        failOrder(session);
    }

    private void handlePaymentIntentFailed(Event event) {
        if (event.getDataObjectDeserializer().getObject().isEmpty()) {
            return;
        }
        com.stripe.model.PaymentIntent pi =
                (com.stripe.model.PaymentIntent) event.getDataObjectDeserializer().getObject().get();
        if (pi.getMetadata() != null && pi.getMetadata().get("orderId") != null) {
            Long orderId = Long.parseLong(pi.getMetadata().get("orderId"));
            orderRepository.findById(orderId).ifPresent(o -> {
                if (o.getStatus() == OrderStatus.CREATED) {
                    o.setStatus(OrderStatus.FAILED);
                    orderRepository.save(o);
                    log.info("Order {} marked FAILED from payment_intent", o.getId());
                }
            });
        }
    }

    private void failOrder(Session session) {
        if (session == null) {
            return;
        }
        Order order = resolveOrder(session);
        if (order == null) {
            return;
        }
        if (order.getStatus() == OrderStatus.CREATED) {
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
            log.info("Order {} marked FAILED", order.getId());
        }
    }

    private Order resolveOrder(Session session) {
        String orderIdStr = session.getMetadata() != null ? session.getMetadata().get("orderId") : null;
        if (orderIdStr != null) {
            Long orderId = Long.parseLong(orderIdStr);
            return orderRepository.findById(orderId).orElse(null);
        }
        if (session.getId() != null) {
            return orderRepository.findByStripeCheckoutSessionId(session.getId()).orElse(null);
        }
        return null;
    }

    private static Session deserializeSession(Event event) {
        if (event.getDataObjectDeserializer().getObject().isEmpty()) {
            log.warn("Stripe event has no deserialized object");
            return null;
        }
        return (Session) event.getDataObjectDeserializer().getObject().get();
    }
}
