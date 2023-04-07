package no.strazdins.transaction;

import no.strazdins.data.Decimal;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.ExtraInfoType;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.WalletSnapshot;
import no.strazdins.tool.Converter;

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
      throw new IllegalStateException("Can't create a deposit transaction without a deposit op!");
    }
  }

  @Override
  public String toString() {
    return "Deposit " + deposit.getAmount() + " " + deposit.getAsset()
        + " @ " + Converter.utcTimeToString(utcTime);
  }

  @Override
  public ExtraInfoEntry getNecessaryExtraInfo() {
    String date = Converter.utcTimeToDateString(utcTime);
    String hint = "<" + deposit.getAsset() + " price in USD on " + date + ">";
    return new ExtraInfoEntry(utcTime, ExtraInfoType.ASSET_PRICE, hint);
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
}
