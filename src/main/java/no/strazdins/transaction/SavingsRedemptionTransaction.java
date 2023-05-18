package no.strazdins.transaction;

import java.util.Objects;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.WalletSnapshot;

/**
 * Transaction for withdrawing money from a savings account.
 */
public class SavingsRedemptionTransaction extends Transaction {
  /**
   * Create a transaction for withdrawal from Savings account.
   *
   * @param t The base transaction data
   */
  public SavingsRedemptionTransaction(Transaction t) {
    super(t);
    withdraw = getFirstChangeOfType(Operation.EARN_REDEMPTION);
    if (withdraw == null) {
      throw new IllegalStateException("Savings withdrawal without a required raw change");
    }
    baseCurrencyAmount = withdraw.getAmount();
    baseCurrency = withdraw.getAsset();
  }

  final RawAccountChange withdraw;

  @Override
  public String toString() {
    return "Save-withdraw " + baseCurrencyAmount.getNiceString() + " " + baseCurrency;
  }

  @Override
  public WalletSnapshot process(WalletSnapshot walletSnapshot, ExtraInfoEntry extraInfo) {
    baseObtainPriceInUsdt = walletSnapshot.getWallet().getAvgObtainPrice(baseCurrency);
    return walletSnapshot.prepareForTransaction(this);
  }

  @Override
  public String getType() {
    return "Withdraw from savings account";
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
    SavingsRedemptionTransaction that = (SavingsRedemptionTransaction) o;
    return Objects.equals(withdraw, that.withdraw);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), withdraw);
  }
}
