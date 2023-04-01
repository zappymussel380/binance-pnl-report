package no.strazdins.transaction;

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
  public DepositTransaction(Transaction t) {
    super(t);
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
    WalletSnapshot newSnapshot = new WalletSnapshot(walletSnapshot);
    String obtainPrice = extraInfo.value();
    RawAccountChange depositOperation = getFirstChangeOfType(Operation.DEPOSIT);
    String depositAmount = depositOperation.getAmount();
    String asset = depositOperation.getAsset();
    walletSnapshot.addAsset(asset, depositAmount, obtainPrice);
    // TODO - recalculate:
    // - wallet.asset.amount
    // - wallet.asset.averageObtainPrice
    return newSnapshot;
  }
}
