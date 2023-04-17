package no.strazdins.transaction;

import no.strazdins.data.*;
import no.strazdins.tool.TimeConverter;
import java.util.Objects;

/**
 * A transaction of receiving interest on savings.
 */
public class SavingsInterestTransaction extends Transaction {
  private final RawAccountChange interest;
  public SavingsInterestTransaction(Transaction t) {
    super(t);
    interest = getFirstChangeOfType(Operation.SIMPLE_EARN_FLEXIBLE_INTEREST);
    if (interest == null) {
      throw new IllegalStateException("Savings interest without a required raw change");
    }
    baseCurrency = interest.getAsset();
    baseCurrencyAmount = interest.getAmount();
    baseObtainPriceInUsdt = Decimal.ZERO;
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
