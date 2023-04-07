package no.strazdins.transaction;

import no.strazdins.data.*;
import no.strazdins.tool.Converter;

/**
 * Withdrawal transaction.
 */
public class WithdrawTransaction extends Transaction {
  RawAccountChange withdraw;

  public WithdrawTransaction(Transaction t) {
    super(t);
    withdraw = getFirstChangeOfType(Operation.WITHDRAW);
    if (withdraw == null) {
      throw new IllegalStateException("Can't create a withdraw transaction without a withdraw op!");
    }
  }

  @Override
  public ExtraInfoEntry getNecessaryExtraInfo() {
    String date = Converter.utcTimeToDateString(utcTime);
    String hint = "<" + withdraw.getAsset() + " price in USD on " + date + ">";
    return new ExtraInfoEntry(utcTime, ExtraInfoType.ASSET_PRICE, hint);
  }

  @Override
  public String getType() {
    return "Withdraw";
  }

  @Override
  public WalletSnapshot process(WalletSnapshot walletSnapshot, ExtraInfoEntry extraInfo) {
    baseCurrency = withdraw.getAsset();
    baseCurrencyAmount = withdraw.getAmount();
    Decimal assetAmount = withdraw.getAmount().negate();
    Decimal realizationPrice = new Decimal(extraInfo.value());
    avgPriceInUsdt = realizationPrice;
    WalletSnapshot newSnapshot = walletSnapshot.prepareForTransaction(this);
    baseObtainPriceInUsdt = newSnapshot.getAvgBaseObtainPrice();
    Decimal investedUsdt = newSnapshot.getAvgBaseObtainPrice().multiply(assetAmount);
    Decimal receivedUsdt = realizationPrice.multiply(assetAmount);
    pnl = receivedUsdt.subtract(investedUsdt);
    newSnapshot.addPnl(pnl);
    newSnapshot.decreaseAsset(baseCurrency, assetAmount);

    return newSnapshot;
  }
}
