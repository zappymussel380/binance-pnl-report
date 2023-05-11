package no.strazdins.transaction;

import java.util.Objects;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.WalletSnapshot;

/**
 * Transaction for depositing money in the savings (Earn) account.
 */
public class SavingsSubscriptionTransaction extends Transaction {
  RawAccountChange deposit;

  /**
   * Create a "Savings deposit" transaction.
   *
   * @param transaction The base transaction
   */
  public SavingsSubscriptionTransaction(Transaction transaction) {
    super(transaction);
    deposit = getFirstChangeOfType(Operation.EARN_SUBSCRIPTION);
    if (deposit == null) {
      deposit = getFirstChangeOfType(Operation.SAVINGS_DISTRIBUTION);
    }
    if (deposit == null) {
      throw new IllegalStateException("Savings subscription without a required raw change");
    }
    baseCurrencyAmount = deposit.getAmount();
    baseCurrency = deposit.getAsset();
  }

  @Override
  public String toString() {
    return "Save-subscribe " + baseCurrencyAmount.getNiceString() + " " + baseCurrency;
  }

  @Override
  public WalletSnapshot process(WalletSnapshot walletSnapshot, ExtraInfoEntry extraInfo) {
    baseObtainPriceInUsdt = walletSnapshot.getWallet().getAvgObtainPrice(baseCurrency);
    return walletSnapshot.prepareForTransaction(this);
  }

  @Override
  public String getType() {
    return "Deposit to savings account";
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
    SavingsSubscriptionTransaction that = (SavingsSubscriptionTransaction) o;
    return Objects.equals(deposit, that.deposit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), deposit);
  }
}
