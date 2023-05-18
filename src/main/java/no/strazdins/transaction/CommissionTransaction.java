package no.strazdins.transaction;

import no.strazdins.data.AccountType;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;

/**
 * The user got a commission (for referral). The logic is the same as for Interest transaction.
 */
public class CommissionTransaction extends SavingsInterestTransaction {
  /**
   * Create a Savings interest transaction.
   *
   * @param t The base transaction
   */
  public CommissionTransaction(Transaction t) {
    super(t);
  }

  @Override
  protected RawAccountChange getInterestOperation() {
    return getFirstChangeOfType(Operation.COMMISSION_REBATE);
  }

  @Override
  protected void checkAccountType() throws IllegalArgumentException {
    if (interest == null || !interest.getAccount().equals(AccountType.SPOT)) {
      throw new IllegalArgumentException("Commission must be added to spot account");
    }
  }
}
