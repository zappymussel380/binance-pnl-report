package no.strazdins.process;

import java.util.HashMap;
import java.util.Map;
import no.strazdins.data.Decimal;

/**
 * Auto-invest subscription, with configuration specifying the investment amount and money
 * distribution among the coins.
 */
public class AutoInvestSubscription {
  private final Decimal investmentAmount;
  private final Map<String, Decimal> assetProportions = new HashMap<>();

  /**
   * Create auto-invest subscription.
   *
   * @param investmentAmount The investment amount, in USD
   * @throws IllegalArgumentException When investment amount is null or non-positive
   */
  public AutoInvestSubscription(Decimal investmentAmount) throws IllegalArgumentException {
    if (investmentAmount == null || !investmentAmount.isPositive()) {
      throw new IllegalArgumentException("Investment amount must be positive, "
          + investmentAmount + " provided");
    }
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

  private boolean allProportionsSumUpToOne() {
    Decimal proportionSum = Decimal.ZERO;
    for (Decimal proportion : assetProportions.values()) {
      proportionSum = proportionSum.add(proportion);
    }
    return proportionSum.equals(Decimal.ONE);
  }
}
