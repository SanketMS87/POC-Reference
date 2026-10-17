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

    @Test
    void deliveryThresholdIsInclusiveAtExactly399() {
        assertMoney("40", Pricing.of(new BigDecimal("398.99")).deliveryFee());
        assertMoney("0", Pricing.of(new BigDecimal("399")).deliveryFee());
        assertMoney("0", Pricing.of(new BigDecimal("399.01")).deliveryFee());
    }

    @Test
    void taxIsRoundedHalfUpToTwoDecimals() {
        assertMoney("0.51", Pricing.of(new BigDecimal("10.10")).tax());
        assertMoney("0.50", Pricing.of(new BigDecimal("10.09")).tax());
        assertMoney("9.95", Pricing.of(new BigDecimal("199")).tax());
        assertEquals(2, Pricing.of(new BigDecimal("199")).tax().scale());
    }

    @Test
    void totalIsSubtotalPlusDeliveryTaxAndPlatformFee() {
        Pricing.Totals t = Pricing.of(new BigDecimal("150"));
        assertMoney("150", t.subtotal());
        assertMoney(t.subtotal().add(t.deliveryFee()).add(t.tax()).add(t.platformFee()).toPlainString(), t.total());
        assertMoney("202.50", t.total());
    }

    @Test
    void largeOrdersStillPayPlatformFeeAndTax() {
        Pricing.Totals t = Pricing.of(new BigDecimal("12345.67"));
        assertMoney("0", t.deliveryFee());
        assertMoney("5", t.platformFee());
        assertMoney("617.28", t.tax());
        assertMoney("12967.95", t.total());
    }

    @Test
    void feeConstantsMatchThePublishedRules() {
        assertMoney("399", Pricing.FREE_DELIVERY_MIN);
        assertMoney("40", Pricing.DELIVERY_FEE);
        assertMoney("5", Pricing.PLATFORM_FEE);
        assertMoney("0.05", Pricing.GST_RATE);
    }
}
