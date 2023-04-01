package no.strazdins.data;

import java.util.HashMap;
import java.util.Map;

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
  public void addAsset(String asset, String amount, String obtainPrice) {
    AssetBalance assetBalance = assets.get(asset);
    if (assetBalance != null) {
      assetBalance.add(amount, obtainPrice);
    } else {
      assets.put(asset, new AssetBalance(amount, obtainPrice));
    }
  }
}
