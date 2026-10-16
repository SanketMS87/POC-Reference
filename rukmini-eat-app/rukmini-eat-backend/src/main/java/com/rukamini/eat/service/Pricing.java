package com.rukamini.eat.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Order pricing rules, shared by checkout and tests. */
public final class Pricing {

    public static final BigDecimal FREE_DELIVERY_MIN = new BigDecimal("399");
    public static final BigDecimal DELIVERY_FEE = new BigDecimal("40");
    public static final BigDecimal PLATFORM_FEE = new BigDecimal("5");
    public static final BigDecimal GST_RATE = new BigDecimal("0.05");

    private Pricing() {
    }

    public record Totals(BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal tax,
                         BigDecimal platformFee, BigDecimal total) {
    }

    public static Totals of(BigDecimal subtotal) {
        boolean empty = subtotal.signum() == 0;
        BigDecimal delivery = empty || subtotal.compareTo(FREE_DELIVERY_MIN) >= 0 ? BigDecimal.ZERO : DELIVERY_FEE;
        BigDecimal platform = empty ? BigDecimal.ZERO : PLATFORM_FEE;
        BigDecimal tax = subtotal.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(delivery).add(tax).add(platform);
        return new Totals(subtotal, delivery, tax, platform, total);
    }
}
