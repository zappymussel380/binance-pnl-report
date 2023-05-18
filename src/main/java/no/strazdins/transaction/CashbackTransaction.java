package no.strazdins.transaction;

import no.strazdins.data.AccountType;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.tool.TimeConverter;

/**
 * A cash-back transaction - handled the same way as savings interest - we acquire and
 * asset "for free".
 */
public class CashbackTransaction extends SavingsInterestTransaction {
  /**
   * Create a Cashback transaction.
   *
   * @param t The base transaction
   */
  public CashbackTransaction(Transaction t) {
    super(t);
  }

  @Override
  protected RawAccountChange getInterestOperation() {
    return getFirstChangeOfType(Operation.CASHBACK_VOUCHER);
  }

  @Override
  public String toString() {
    return "Cashback " + baseCurrencyAmount + " " + baseCurrency
        + " @ " + TimeConverter.utcTimeToString(utcTime);
  }

  @Override
  public String getType() {
    return "Cashback";
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
