package no.strazdins.data;

/**
 * Holds tha amount and obtain-price for one specific asset in the wallet.
 */
public class AssetBalance {
  private String amount;
  private String obtainPrice;

  public AssetBalance(String amount, String obtainPrice) {
    this.amount = amount;
    this.obtainPrice = obtainPrice;
  }

  /**
   * Add an asset to the balance.
   *
   * @param amount      The amount to add
   * @param obtainPrice The price at which the asset was obtained (in Home Currency)
   */
  public void add(String amount, String obtainPrice) {
    throw new UnsupportedOperationException();
    // TODO
    //    String quoteSpentInTransaction = amount * obtainPrice;
    //    String totalQuoteCurrencySpent = getQuoteSpent() + quoteSpentInTransaction;
    //    this.amount += amount;
    //    this.obtainPrice = totalQuoteCurrencySpent / this.amount;
  }
}
