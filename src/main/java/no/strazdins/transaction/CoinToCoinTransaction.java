package no.strazdins.transaction;

import no.strazdins.data.Decimal;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.WalletSnapshot;
import no.strazdins.tool.TimeConverter;

/**
 * Transaction for exchanging one coin to another. For example, buy/sell in the market LTC/BTC.
 */
public class CoinToCoinTransaction extends BuyTransaction {
  public CoinToCoinTransaction(Transaction t) {
    super(t);
  }

  @Override
  public String toString() {
    return "CC " + baseCurrencyAmount.getNiceString() + " " + base.getAsset() + " -> "
        + quoteAmount.getNiceString() + " " + quote.getAsset()
        + " @ " + TimeConverter.utcTimeToString(utcTime);
  }

  @Override
  public String getType() {
    return "Coin to coin";
  }

  @Override
  public WalletSnapshot process(WalletSnapshot walletSnapshot, ExtraInfoEntry extraInfo) {
    WalletSnapshot newSnapshot = walletSnapshot.prepareForTransaction(this);
    calculateFeeInUsdt(newSnapshot.getWallet());
    // Base - the bought coin, quote - the sold coin
    Decimal usdUsed = quoteAmount.negate().multiply(newSnapshot.getAvgQuoteObtainPrice());
    usdUsed = usdUsed.add(feeInUsdt.negate());
    Decimal avgBuyPriceInUsdt = usdUsed.divide(baseCurrencyAmount);
    newSnapshot.addAsset(baseCurrency, baseCurrencyAmount, avgBuyPriceInUsdt);

    this.baseObtainPriceInUsdt = newSnapshot.getAvgBaseObtainPrice();
    // Note: this is actually wrong naming, because in this case, the variable will contain
    // coin-to-coin price, not coin price in USDT!
    this.avgPriceInUsdt = baseCurrencyAmount.divide(quoteAmount.negate());

    try {
      newSnapshot.decreaseAsset(quoteCurrency, quoteAmount.negate());
      if (fee != null && fee.isNegative()) {
        newSnapshot.decreaseAsset(feeCurrency, fee.negate());
      }
    } catch (IllegalStateException e) {
      throw new IllegalStateException(this + ": " + e.getMessage());
    }

    return newSnapshot;
  }
}
