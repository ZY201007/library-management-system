package com.library.config;

import java.math.BigDecimal;

/**
 * 业务规则常量集中配置：借期、续借上限、逾期费率与封顶金额。
 * 修改这里即可统一调整全局规则，Service 层不散落魔法数字。
 */
public final class AppConfig {

    /** 单本图书默认借期（天） */
    public static final int LOAN_DAYS = 30;

    /** 单本图书最大续借次数 */
    public static final int MAX_RENEW_TIMES = 2;

    /** 逾期宽限期（天）：宽限期内不计费 */
    public static final int FREE_GRACE_DAYS = 0;

    /** 逾期每日费率：0.50 元/天 */
    public static final BigDecimal DAILY_FINE_RATE = new BigDecimal("0.50");

    /** 单笔逾期罚款封顶金额：50.00 元 */
    public static final BigDecimal MAX_FINE = new BigDecimal("50.00");

    private AppConfig() {
    }
}
