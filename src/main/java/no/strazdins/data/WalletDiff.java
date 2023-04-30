package no.strazdins.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Difference between two wallet snapshots - the changes made by a single transaction.
 */
public class WalletDiff {
  private final Map<String, Decimal> assetDiffs = new HashMap<>();

  /**
   * Add the given amount of asset in the diff.
   *
   * @param asset  The asset to add
   * @param amount The amount to add. It can be negative.
   * @return This same object, for method chaining
   */
  public WalletDiff add(String asset, Decimal amount) {
    Decimal newAmount = assetDiffs.getOrDefault(asset, Decimal.ZERO).add(amount);
    if (newAmount.isZero()) {
      assetDiffs.remove(asset);
    } else {
      assetDiffs.put(asset, newAmount);
    }
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WalletDiff that = (WalletDiff) o;
    return Objects.equals(assetDiffs, that.assetDiffs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(assetDiffs);
  }

  /**
   * Get the amount of given asset stored in this diff.
   *
   * @param asset The asset to check
   * @return The amount of asset in the diff or Decimal.ZERO if not found
   */
  public Decimal getAmount(String asset) {
    return assetDiffs.getOrDefault(asset, Decimal.ZERO);
  }

  @Override
  public String toString() {
    return assetDiffs.toString();
  }

  /**
   * Add all assets from the given wallet.
   *
   * @param wallet The wallet to add the assets from
   * @return This object, for chained calls
   */
  public WalletDiff addAll(Wallet wallet) {
    for (String asset : wallet) {
      add(asset, wallet.getAssetAmount(asset));
    }
    return this;
  }

  /**
   * Subtract all the assets in the wallet from this difference.
   *
   * @param wallet The wallet holding the assets to subtract
   * @return This object, for chained calls
   */
  public WalletDiff removeAll(Wallet wallet) {
    for (String asset : wallet) {
      add(asset, wallet.getAssetAmount(asset).negate());
    }
    return this;
  }
}
