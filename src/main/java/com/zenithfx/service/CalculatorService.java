package com.zenithfx.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Four-operation calculator used by the Test menu.
 * BigDecimal is used so that results such as 0.1 + 0.2 are exact (0.3).
 */
@Service
public class CalculatorService {

    public BigDecimal add(BigDecimal a, BigDecimal b) {
        return a.add(b);
    }

    public BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return a.subtract(b);
    }

    public BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return a.multiply(b);
    }

    public BigDecimal divide(BigDecimal a, BigDecimal b) {
        if (b.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Division by zero is not allowed.");
        }
        return a.divide(b, 10, RoundingMode.HALF_UP);
    }

    /** operation is one of: add, subtract, multiply, divide. */
    public BigDecimal calculate(BigDecimal a, BigDecimal b, String operation) {
        BigDecimal result;
        if ("add".equals(operation)) {
            result = add(a, b);
        } else if ("subtract".equals(operation)) {
            result = subtract(a, b);
        } else if ("multiply".equals(operation)) {
            result = multiply(a, b);
        } else if ("divide".equals(operation)) {
            result = divide(a, b);
        } else {
            throw new IllegalArgumentException("Unknown operation: " + operation);
        }
        return clean(result);
    }

    /** Removes trailing zeros (2.5000000000 becomes 2.5, 5.0 becomes 5). */
    private BigDecimal clean(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.stripTrailingZeros();
    }
}
