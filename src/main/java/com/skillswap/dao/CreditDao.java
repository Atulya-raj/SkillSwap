package com.skillswap.dao;

import com.skillswap.model.CreditEntry;
import com.skillswap.model.enums.LedgerType;

import java.util.List;

/**
 * Data access interface for credit ledger movements.
 */
public interface CreditDao {

    long insert(CreditEntry e);

    List<CreditEntry> findByUser(long userId, int limit, int offset);

    int countByUser(long userId);

    int sumByType(long userId, LedgerType t);

    int totalCreditsInCirculation();

    int sumHeldCredits(long userId);
}
