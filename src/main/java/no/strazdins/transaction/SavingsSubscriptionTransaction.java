package no.strazdins.transaction;

import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.WalletSnapshot;

/**
 * Transaction for depositing money in the savings (Earn) account.
 */
public class SavingsSubscriptionTransaction extends Transaction {

  public SavingsSubscriptionTransaction(Transaction transaction) {
    super(transaction);
  }

  @Override
  public String toString() {
    return "Save-subscribe";
  }

  @Override
  public WalletSnapshot process(WalletSnapshot walletSnapshot, ExtraInfoEntry extraInfo) {
    RawAccountChange deposit = getFirstChangeOfType(Operation.SIMPLE_EARN_FLEXIBLE_SUBSCRIPTION);
    if (deposit == null) {
      throw new IllegalStateException("Savings subscription without necessary raw change");
    }
    baseCurrencyAmount = deposit.getAmount();
    baseCurrency = deposit.getAsset();
    baseObtainPriceInUsdt = walletSnapshot.getWallet().getAvgObtainPrice(baseCurrency);
    return walletSnapshot.prepareForTransaction(this);
  }

  @Override
  public String getType() {
    return "Deposit to savings account";
  }
}
