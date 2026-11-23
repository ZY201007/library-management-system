package com.library.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 逾期罚款账单实体：对应 fine 表。 */
public class Fine {

    private Integer id;
    private Integer borrowId;
    private Integer readerId;
    private BigDecimal amount;
    private Integer daysOverdue;
    private Boolean paid;
    private LocalDateTime paidAt;

    public Fine() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getBorrowId() {
        return borrowId;
    }

    public void setBorrowId(Integer borrowId) {
        this.borrowId = borrowId;
    }

    public Integer getReaderId() {
        return readerId;
    }

    public void setReaderId(Integer readerId) {
        this.readerId = readerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getDaysOverdue() {
        return daysOverdue;
    }

    public void setDaysOverdue(Integer daysOverdue) {
        this.daysOverdue = daysOverdue;
    }

    public Boolean getPaid() {
        return paid;
    }

    public void setPaid(Boolean paid) {
        this.paid = paid;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    @Override
    public String toString() {
        return "Fine{id=" + id + ", borrowId=" + borrowId + ", amount=" + amount + ", paid=" + paid + '}';
    }
}
