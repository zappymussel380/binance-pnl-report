package no.strazdins.transaction;

import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.WalletSnapshot;
import no.strazdins.tool.Converter;

/**
 * A Sell transaction.
 */
public class SellTransaction extends Transaction {
  private RawAccountChange base;
  private RawAccountChange quote;

  public SellTransaction(Transaction transaction) {
    super(transaction);
    initBaseAndQuote();
  }

  private void initBaseAndQuote() {
    if (looksLikeReversedBuy()) {
      base = getFirstChangeOfType(Operation.TRANSACTION_RELATED);
      quote = getFirstChangeOfType(Operation.BUY);
    } else {
      base = getFirstChangeOfType(Operation.BUY);
      quote = getFirstChangeOfType(Operation.TRANSACTION_RELATED);
    }
  }

  @Override
  public String toString() {
    return "Sell " + base.getAmount() + " " + base.getAsset() + "/" + quote.getAsset()
        + " @ " + Converter.utcTimeToString(utcTime);
  }

  @Override
  public WalletSnapshot process(WalletSnapshot walletSnapshot, ExtraInfoEntry extraInfo) {
    // TODO
    throw new UnsupportedOperationException();
  }

  @Override
  public String getType() {
    return "Sell";
  }
}
