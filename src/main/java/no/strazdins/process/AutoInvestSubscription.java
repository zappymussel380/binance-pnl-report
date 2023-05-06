package no.strazdins.process;

import no.strazdins.data.Decimal;

/**
 * Auto-invest subscription, with configuration specifying the investment amount and money
 * distribution among the coins.
 */
public class AutoInvestSubscription {
  private final Decimal investmentAmount;

  /**
   * Create auto-invest subscription.
   *
   * @param investmentAmount The investment amount, in USD
   */
  public AutoInvestSubscription(Decimal investmentAmount) {
    this.investmentAmount = investmentAmount;
  }
}
