package no.strazdins.transaction;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import no.strazdins.data.Decimal;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.Wallet;
import no.strazdins.data.WalletSnapshot;

/**
 * Dust collection: exchange small amounts of one or several coins into BNB. Can contain multiple
 * raw account changes: positive BNB changes and negative changes of other assets.
 */
public class DustCollectionTransaction extends Transaction {
  private final Map<String, Decimal> dustAssets = new HashMap<>();
  private Decimal obtainedBnbAmount = Decimal.ZERO;

  /**
   * Create a dust collection transaction - convert small assets to BNB.
   *
   * @param t Base transaction data
   */
  public DustCollectionTransaction(Transaction t) {
    super(t);
    mergeAssetAmounts();
    baseCurrency = "BNB";
    baseCurrencyAmount = getDustChangeAmount("BNB");
    quoteCurrency = mergeBaseCurrencySymbols();
    quoteAmount = getDustChangeAmount(getQuoteAsset());
  }

  private String getQuoteAsset() {
    return dustAssets.size() == 1 ? getFirstDustAssetName() : null;
  }

  /**
   * Get the first asset name from the dustAssets map.
   *
   * @return The first asset name from the dustAssets map.
   */
  private String getFirstDustAssetName() {
    return dustAssets.keySet().iterator().next();
  }

  /**
   * Get the number of base assets involved in the dust collection - the small assets which were
   * converted to BNB.
   *
   * @return The number of small assets which were converted to BNB.
   */
  public long getDustAssetCount() {
    return dustAssets.size();
  }

  /**
   * Get all strings from assetChanges keys, except "BNB", sorted alphabetically, merged with + as
   * the glue.
   *
   * @return Merged asset strings
   */
  private String mergeBaseCurrencySymbols() {
    return dustAssets.keySet().stream()
        .sorted()
        .collect(Collectors.joining("+"));
  }

  /**
   * Look at all involved dust assets, group all sold dust assets as total amount per asset.
   * Also calculate the total gained BNB amount.
   */
  private void mergeAssetAmounts() {
    for (RawAccountChange dust : getChangesOfType(Operation.SMALL_ASSETS_EXCHANGE_BNB)) {
      String asset = dust.getAsset();
      Decimal amount = dust.getAmount();
      if (asset.equals("BNB")) {
        obtainedBnbAmount = obtainedBnbAmount.add(amount);
      } else {
        addDustAsset(asset, amount);
      }
    }
  }

  private void addDustAsset(String asset, Decimal amount) {
    if (dustAssets.containsKey(asset)) {
      dustAssets.put(asset, dustAssets.get(asset).add(amount));
    } else {
      dustAssets.put(asset, amount);
    }
  }

  @Override
  public String toString() {
    return "Dust collect " + obtainedBnbAmount.getNiceString() + " BNB, " + getAssetAmounts();
  }


  private String getAssetAmounts() {
    List<String> orderedAssets = dustAssets.keySet().stream().sorted().toList();
    List<String> values = new LinkedList<>();
    for (String asset : orderedAssets) {
      String amount = dustAssets.get(asset).getNiceString();
      values.add(amount + " " + asset);
    }
    return String.join(", ", values);
  }

  @Override
  public WalletSnapshot process(WalletSnapshot walletSnapshot, ExtraInfoEntry extraInfo) {
    WalletSnapshot newSnapshot = walletSnapshot.prepareForTransaction(this);
    Wallet wallet = newSnapshot.getWallet();
    Decimal totalDustValue = Decimal.ZERO;
    for (Map.Entry<String, Decimal> dust : dustAssets.entrySet()) {
      String asset = dust.getKey();
      Decimal amount = dust.getValue().negate();
      Decimal usdUsedToObtainDust = wallet.getAvgObtainPrice(asset).multiply(amount);
      totalDustValue = totalDustValue.add(usdUsedToObtainDust);
      newSnapshot.decreaseAsset(asset, amount);
    }
    Decimal bnbObtainPrice = totalDustValue.divide(obtainedBnbAmount);
    newSnapshot.addAsset("BNB", obtainedBnbAmount, bnbObtainPrice);
    return newSnapshot;
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
    Decimal amount;
    if ("BNB".equals(asset)) {
      amount = obtainedBnbAmount;
    } else {
      amount = dustAssets.getOrDefault(asset, Decimal.ZERO);
    }
    return amount;
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
    return Objects.equals(dustAssets, that.dustAssets);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), dustAssets);
  }
}
