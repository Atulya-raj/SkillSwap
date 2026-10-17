package com.skillswap.service;

import com.skillswap.dao.CreditDao;
import com.skillswap.dao.UserDao;
import com.skillswap.dao.jdbc.JdbcCreditDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.db.TransactionManager;
import com.skillswap.exception.InsufficientCreditsException;
import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.exception.ValidationException;
import com.skillswap.model.CreditEntry;
import com.skillswap.model.User;
import com.skillswap.model.enums.LedgerType;

import java.util.List;
import java.util.Map;

/**
 * Service managing credit balance movements, escrow hold/release/pay, and ledger audit records.
 */
public class CreditService {

    private final UserDao userDao;
    private final CreditDao creditDao;

    /**
     * Default constructor using standard JDBC DAOs.
     */
    public CreditService() {
        this(new JdbcUserDao(), new JdbcCreditDao());
    }

    /**
     * Constructs CreditService with injected DAOs.
     *
     * @param userDao user DAO
     * @param creditDao credit ledger DAO
     */
    public CreditService(UserDao userDao, CreditDao creditDao) {
        this.userDao = userDao;
        this.creditDao = creditDao;
    }

    /**
     * Locks two users in ascending ID order to prevent deadlock.
     *
     * @param userId1 first user ID
     * @param userId2 second user ID
     */
    public void lockUsersInOrder(long userId1, long userId2) {
        long first = Math.min(userId1, userId2);
        long second = Math.max(userId1, userId2);
        userDao.lockForUpdate(first);
        if (first != second) {
            userDao.lockForUpdate(second);
        }
    }

    /**
     * Holds credits in escrow for a newly created session request.
     * Expects an active transaction.
     *
     * @param learnerId learner user ID
     * @param requestId session request ID
     * @param amount number of credits to hold
     * @throws InsufficientCreditsException if learner does not have enough credits
     * @throws ResourceNotFoundException if user is not found
     */
    public void hold(long learnerId, long requestId, int amount) throws InsufficientCreditsException, ResourceNotFoundException {
        User learner = userDao.lockForUpdate(learnerId);
        if (learner == null) {
            throw new ResourceNotFoundException("User not found: " + learnerId);
        }
        if (learner.getCredits() < amount) {
            throw new InsufficientCreditsException("Insufficient credits: balance is " + learner.getCredits() + ", required: " + amount);
        }

        int newBalance = learner.getCredits() - amount;
        userDao.setCredits(learnerId, newBalance);

        CreditEntry entry = new CreditEntry();
        entry.setUserId(learnerId);
        entry.setSessionRequestId(requestId);
        entry.setEntryType(LedgerType.HOLD);
        entry.setAmount(-amount);
        entry.setBalanceAfter(newBalance);
        entry.setNote("Held for session request #" + requestId);
        creditDao.insert(entry);
    }

    /**
     * Releases held escrow credits back to the learner.
     * Expects an active transaction.
     *
     * @param learnerId learner user ID
     * @param requestId session request ID
     * @param amount number of credits to refund
     * @param reason description of release cause
     * @throws ResourceNotFoundException if user is not found
     */
    public void release(long learnerId, long requestId, int amount, String reason) throws ResourceNotFoundException {
        if (amount <= 0) {
            return;
        }
        User learner = userDao.lockForUpdate(learnerId);
        if (learner == null) {
            throw new ResourceNotFoundException("User not found: " + learnerId);
        }

        int newBalance = learner.getCredits() + amount;
        userDao.setCredits(learnerId, newBalance);

        CreditEntry entry = new CreditEntry();
        entry.setUserId(learnerId);
        entry.setSessionRequestId(requestId);
        entry.setEntryType(LedgerType.RELEASE);
        entry.setAmount(amount);
        entry.setBalanceAfter(newBalance);
        entry.setNote(reason != null ? reason : "Released from session request #" + requestId);
        creditDao.insert(entry);
    }

