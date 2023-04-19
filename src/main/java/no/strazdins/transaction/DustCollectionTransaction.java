package no.strazdins.transaction;

/**
 * Dust collection: exchange small amounts of one or several coins into BNB. Can contain multiple
 * raw account changes: positive BNB changes and negative changes of other assets.
 */
public class DustCollectionTransaction extends Transaction {
  public DustCollectionTransaction(Transaction t) {
    super(t);
  }
}
