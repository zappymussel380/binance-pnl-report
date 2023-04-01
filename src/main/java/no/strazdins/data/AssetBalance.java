package no.strazdins.data;

import java.util.Objects;

/**
 * Holds tha amount and obtain-price for one specific asset in the wallet.
 */
public class AssetBalance {
  private Decimal amount;
  private Decimal obtainPrice;

  public AssetBalance(Decimal amount, Decimal obtainPrice) {
    this.amount = amount;
    this.obtainPrice = obtainPrice;
  }

  public AssetBalance(AssetBalance ab) {
    this.amount = ab.amount;
    this.obtainPrice = ab.obtainPrice;
  }

  /**
   * Add an asset to the balance.
   *
   * @param amount      The amount to add
   * @param obtainPrice The price at which the asset was obtained (in Home Currency)
   */
  public void add(Decimal amount, Decimal obtainPrice) {
    Decimal quoteSpentInTransaction = amount.multiply(obtainPrice);
    Decimal totalQuoteCurrencySpent = getQuoteSpent().add(quoteSpentInTransaction);
    this.amount = this.amount.add(amount);
    this.obtainPrice = totalQuoteCurrencySpent.divide(this.amount);
  }

  private Decimal getQuoteSpent() {
    return amount.multiply(obtainPrice);
  }

  /**
   * Get the amount of the asset held in the balance.
   *
   * @return The amount of the asset
   */
  public Decimal getAmount() {
    return amount;
  }

  /**
   * Get the average obtain-price of the asset.
   *
   * @return The average obtain price of the asset, in Home Currency.
   */
  public Decimal getObtainPrice() {
    return obtainPrice;
  }

  @Override
  public String toString() {
    return amount.getNiceString() + " @ " + obtainPrice.getNiceString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AssetBalance that = (AssetBalance) o;
    return amount.equals(that.amount) && obtainPrice.equals(that.obtainPrice);
  }

  @Override
  public int hashCode() {
    return Objects.hash(amount, obtainPrice);
  }

}
