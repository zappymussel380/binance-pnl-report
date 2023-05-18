package no.strazdins.data;

import java.io.IOException;

/**
 * Possible account-change operations.
 */
public enum Operation {
  BUY, SELL, FEE, DEPOSIT, WITHDRAW, DISTRIBUTION, SAVINGS_DISTRIBUTION,
  BNB_VAULT_REWARDS, BUY_CRYPTO, CASHBACK_VOUCHER, COMMISSION_REBATE, FIAT_DEPOSIT,
  EARN_SUBSCRIPTION, EARN_REDEMPTION, EARN_INTEREST,
  SMALL_ASSETS_EXCHANGE_BNB, AUTO_INVEST;

  /**
   * Convert a capitalized string to a corresponding enum.
   *
   * @param s A capitalized string, as used in the CSV file
   * @return Corresponding Enum value
   * @throws IOException When an unexpected s value is received
   */
  public static Operation fromString(String s) throws IOException {
    return switch (s) {
      case "Buy", "Transaction Buy", "Transaction Revenue" -> BUY;
      case "Sell", "Transaction Sold", "Transaction Spend", "Transaction Related" -> SELL;
      case "Fee" -> FEE;
      case "Deposit" -> DEPOSIT;
      case "Withdraw" -> WITHDRAW;
      case "Distribution" -> DISTRIBUTION;
      case "Savings Distribution" -> SAVINGS_DISTRIBUTION;
      case "BNB Vault Rewards" -> BNB_VAULT_REWARDS;
      case "Buy Crypto" -> BUY_CRYPTO;
      case "Cashback Voucher" -> CASHBACK_VOUCHER;
      case "Commission Rebate" -> COMMISSION_REBATE;
      case "Fiat Deposit" -> FIAT_DEPOSIT;
      case "Simple Earn Flexible Subscription" -> EARN_SUBSCRIPTION;
      case "Simple Earn Flexible Redemption" -> EARN_REDEMPTION;
      case "Simple Earn Flexible Interest" -> EARN_INTEREST;
      case "Small Assets Exchange BNB" -> SMALL_ASSETS_EXCHANGE_BNB;
      case "Auto-Invest Transaction" -> AUTO_INVEST;
      default -> throw new IOException("Invalid operation string: " + s);
    };
  }
}
