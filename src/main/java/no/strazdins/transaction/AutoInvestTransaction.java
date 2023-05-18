package no.strazdins.transaction;

import java.util.Objects;
import no.strazdins.data.Decimal;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.WalletSnapshot;
import no.strazdins.process.AutoInvestSubscription;
import no.strazdins.tool.TimeConverter;

/**
 * Auto-invest transaction. It is a bit special, because its parts (operations) can have
 * different timestamps. First the money is spent (USD), then individual coins are bought at a
 * later time.
 */
public class AutoInvestTransaction extends Transaction {
  private AutoInvestSubscription subscription;

  public AutoInvestTransaction(Transaction transaction, AutoInvestSubscription subscription) {
    super(transaction);
    this.subscription = subscription;
  }

  /**
   * Check whether the raw change represents part of an auto-invest operation.
   *
   * @param change The change to check
   * @return True if it belongs to an auto-invest operation
   */
  public static boolean isAutoInvestOperation(RawAccountChange change) {
    return change.getOperation().equals(Operation.AUTO_INVEST);
  }

  /**
   * Get the asset bought in this transaction.
   *
   * @return The bought asset or null if no asset was bought (acquired) here
   */
  public String getBoughtAsset() {
    String boughtAsset = null;
    RawAccountChange invest = getRawAccountChange();
    if (invest != null && invest.getAmount().isPositive()) {
      boughtAsset = invest.getAsset();
    }
    return boughtAsset;
  }

  public void setSubscription(AutoInvestSubscription subscription) {
    this.subscription = subscription;
  }

  @Override
  public String getType() {
    return "Auto-invest";
  }

  @Override
  public String toString() {
    RawAccountChange op = getRawAccountChange();
    String opDetails = op != null ? (op.getAmount().getNiceString() + " " + op.getAsset()) : "";
    return "Auto-Invest " + opDetails + " @ "
        + TimeConverter.utcTimeToString(utcTime);
  }
  // TODO - if invest and acquire (USDT and coin) in the same second - throw error

  @Override
  public ExtraInfoEntry getNecessaryExtraInfo() {
    return utcTime == subscription.getUtcTime()
        ? subscription.getNecessaryExtraInfo()
        : null;
  }

  @Override
  public WalletSnapshot process(WalletSnapshot walletSnapshot, ExtraInfoEntry extraInfo) {
    if (!subscription.isValid() && !subscription.tryConfigure(extraInfo)) {
      throw new IllegalStateException("Missing necessary extra info for auto-invest: "
          + getNecessaryExtraInfo());
    }

    WalletSnapshot newSnapshot = walletSnapshot.prepareForTransaction(this);
    RawAccountChange change = getRawAccountChange();
    baseCurrency = change.getAsset();
    baseCurrencyAmount = change.getAmount();
    if (isInvestment()) {
      newSnapshot.decreaseAsset(baseCurrency, baseCurrencyAmount.negate());
    } else {
      Decimal investedUsdt = subscription.getInvestmentForAsset(baseCurrency);
      Decimal obtainPrice = investedUsdt.divide(baseCurrencyAmount);
      newSnapshot.addAsset(baseCurrency, baseCurrencyAmount, obtainPrice);
    }
    return newSnapshot;
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
    AutoInvestTransaction that = (AutoInvestTransaction) o;
    return Objects.equals(subscription, that.subscription);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), subscription);
  }

  /**
   * Check whether this is an investment transaction - USDT was invested (spent) here.
   *
   * @return True when this is an investment, false when this is an asset acquisition
   */
  public boolean isInvestment() {
    return getInvestedAsset() != null;
  }

  /**
   * Check whether this is an asset acquisition - an asset was bought.
   *
   * @return True when this is an asset acquisition, false when this is an investment
   */
  public boolean isAcquisition() {
    return getInvestedAsset() == null;
  }

  /**
   * Get the invested asset.
   *
   * @return The invested asset or null if this is not an investment transaction
   */
  public String getInvestedAsset() {
    RawAccountChange invest = getRawAccountChange();
    return invest.getAmount().isNegative() ? invest.getAsset() : null;
  }

  /**
   * Get the asset amount in this transaction.
   *
   * @return The amount or null if no asset is found
   */
  public Decimal getAmount() {
    RawAccountChange invest = getRawAccountChange();
    return invest != null ? invest.getAmount() : null;
  }

  private RawAccountChange getRawAccountChange() {
    return getFirstChangeOfType(Operation.AUTO_INVEST);
  }

  public AutoInvestSubscription getSubscription() {
    return subscription;
  }
}
