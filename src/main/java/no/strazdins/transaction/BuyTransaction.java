package no.strazdins.transaction;

import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.WalletSnapshot;
import no.strazdins.tool.Converter;

/**
 * A Buy-transaction.
 */
public class BuyTransaction extends Transaction {
  private RawAccountChange base;
  private RawAccountChange quote;
  private RawAccountChange feeOp;

  public BuyTransaction(Transaction transaction) {
    super(transaction);
    initBaseAndQuote();
  }

  private void initBaseAndQuote() {
    base = getFirstChangeOfType(Operation.BUY);
    quote = getFirstChangeOfType(Operation.TRANSACTION_RELATED);
    feeOp = getFirstChangeOfType(Operation.FEE);
    if (base == null || quote == null || feeOp == null) {
      throw new IllegalStateException("Can't create a buy when some ops are missing!");
    }
  }

  @Override
  public String toString() {
    return "Buy " + base.getAmount() + " " + base.getAsset() + "/" + quote.getAsset()
        + " @ " + Converter.utcTimeToString(utcTime);
  }

  @Override
  public String getType() {
    return "Buy";
  }

  @Override
  public WalletSnapshot process(WalletSnapshot walletSnapshot, ExtraInfoEntry extraInfo) {
    // TODO
    // !!! throw new UnsupportedOperationException();
    return walletSnapshot;
  }
}
