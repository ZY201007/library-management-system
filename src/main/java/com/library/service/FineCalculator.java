package com.library.service;

import com.library.config.AppConfig;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 逾期罚款计算器：纯业务规则，无数据库依赖，便于单元测试。
 *
 * <p>规则：罚款 = 逾期天数 × 每日费率(0.50 元)，超过封顶金额(50.00 元)按封顶收取；
 * 归还日期早于或等于应还日期时不计费。</p>
 */
public final class FineCalculator {

    private FineCalculator() {
    }

    /** 逾期天数：仅当归还日晚于应还日时计数，否则为 0。 */
    public static int overdueDays(LocalDate dueDate, LocalDate returnDate) {
        if (dueDate == null || returnDate == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(dueDate, returnDate);
        return days > 0 ? (int) days : 0;
    }

    /** 计算罚款金额，保留两位小数（四舍五入）。 */
    public static BigDecimal calculate(LocalDate dueDate, LocalDate returnDate) {
        int days = overdueDays(dueDate, returnDate);
        if (days <= AppConfig.FREE_GRACE_DAYS) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        int billableDays = days - AppConfig.FREE_GRACE_DAYS;
        BigDecimal fine = AppConfig.DAILY_FINE_RATE.multiply(BigDecimal.valueOf(billableDays));
        return fine.min(AppConfig.MAX_FINE).setScale(2, RoundingMode.HALF_UP);
    }
}
