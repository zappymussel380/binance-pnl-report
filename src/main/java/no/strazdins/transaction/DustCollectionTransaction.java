package no.strazdins.transaction;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.strazdins.data.Decimal;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.WalletSnapshot;

/**
 * Dust collection: exchange small amounts of one or several coins into BNB. Can contain multiple
 * raw account changes: positive BNB changes and negative changes of other assets.
 */
public class DustCollectionTransaction extends Transaction {
  private final Map<String, Decimal> assetChanges = new HashMap<>();

  /**
   * Create a dust collection transaction - convert small assets to BNB.
   *
   * @param t Base transaction data
   */
  public DustCollectionTransaction(Transaction t) {
    super(t);
    mergeAssetAmounts();
    quoteCurrency = "BNB";
    quoteAmount = getDustChangeAmount("BNB");
    baseCurrency = mergeBaseCurrencySymbols();
    baseCurrencyAmount = getDustChangeAmount(getBaseAsset());
  }

  private String getBaseAsset() {
    String asset = null;
    if (getBaseAssets().count() == 1) {
      asset = getBaseAssets().findFirst().orElse(null);
    }
    return asset;
  }

  /**
   * Get the number of base assets involved in the dust collection - the small assets which were
   * converted to BNB.
   *
   * @return The number of small assets which were converted to BNB.
   */
  public long getBaseAssetCount() {
    return getBaseAssets().count();
  }

  /**
   * Get all strings from assetChanges keys, except "BNB", sorted alphabetically, merged with + as
   * the glue.
   *
   * @return Merged asset strings
   */
  private String mergeBaseCurrencySymbols() {
    return getBaseAssets()
        .sorted()
        .collect(Collectors.joining("+"));
  }

  private Stream<String> getBaseAssets() {
    return assetChanges.keySet().stream()
        .filter(asset -> !asset.equals("BNB"));
  }

  private void mergeAssetAmounts() {
    for (RawAccountChange dust : getChangesOfType(Operation.SMALL_ASSETS_EXCHANGE_BNB)) {
      String asset = dust.getAsset();
      Decimal amount = dust.getAmount();
      if (assetChanges.containsKey(asset)) {
        assetChanges.put(asset, assetChanges.get(asset).add(amount));
      } else {
        assetChanges.put(asset, amount);
      }
    }
  }

  @Override
  public String toString() {
    return "Dust collect " + getAssetAmounts();
  }


  private String getAssetAmounts() {
    List<String> orderedAssets = assetChanges.keySet().stream().sorted().toList();
    List<String> values = new LinkedList<>();
    for (String asset : orderedAssets) {
      String amount = assetChanges.get(asset).getNiceString();
      values.add(amount + " " + asset);
    }
    return String.join(", ", values);
  }

  @Override
  public WalletSnapshot process(WalletSnapshot walletSnapshot, ExtraInfoEntry extraInfo) {
    return walletSnapshot.prepareForTransaction(this);
  }

  @Override
  public String getType() {
    return "Convert dust to BNB";
  }

  /**
   * Get the amount of change for the given asset in the dust collection transaction.
   *
   * @param asset The asset to check
   * @return The amount of the asset involved in the transaction or Decimal.ZERO if the asset is
   *         not found involved in the transaction.
   */
  public Decimal getDustChangeAmount(String asset) {
    return assetChanges.getOrDefault(asset, Decimal.ZERO);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    DustCollectionTransaction that = (DustCollectionTransaction) o;
    return Objects.equals(assetChanges, that.assetChanges);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), assetChanges);
  }
}
