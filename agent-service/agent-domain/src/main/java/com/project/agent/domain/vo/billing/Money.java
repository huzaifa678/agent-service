package com.project.agent.domain.vo.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value object representing a monetary amount in a given ISO currency.
 * Amount is stored at 4 decimal places ({@link java.math.RoundingMode#HALF_UP});
 * negative values are rejected. Arithmetic preserves the currency and returns
 * new instances — {@link #add(Money)} guards against currency mismatches.
 */
public record Money(
        BigDecimal amount,
        Currency currency
) {

    public Money {

        Objects.requireNonNull(amount);
        Objects.requireNonNull(currency);

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }

        amount = amount.setScale(4, RoundingMode.HALF_UP);
    }

    /** Zero-value instance for the given currency; used as an initial cost placeholder. */
    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    /** Add another {@link Money} of the same currency; throws on currency mismatch. */
    public Money add(Money other) {

        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch");
        }

        return new Money(
                amount.add(other.amount),
                currency
        );
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money of(String amount, Currency currency) {
        return new Money(new BigDecimal(amount), currency);
    }
}
