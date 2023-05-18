package no.strazdins.transaction;

import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.Operation;
import no.strazdins.data.WalletSnapshot;
import no.strazdins.tool.TimeConverter;

/**
 * A transaction of depositing a crypto to the Binance account.
 */
public class DepositTransaction extends ExternalTransferTransaction {

  /**
   * Create a deposit transaction.
   *
   * @param t the base transaction data to use
   */
  public DepositTransaction(Transaction t) {
    super(t);
    change = getFirstChangeOfType(Operation.DEPOSIT);
    if (change == null) {
      change = getFirstChangeOfType(Operation.FIAT_DEPOSIT);
    }
    if (change == null) {
      throw new IllegalStateException("Can't create a deposit transaction without a deposit op!");
    }
  }

  @Override
  public String toString() {
    return "Deposit " + change.getAmount() + " " + change.getAsset()
        + " @ " + TimeConverter.utcTimeToString(utcTime);
  }

  @Override
  public WalletSnapshot process(WalletSnapshot walletSnapshot, ExtraInfoEntry extraInfo) {
    baseObtainPriceInUsdt = findExternalPrice(extraInfo);
    baseCurrency = change.getAsset();
    baseCurrencyAmount = change.getAmount();
    WalletSnapshot newSnapshot = walletSnapshot.prepareForTransaction(this);
    newSnapshot.addAsset(baseCurrency, baseCurrencyAmount, baseObtainPriceInUsdt);
    return newSnapshot;
  }

  @Override
  public String getType() {
    return "Deposit";
  }
}
