package com.byteforge.facade;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Subsystem used by the facade: a simulated card processor with the usual two-step
 * flow of real gateways: <b>authorize</b> (hold the money) and later <b>capture</b>
 * (take it) or <b>void</b> (release the hold).
 * Card numbers ending in "0000" are declined so failures can be demonstrated.
 */
public class PaymentGateway {

    private final AtomicInteger seq = new AtomicInteger(5000);
    private final Map<String, Double> holds = new HashMap<>();

    /** Places a hold on the card; returns an authorization id or throws if declined. */
    public synchronized String authorize(String customer, String cardLast4, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (cardLast4 == null || !cardLast4.matches("\\d{4}")) {
            throw new PaymentDeclinedException("Enter the last 4 digits of the card");
        }
        if ("0000".equals(cardLast4)) {
            throw new PaymentDeclinedException("Card ****" + cardLast4 + " was declined");
        }
        String id = "AUTH-" + seq.incrementAndGet();
        holds.put(id, amount);
        return id;
    }

    /** Takes the held money; returns the transaction id. */
    public synchronized String capture(String authId) {
        if (holds.remove(authId) == null) {
            throw new IllegalStateException("Unknown or already used authorization " + authId);
        }
        return authId.replace("AUTH", "TX");
    }

    /** Releases a hold that will not be captured. */
    public synchronized void voidAuthorization(String authId) {
        holds.remove(authId);
    }

    private final Map<String, Double> refunds = new HashMap<>();

    /** Returns captured money to the card. A transaction can be refunded only once. */
    public synchronized void refund(String txId, double amount) {
        if (txId == null || !txId.startsWith("TX-")) {
            throw new IllegalArgumentException("Unknown transaction " + txId);
        }
        if (refunds.putIfAbsent(txId, amount) != null) {
            throw new IllegalStateException("Transaction " + txId + " was already refunded");
        }
    }

    public synchronized double refundedTotal() {
        return refunds.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public synchronized int openHolds() {
        return holds.size();
    }

    public static class PaymentDeclinedException extends RuntimeException {
        public PaymentDeclinedException(String message) {
            super(message);
        }
    }
}
