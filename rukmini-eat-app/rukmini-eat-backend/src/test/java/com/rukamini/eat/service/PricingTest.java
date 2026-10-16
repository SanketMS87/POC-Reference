package com.rukamini.eat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PricingTest {

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    @Test
    void chargesDeliveryFeeBelowThreshold() {
        Pricing.Totals t = Pricing.of(new BigDecimal("200"));
        assertMoney("40", t.deliveryFee());
        assertMoney("10.00", t.tax());
        assertMoney("5", t.platformFee());
        assertMoney("255.00", t.total());
    }

    @Test
    void freeDeliveryAtOrAboveThreshold() {
        Pricing.Totals t = Pricing.of(new BigDecimal("400"));
        assertMoney("0", t.deliveryFee());
        assertMoney("425.00", t.total());
    }

    @Test
    void emptyCartCostsNothing() {
        assertMoney("0", Pricing.of(BigDecimal.ZERO).total());
    }
}
