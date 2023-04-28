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
    // PNL = 0
    // Bought coin - calculate transaction-obtain-price
    calculateFeeInUsdt(newSnapshot.getWallet());
    Decimal usdUsed = quoteAmount.negate().multiply(newSnapshot.getAvgQuoteObtainPrice());
    usdUsed = usdUsed.add(feeInUsdt.negate());
    Decimal avgBuyPrice = usdUsed.divide(baseCurrencyAmount);
    newSnapshot.addAsset(baseCurrency, baseCurrencyAmount, avgBuyPrice);

    newSnapshot.decreaseAsset(quoteCurrency, quoteAmount.negate());
    newSnapshot.decreaseAsset(feeCurrency, fee.negate());

    return newSnapshot;
  }
}
