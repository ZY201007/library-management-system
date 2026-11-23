package com.library.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 逾期计费单元测试：纯逻辑，无需数据库。 */
class FineCalculatorTest {

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "期望 " + expected + "，实际 " + actual);
    }

    @Test
    void returnOnOrBeforeDueDateIsFree() {
        assertMoney("0.00", FineCalculator.calculate(
                LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 10)));
        assertMoney("0.00", FineCalculator.calculate(
                LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 5)));
    }

    @Test
    void tenDaysOverdueAtDailyRate() {
        // 0.50 元/天 × 10 天 = 5.00 元
        assertMoney("5.00", FineCalculator.calculate(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 11)));
    }

    @Test
    void fineIsCappedAtFiftyYuan() {
        // 200 天 × 0.50 = 100.00，封顶后应为 50.00
        assertMoney("50.00", FineCalculator.calculate(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 7, 20)));
    }

    @Test
    void exactlyOneHundredDaysHitsCap() {
        // 100 天 × 0.50 = 50.00，恰好等于封顶值
        assertMoney("50.00", FineCalculator.calculate(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 4, 11)));
    }

    @Test
    void overdueDaysNeverNegative() {
        assertEquals(0, FineCalculator.overdueDays(
                LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 9)));
        assertEquals(3, FineCalculator.overdueDays(
                LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 13)));
    }
}
