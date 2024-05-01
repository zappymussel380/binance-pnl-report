package no.strazdins.transaction;

import static no.strazdins.data.Operation.CONVERT;

import java.util.List;
import no.strazdins.data.RawAccountChange;

/**
 * Currency exchange transaction.
 */
public class CurrencyExchangeTransaction extends Transaction {
  private RawAccountChange buy;
  private RawAccountChange sell;

  public CurrencyExchangeTransaction(Transaction t) {
    super(t);
    initCurrencies();
  }

  private void initCurrencies() {
    List<RawAccountChange> changes = getChangesOfType(CONVERT);
    if (changes.size() != 2) {
      throw new IllegalStateException("Convert transaction must contain exactly two changes");
    }
    RawAccountChange firstChange = changes.get(0);
    RawAccountChange secondChange = changes.get(1);
    if (firstChange.getAmount().isPositive()) {
      buy = firstChange;
      sell = secondChange;
    } else {
      buy = secondChange;
      sell = firstChange;
    }
    if (!sell.getAmount().isNegative()) {
      throw new IllegalStateException("Second currency amount must be negative in exchange");
    }
  }

  @Override
  public String getType() {
    return "Currency exchange";
  }
}
