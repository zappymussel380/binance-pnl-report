package no.strazdins.transaction;

/**
 * Auto-invest transaction. It is a bit special, because its parts (operations) can have
 * different timestamps. First the money is spent (USD), then individual coins are bought at a
 * later time.
 */
public class AutoInvestTransaction extends Transaction {
  public AutoInvestTransaction(Transaction t) {
    super(t);
  }
}