    /**
     * Pays earned credits to the provider upon completion or compensation.
     * Expects an active transaction.
     *
     * @param providerId provider user ID
     * @param requestId session request ID
     * @param amount number of credits to pay
     * @param reason description of payment cause
     * @throws ResourceNotFoundException if user is not found
     */
    public void pay(long providerId, long requestId, int amount, String reason) throws ResourceNotFoundException {
        if (amount <= 0) {
            return;
        }
        User provider = userDao.lockForUpdate(providerId);
        if (provider == null) {
            throw new ResourceNotFoundException("User not found: " + providerId);
        }

        int newBalance = provider.getCredits() + amount;
        userDao.setCredits(providerId, newBalance);

        CreditEntry entry = new CreditEntry();
        entry.setUserId(providerId);
        entry.setSessionRequestId(requestId);
        entry.setEntryType(LedgerType.EARN);
        entry.setAmount(amount);
        entry.setBalanceAfter(newBalance);
        entry.setNote(reason != null ? reason : "Earned from session #" + requestId);
        creditDao.insert(entry);
    }

    /**
     * Manually adjusts a user's credit balance as an administrator.
     *
     * @param userId user ID
     * @param amount signed adjustment amount (+ or -)
     * @param reason mandatory explanation for audit trail
     * @throws ValidationException if reason is missing or amount is zero
     * @throws ResourceNotFoundException if user is not found
     * @throws InsufficientCreditsException if adjustment would result in negative balance
     */
    public void adminAdjust(long userId, int amount, String reason)
            throws ValidationException, ResourceNotFoundException, InsufficientCreditsException {
        if (reason == null || reason.trim().isEmpty()) {
            throw new ValidationException(Map.of("reason", "Adjustment reason is required"));
        }
        if (amount == 0) {
            throw new ValidationException(Map.of("amount", "Adjustment amount cannot be zero"));
        }

        try {
            TransactionManager.execute(() -> {
                User user = userDao.lockForUpdate(userId);
                if (user == null) {
                    throw new ResourceNotFoundException("User not found: " + userId);
                }
                int newBalance = user.getCredits() + amount;
                if (newBalance < 0) {
                    throw new InsufficientCreditsException("Adjustment would cause negative balance: " + newBalance);
                }

                userDao.setCredits(userId, newBalance);

                CreditEntry entry = new CreditEntry();
                entry.setUserId(userId);
                entry.setSessionRequestId(null);
                entry.setEntryType(LedgerType.ADMIN_ADJUST);
                entry.setAmount(amount);
                entry.setBalanceAfter(newBalance);
                entry.setNote(reason.trim());
                creditDao.insert(entry);
                return null;
            });
        } catch (ValidationException e) {
            throw e;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (InsufficientCreditsException e) {
            throw e;
        } catch (com.skillswap.exception.SkillSwapException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    /**
     * Retrieves current balance of user credits.
     *
     * @param userId user ID
     * @return current balance or 0 if not found
     */
    public int getBalance(long userId) {
        return userDao.findById(userId).map(User::getCredits).orElse(0);
    }

    /**
     * Retrieves total credits held in escrow for active pending or accepted outgoing sessions.
     *
     * @param userId learner user ID
     * @return sum of held credits
     */
    public int getHeldCredits(long userId) {
        return creditDao.sumHeldCredits(userId);
    }

    /**
     * Retrieves paginated credit ledger movements for a user.
     *
     * @param userId user ID
     * @param limit maximum rows
     * @param offset row offset
     * @return list of credit entries
     */
    public List<CreditEntry> getLedger(long userId, int limit, int offset) {
        return creditDao.findByUser(userId, limit, offset);
    }

    /**
     * Counts total credit entries for a user.
     *
     * @param userId user ID
     * @return count of entries
     */
    public int countLedger(long userId) {
        return creditDao.countByUser(userId);
    }

    /**
     * Computes total credits earned by user.
     *
     * @param userId user ID
     * @return total credits earned
     */
    public int getTotalEarned(long userId) {
        return creditDao.sumByType(userId, LedgerType.EARN);
    }
}
