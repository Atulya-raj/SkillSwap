package com.skillswap.model;

import com.skillswap.model.enums.LedgerType;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable audit ledger entry tracking user credit movements.
 */
public class CreditEntry {

    private long id;
    private long userId;
    private Long sessionRequestId;
    private LedgerType entryType;
    private int amount;
    private int balanceAfter;
    private String note;
    private LocalDateTime createdAt;

    /**
     * Default constructor.
     */
    public CreditEntry() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Long getSessionRequestId() {
        return sessionRequestId;
    }

    public void setSessionRequestId(Long sessionRequestId) {
        this.sessionRequestId = sessionRequestId;
    }

    public LedgerType getEntryType() {
        return entryType;
    }

    public void setEntryType(LedgerType entryType) {
        this.entryType = entryType;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(int balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreditEntry that = (CreditEntry) o;
        return id != 0 && id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CreditEntry{id=" + id + ", userId=" + userId + ", type=" + entryType + ", amount=" + amount + "}";
    }
}
