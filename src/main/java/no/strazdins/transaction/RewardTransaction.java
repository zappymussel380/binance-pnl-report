package no.strazdins.transaction;

import no.strazdins.data.AccountType;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.tool.TimeConverter;

/**
 * A transaction where an asset is earned "for free" as a some kind of reward.
 */
public class RewardTransaction extends SavingsInterestTransaction {
  /**
   * Create a Reward transaction.
   *
   * @param t The base transaction
   */
  public RewardTransaction(Transaction t) {
    super(t);
  }

  @Override
  protected RawAccountChange getInterestOperation() {
    RawAccountChange change = getFirstChangeOfType(Operation.CASHBACK_VOUCHER);
    if (change == null) {
      change = getFirstChangeOfType(Operation.BNB_VAULT_REWARDS);
    }
    return change;
  }

  @Override
  public String toString() {
    return "Reward " + baseCurrencyAmount + " " + baseCurrency
        + " @ " + TimeConverter.utcTimeToString(utcTime);
  }

  @Override
  public String getType() {
    return "Reward";
  }

  /**
   * Check whether correct account type is used.
   *
   * @throws IllegalArgumentException When incorrect account type is used
   */
  @Override
  protected void checkAccountType() throws IllegalArgumentException {
    if (interest == null || !interest.getAccount().equals(AccountType.SPOT)) {
      throw new IllegalArgumentException("Cashback must be added to spot account");
    }
  }

}
