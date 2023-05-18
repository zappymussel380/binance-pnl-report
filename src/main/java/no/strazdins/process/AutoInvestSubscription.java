package no.strazdins.process;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import no.strazdins.data.Decimal;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.ExtraInfoType;

/**
 * Auto-invest subscription, with configuration specifying the investment amount and money
 * distribution among the coins.
 */
public class AutoInvestSubscription {
  private final Decimal investmentAmount;
  private final Map<String, Decimal> assetProportions = new HashMap<>();

  private final Set<String> acquiredAssets = new HashSet<>();

  private final long utcTime;

  /**
   * Create auto-invest subscription.
   *
   * @param utcTime          UTC timestamp of the subscriptions first investment transaction
   * @param investmentAmount The investment amount, in USD
   * @throws IllegalArgumentException When investment amount is null or non-positive
   */
  public AutoInvestSubscription(long utcTime, Decimal investmentAmount)
      throws IllegalArgumentException {
    if (investmentAmount == null || !investmentAmount.isPositive()) {
      throw new IllegalArgumentException("Investment amount must be positive, "
          + investmentAmount + " provided");
    }
    this.utcTime = utcTime;
    this.investmentAmount = investmentAmount;
  }

  /**
   * Register what proportion of the investment will be used to acquire the given asset.
   *
   * @param asset      The asset in question
   * @param proportion The proportion of invested money used to buy this asset in each buy
   * @throws IllegalArgumentException When trying to register proportion for an asset which has
   *                                  already a previous proportion registered
   */
  public void addAssetProportion(String asset, Decimal proportion) throws IllegalArgumentException {
    if (assetProportions.containsKey(asset)) {
      throw new IllegalArgumentException("Proportion for " + asset + " already registered");
    }
    assetProportions.put(asset, proportion);
  }

  /**
   * Check whether the subscription is valid - whether it has all necessary information.
   *
   * @return True if the subscription is valid (information is complete), false otherwise
   */
  public boolean isValid() {
    return investmentAmount != null && investmentAmount.isPositive()
        && allProportionsSumUpToOne();
  }

  /**
   * Get the investment amount for this subscription.
   *
   * @return Investment amount (for every step)
   */
  public Decimal getInvestmentAmount() {
    return investmentAmount;
  }

  /**
   * Get UTC timestamp of the subscriptions first investment transaction.
   *
   * @return The UTC timestamp of the subscription (first transaction)
   */
  public long getUtcTime() {
    return utcTime;
  }

  private boolean allProportionsSumUpToOne() {
    Decimal proportionSum = Decimal.ZERO;
    for (Decimal proportion : assetProportions.values()) {
      proportionSum = proportionSum.add(proportion);
    }
    return proportionSum.equals(Decimal.ONE);
  }

  /**
   * Get the ExtraInfo which is necessary for this transaction.
   *
   * @return The necessary extra info
   */
  public ExtraInfoEntry getNecessaryExtraInfo() {
    String[] assets = acquiredAssets.toArray(new String[assetProportions.size()]);
    String assetString = String.join("|", assets);
    return new ExtraInfoEntry(utcTime, ExtraInfoType.AUTO_INVEST_PROPORTIONS, assetString,
        "Proportions for assets in format proportion1|proportion2|...");
  }

  /**
   * Register that an asset is acquired in an auto-invest transaction belonging to
   * this subscription.
   *
   * @param asset The acquired asset, must be non-USD-like (not USD, USDT, BUSD, USDC)
   */
  public void registerAcquiredAsset(String asset) {
    acquiredAssets.add(asset);
  }

  /**
   * Try to configure this subscription with provided extra information.
   *
   * @param extraInfo The extra information
   * @return True if the subscription is now valid after configuration
   */
  public boolean tryConfigure(ExtraInfoEntry extraInfo) {
    if (extraInfo != null) {
      String[] assets = extraInfo.asset().split("\\|");
      String[] proportions = extraInfo.value().split("\\|");
      if (assets.length == 0 || assets.length != proportions.length) {
        throw new IllegalArgumentException("Incorrect extra info format: " + extraInfo);
      }
      for (int i = 0; i < assets.length; ++i) {
        try {
          addAssetProportion(assets[i], new Decimal(proportions[i]));
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Invalid proportion: " + proportions[i]);
        }
      }
    }
    return isValid();
  }

  /**
   * Get the amount of quote currency (USDT) used to obtain the provided asset, in each investment
   * step. This is calculated based on provided investment amount and proportions.
   *
   * @param asset The asset in question
   * @return The USDT amount used to acquire this asset in each investment step
   * @throws IllegalArgumentException If there is no configuration for the asset or when the
   *                                  subscription is invalid
   */
  public Decimal getInvestmentForAsset(String asset) throws IllegalArgumentException {
    if (!isValid() || !assetProportions.containsKey(asset)) {
      throw new IllegalArgumentException("Subscription can't determine investment amount for "
          + asset);
    }
    return investmentAmount.multiply(assetProportions.get(asset));
  }
}
