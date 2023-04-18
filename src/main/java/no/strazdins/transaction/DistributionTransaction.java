package no.strazdins.transaction;

import no.strazdins.data.Decimal;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.WalletSnapshot;

/**
 * A transaction for receiving "free assets" as a result of a distribution (a reward for holding
 * another asset).
 */
public class DistributionTransaction extends Transaction {

  /**
   * Create an asset distribution transaction.
   *
   * @param transaction The base transaction
   */
  public DistributionTransaction(Transaction transaction) {
    super(transaction);
    RawAccountChange distribution = getFirstChangeOfType(Operation.DISTRIBUTION);
    if (distribution == null) {
      throw new IllegalStateException("Distribution transaction without the necessary raw change");
    }
    baseCurrency = distribution.getAsset();
    baseCurrencyAmount = distribution.getAmount();
  }

  @Override
  public WalletSnapshot process(WalletSnapshot walletSnapshot, ExtraInfoEntry extraInfo) {
    WalletSnapshot newSnapshot = walletSnapshot.prepareForTransaction(this);
    newSnapshot.addAsset(baseCurrency, baseCurrencyAmount, Decimal.ZERO);
    return newSnapshot;
  }

  @Override
  public String toString() {
    return "Distribution " + baseCurrencyAmount.getNiceString() + " " + baseCurrency;
  }

  @Override
  public String getType() {
    return "Distribution";
  }
}
