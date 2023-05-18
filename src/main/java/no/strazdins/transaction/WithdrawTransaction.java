package no.strazdins.transaction;

import no.strazdins.data.Decimal;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.Operation;
import no.strazdins.data.WalletSnapshot;

/**
 * Withdrawal transaction.
 */
public class WithdrawTransaction extends ExternalTransferTransaction {
  /**
   * Create a Withdrawal transaction.
   *
   * @param t The raw transaction to use as the starting point
   */
  public WithdrawTransaction(Transaction t) {
    super(t);
    change = getFirstChangeOfType(Operation.WITHDRAW);
    if (change == null) {
      throw new IllegalStateException("Can't create a withdraw transaction without a withdraw op!");
    }
  }

  @Override
  public String getType() {
    return "Withdraw";
  }

  @Override
  public WalletSnapshot process(WalletSnapshot walletSnapshot, ExtraInfoEntry extraInfo) {
    baseCurrency = change.getAsset();
    baseCurrencyAmount = change.getAmount();
    Decimal assetAmount = change.getAmount().negate();
    Decimal realizationPrice = findExternalPrice(extraInfo);
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
