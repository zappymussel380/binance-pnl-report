package no.strazdins.transaction;

import java.util.Objects;
import no.strazdins.data.AccountType;
import no.strazdins.data.Decimal;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.WalletSnapshot;
import no.strazdins.tool.TimeConverter;

/**
 * A transaction of receiving interest on savings.
 */
public class SavingsInterestTransaction extends Transaction {
  protected final RawAccountChange interest;

  /**
   * Create a Savings interest transaction.
   *
   * @param t The base transaction
   */
  public SavingsInterestTransaction(Transaction t) {
    super(t);
    interest = getInterestOperation();
    if (interest == null) {
      throw new IllegalStateException("Savings interest without a required raw change");
    }
    checkAccountType();
    baseCurrency = interest.getAsset();
    baseCurrencyAmount = interest.getAmount();
    baseObtainPriceInUsdt = Decimal.ZERO;
  }

  /**
   * Check whether correct account type is used.
   *
   * @throws IllegalArgumentException When incorrect account type is used
   */
  protected void checkAccountType() throws IllegalArgumentException {
    if (interest == null || !interest.getAccount().equals(AccountType.EARN)) {
      throw new IllegalArgumentException("Interest must be added to earnings account");
    }
  }

  /**
   * Get the raw-change where the interest is received.
   *
   * @return The interest operation or null if none found
   */
  protected RawAccountChange getInterestOperation() {
    return getFirstChangeOfType(Operation.SIMPLE_EARN_FLEXIBLE_INTEREST);
  }

  @Override
  public String toString() {
    return "Interest " + baseCurrencyAmount + " " + baseCurrency
        + " @ " + TimeConverter.utcTimeToString(utcTime);
  }

  @Override
  public WalletSnapshot process(WalletSnapshot walletSnapshot, ExtraInfoEntry extraInfo) {
    WalletSnapshot newSnapshot = walletSnapshot.prepareForTransaction(this);
    // The earned interest is "acquired for free" hence the average purchase price for it is zero
    newSnapshot.addAsset(baseCurrency, baseCurrencyAmount, Decimal.ZERO);
    return newSnapshot;
  }

  @Override
  public String getType() {
    return "Interest";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    SavingsInterestTransaction that = (SavingsInterestTransaction) o;
    return Objects.equals(interest, that.interest);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), interest);
  }

}
