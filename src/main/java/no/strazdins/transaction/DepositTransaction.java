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
  /**
   * Create a deposit transaction.
   *
   * @param t the base transaction data to use
   */
  public DepositTransaction(Transaction t) {
    super(t);
    if (getFirstChangeOfType(Operation.DEPOSIT) == null) {
      throw new IllegalStateException("Can't create a deposit without a deposit operation!");
    }
  }

  @Override
  public String toString() {
    RawAccountChange change = getChange();
    return "Deposit " + change.getAmount() + " " + change.getAsset()
        + " @ " + Converter.utcTimeToString(utcTime);
  }

  private RawAccountChange getChange() {
    return getFirstChangeOfType(Operation.DEPOSIT);
  }

  @Override
  public ExtraInfoEntry getNecessaryExtraInfo() {
    String date = Converter.utcTimeToDateString(utcTime);
    String hint = "<" + getChange().getAsset() + " price in home currency on " + date + ">";
    return new ExtraInfoEntry(utcTime, ExtraInfoType.ASSET_PRICE, hint);
  }

  @Override
  public WalletSnapshot process(WalletSnapshot walletSnapshot, ExtraInfoEntry extraInfo) {
    WalletSnapshot newSnapshot = walletSnapshot.prepareForTransaction(this);
    Decimal obtainPrice = new Decimal(extraInfo.value());
    RawAccountChange depositOperation = getFirstChangeOfType(Operation.DEPOSIT);
    Decimal depositAmount = depositOperation.getAmount();
    String asset = depositOperation.getAsset();
    newSnapshot.addAsset(asset, depositAmount, obtainPrice);
    return newSnapshot;
  }
}
