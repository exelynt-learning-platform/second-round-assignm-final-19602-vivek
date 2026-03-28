package com.ecommerce.service;

import com.ecommerce.config.StripeProperties;
import com.ecommerce.dto.PaymentSessionRequest;
import com.ecommerce.dto.PaymentSessionResponse;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.OrderRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final OrderRepository orderRepository;
    private final StripeProperties stripeProperties;

    public PaymentService(OrderRepository orderRepository, StripeProperties stripeProperties) {
        this.orderRepository = orderRepository;
        this.stripeProperties = stripeProperties;
    }

    @Transactional
    public PaymentSessionResponse createCheckoutSession(Long userId, PaymentSessionRequest request) throws StripeException {
        Order order = orderRepository.findByIdWithDetails(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException("Order does not belong to current user");
        }
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new BusinessException("Order is not payable in current status");
        }
        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
        for (OrderItem oi : order.getItems()) {
            long unitAmountCents = oi.getPrice()
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValue();
            SessionCreateParams.LineItem line = SessionCreateParams.LineItem.builder()
                    .setQuantity((long) oi.getQuantity())
                    .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("usd")
                                    .setUnitAmount(unitAmountCents)
                                    .setProductData(
                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                    .setName(oi.getProduct().getName())
                                                    .build())
                                    .build())
                    .build();
            lineItems.add(line);
        }
        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .addAllLineItem(lineItems)
                .setSuccessUrl(stripeProperties.getSuccessUrl() + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(stripeProperties.getCancelUrl())
                .putMetadata("orderId", String.valueOf(order.getId()));
        SessionCreateParams params = paramsBuilder.build();
        Session session = Session.create(params);
        order.setStripeCheckoutSessionId(session.getId());
        orderRepository.save(order);
        log.info("Stripe session created orderId={} sessionId={}", order.getId(), session.getId());
        return PaymentSessionResponse.builder()
                .sessionId(session.getId())
                .checkoutUrl(session.getUrl())
                .build();
    }
}
