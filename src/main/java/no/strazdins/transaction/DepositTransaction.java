package no.strazdins.transaction;

import java.util.Objects;
import no.strazdins.data.Decimal;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.ExtraInfoType;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.WalletSnapshot;
import no.strazdins.tool.TimeConverter;

/**
 * A transaction of depositing a crypto to the Binance account.
 */
public class DepositTransaction extends Transaction {
  RawAccountChange deposit;

  /**
   * Create a deposit transaction.
   *
   * @param t the base transaction data to use
   */
  public DepositTransaction(Transaction t) {
    super(t);
    deposit = getFirstChangeOfType(Operation.DEPOSIT);
    if (deposit == null) {
      deposit = getFirstChangeOfType(Operation.FIAT_DEPOSIT);
    }
    if (deposit == null) {
      throw new IllegalStateException("Can't create a deposit transaction without a deposit op!");
    }
  }

  @Override
  public String toString() {
    return "Deposit " + deposit.getAmount() + " " + deposit.getAsset()
        + " @ " + TimeConverter.utcTimeToString(utcTime);
  }

  @Override
  public ExtraInfoEntry getNecessaryExtraInfo() {
    String date = TimeConverter.utcTimeToDateString(utcTime);
    String hint = "<" + deposit.getAsset() + " price in USD on " + date + ">";
    return new ExtraInfoEntry(utcTime, ExtraInfoType.ASSET_PRICE, deposit.getAsset(), hint);
  }

  @Override
  public WalletSnapshot process(WalletSnapshot walletSnapshot, ExtraInfoEntry extraInfo) {
    baseObtainPriceInUsdt = new Decimal(extraInfo.value());
    baseCurrency = deposit.getAsset();
    baseCurrencyAmount = deposit.getAmount();
    WalletSnapshot newSnapshot = walletSnapshot.prepareForTransaction(this);
    newSnapshot.addAsset(baseCurrency, baseCurrencyAmount, baseObtainPriceInUsdt);
    return newSnapshot;
  }

  @Override
  public String getType() {
    return "Deposit";
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
    DepositTransaction that = (DepositTransaction) o;
    return Objects.equals(deposit, that.deposit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), deposit);
  }
}
