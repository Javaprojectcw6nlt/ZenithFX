package com.zenithfx.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculatorServiceTest {

    private CalculatorService calculator;

    @BeforeEach
    void setUp() {
        calculator = new CalculatorService();
    }

    /** Compares numeric value and ignores the scale (2.5 equals 2.50). */
    private void assertNumber(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected " + expected + " but was " + actual);
    }

    @Test
    void addsTwoNumbers() {
        assertNumber("5", calculator.add(new BigDecimal("2"), new BigDecimal("3")));
    }

    @Test
    void addsDecimalsExactly() {
        assertNumber("0.3", calculator.add(new BigDecimal("0.1"), new BigDecimal("0.2")));
    }

    @Test
    void subtractsTwoNumbers() {
        assertNumber("6", calculator.subtract(new BigDecimal("10"), new BigDecimal("4")));
    }

    @Test
    void subtractionCanBeNegative() {
        assertNumber("-2", calculator.subtract(new BigDecimal("3"), new BigDecimal("5")));
    }

    @Test
    void multipliesTwoNumbers() {
        assertNumber("42", calculator.multiply(new BigDecimal("6"), new BigDecimal("7")));
    }

    @Test
    void multiplyingByZeroGivesZero() {
        assertNumber("0", calculator.multiply(new BigDecimal("123.45"), BigDecimal.ZERO));
    }

    @Test
    void dividesTwoNumbers() {
        assertNumber("2.5", calculator.divide(new BigDecimal("10"), new BigDecimal("4")));
    }

    @Test
    void divisionIsRoundedToTenDecimals() {
        assertNumber("0.3333333333", calculator.divide(BigDecimal.ONE, new BigDecimal("3")));
    }

    @Test
    void divisionByZeroThrowsException() {
        ArithmeticException ex = assertThrows(ArithmeticException.class,
                () -> calculator.divide(BigDecimal.ONE, BigDecimal.ZERO));
        assertEquals("Division by zero is not allowed.", ex.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
            "add,2,3,5",
            "subtract,10,4,6",
            "multiply,6,7,42",
            "divide,10,4,2.5"
    })
    void calculateChoosesTheRightOperation(String operation, String a, String b, String expected) {
        BigDecimal result = calculator.calculate(new BigDecimal(a), new BigDecimal(b), operation);
        assertNumber(expected, result);
    }

    @Test
    void calculateRemovesTrailingZeros() {
        BigDecimal result = calculator.calculate(new BigDecimal("2.50"), new BigDecimal("2"), "multiply");
        assertEquals("5", result.toPlainString());
    }

    @Test
    void unknownOperationThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(BigDecimal.ONE, BigDecimal.ONE, "power"));
        assertEquals("Unknown operation: power", ex.getMessage());
    }

    @Test
    void nullOperationThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(BigDecimal.ONE, BigDecimal.ONE, null));
    }
}
