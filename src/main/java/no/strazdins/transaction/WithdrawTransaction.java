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
 * Withdrawal transaction.
 */
public class WithdrawTransaction extends Transaction {
  RawAccountChange withdraw;

  /**
   * Create a Withdrawal transaction.
   *
   * @param t The raw transaction to use as the starting point
   */
  public WithdrawTransaction(Transaction t) {
    super(t);
    withdraw = getFirstChangeOfType(Operation.WITHDRAW);
    if (withdraw == null) {
      throw new IllegalStateException("Can't create a withdraw transaction without a withdraw op!");
    }
  }

  @Override
  public ExtraInfoEntry getNecessaryExtraInfo() {
    String date = TimeConverter.utcTimeToDateString(utcTime);
    String hint = "<" + withdraw.getAsset() + " price in USD on " + date + ">";
    return new ExtraInfoEntry(utcTime, ExtraInfoType.ASSET_PRICE, withdraw.getAsset(), hint);
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
    Decimal realizationPrice = findRealizationPrice(extraInfo);
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

  /**
   * Find out realization value of the withdrawn asset - either from the extra info or use 1.0 if
   * it is a USD-like asset.
   *
   * @param extraInfo The user-provided extra info
   * @return The realization price of hte withdrawn asset
   * @throws IllegalArgumentException When price can neither be determined from ExtraInfo
   *                                  nor inferred
   */
  private Decimal findRealizationPrice(ExtraInfoEntry extraInfo) throws IllegalArgumentException {
    if (isUsdLike(withdraw.getAsset())) {
      return Decimal.ONE;
    } else if (extraInfo != null) {
      return new Decimal(extraInfo.value());
    } else {
      throw new IllegalArgumentException("Realization price must be specified for withdrawals");
    }
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
    WithdrawTransaction that = (WithdrawTransaction) o;
    return Objects.equals(withdraw, that.withdraw);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), withdraw);
  }
}
