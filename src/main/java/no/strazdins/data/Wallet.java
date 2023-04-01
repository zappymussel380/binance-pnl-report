package no.strazdins.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A wallet that holds a list of assets in it, keeps track of the amount and average purchase
 * price for each asset.
 */
public class Wallet {
  private final Map<String, AssetBalance> assets = new HashMap<>();

  /**
   * Add an asset to the wallet.
   *
   * @param asset       The asset to add
   * @param amount      The amount of the asset
   * @param obtainPrice The price at which the asset was obtained (in Home Currency)
   */
  public void addAsset(String asset, Decimal amount, Decimal obtainPrice) {
    AssetBalance assetBalance = assets.get(asset);
    if (assetBalance != null) {
      assetBalance.add(amount, obtainPrice);
    } else {
      assets.put(asset, new AssetBalance(amount, obtainPrice));
    }
  }

  /**
   * Get the number of assets held in the wallet.
   *
   * @return The number of assets in the wallet
   */
  public int getAssetCount() {
    return assets.size();
  }

  /**
   * Create a clone of this wallet - create duplicates of all the data, a deep copy.
   *
   * @return A clone of this object
   */
  public Wallet clone() {
    Wallet w = new Wallet();
    for (Map.Entry<String, AssetBalance> entry : assets.entrySet()) {
      w.assets.put(entry.getKey(), entry.getValue().clone());
    }
    return w;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Wallet wallet = (Wallet) o;
    return assets.equals(wallet.assets);
  }

  @Override
  public int hashCode() {
    return Objects.hash(assets);
  }

  /**
   * Get the amount of asset stored in the wallet.
   *
   * @param asset The asset to look for
   * @return The amount of asset or Decimal.ZERO if it is not found
   */
  public Decimal getAssetAmount(String asset) {
    AssetBalance b = assets.get(asset);
    return b != null ? b.getAmount() : Decimal.ZERO;
  }

  /**
   * Get average obtain price for a given asset.
   *
   * @param asset The asset to look for
   * @return The average obtain price of asset or Decimal.ZERO if it is not found
   */
  public Decimal getAvgObtainPrice(String asset) {
    AssetBalance b = assets.get(asset);
    return b != null ? b.getObtainPrice() : Decimal.ZERO;
  }
}
