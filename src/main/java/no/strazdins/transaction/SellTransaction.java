package no.strazdins.transaction;

import no.strazdins.data.Decimal;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.Wallet;
import no.strazdins.data.WalletSnapshot;
import no.strazdins.tool.Converter;

/**
 * A Sell transaction. Note: only transactions where USDT was obtained are
 * considered sell-transactions. For example, selling LTC in the market LTC/BTC is considered
 * a buy transaction. The reason: for each sell transaction we update the PNL. In the LTC/BTC
 * market we can't find any profit. We get profit or loss only when we get sell a currency for USDT.
 */
public class SellTransaction extends Transaction {
  private RawAccountChange base;
  private RawAccountChange quote;
  private RawAccountChange feeOp;

  public SellTransaction(Transaction transaction) {
    super(transaction);
    initBaseAndQuote();
  }

  private void initBaseAndQuote() {
    base = getFirstChangeOfType(Operation.TRANSACTION_RELATED);
    quote = getFirstChangeOfType(Operation.BUY);
    feeOp = getFirstChangeOfType(Operation.FEE);
    if (base == null || quote == null || feeOp == null) {
      throw new IllegalStateException("Can't create a sell when some ops are missing!");
    }
    if (!quote.getAsset().equals(QUOTE_CURR)) {
      throw new IllegalStateException("Sell transactions must have "
          + QUOTE_CURR + " quote currency!");
    }
    baseCurrency = base.getAsset();
    baseCurrencyAmount = base.getAmount();
    quoteAmount = quote.getAmount();
    quoteCurrency = "USDT";
    fee = feeOp.getAmount();
    feeCurrency = feeOp.getAsset();
  }

  @Override
  public String toString() {
    return "Sell " + base.getAmount() + " " + base.getAsset() + "/" + quote.getAsset()
        + " @ " + Converter.utcTimeToString(utcTime);
  }

  @Override
  public WalletSnapshot process(WalletSnapshot walletSnapshot, ExtraInfoEntry extraInfo) {
    WalletSnapshot newSnapshot = walletSnapshot.prepareForTransaction(this);
    Wallet w = newSnapshot.getWallet();
    calculateFeeInUsdt(w);
    Decimal receivedUsdt = quote.getAmount().add(feeInUsdt); // Fee is negative
    Decimal investedUsdt = base.getAmount().negate().multiply(w.getAvgObtainPrice(base.getAsset()));
    pnl = receivedUsdt.subtract(investedUsdt);
    newSnapshot.addPnl(pnl);

    newSnapshot.addAsset(QUOTE_CURR, quote.getAmount(), Decimal.ONE);
    newSnapshot.decreaseAsset(base.getAsset(), base.getAmount().negate());
    newSnapshot.decreaseAsset(feeOp.getAsset(), feeOp.getAmount().negate());

    baseObtainPriceInUsdt = w.getAvgObtainPrice(base.getAsset());

    return newSnapshot;
  }

  @Override
  public String getType() {
    return "Sell";
  }

}
