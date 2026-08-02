package com.project.agent.domain.vo.billing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    void zeroHasZeroAmount() {
        assertEquals(0, Money.zero(USD).amount().compareTo(BigDecimal.ZERO));
        assertEquals(USD, Money.zero(USD).currency());
    }

    @Test
    void amountIsScaledToFourDecimals() {
        Money money = Money.of("1.5", USD);
        assertEquals(new BigDecimal("1.5000"), money.amount());
    }

    @Test
    void addRequiresMatchingCurrency() {
        Money usd = Money.of("1.00", USD);
        assertEquals(0, usd.add(Money.of("2.00", USD)).amount().compareTo(new BigDecimal("3")));
        assertThrows(IllegalArgumentException.class, () -> usd.add(Money.of("1.00", EUR)));
    }

    @Test
    void negativeAmountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("-1.00", USD));
    }
}
