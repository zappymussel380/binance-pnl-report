package no.strazdins.transaction;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import no.strazdins.data.AccountType;
import no.strazdins.data.Decimal;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.WalletSnapshot;

/**
 * A context for temporary storing data for Coin-to-coin tests, and running the tests.
 */
class CoinToCoinContext {
  private final WalletSnapshot startSnapshot;
  private String buyAsset;
  private List<String> buyAmounts = new ArrayList<>();
  private String sellAsset;
  private List<String> sellAmounts = new ArrayList<>();
  private String feeAsset;
  private List<String> feeAmounts = new ArrayList<>();

  /**
   * Create test for coin-to-coin transaction.
   *
   * @param startSnapshot The starting wallet snapshot, before the transaction
   */
  CoinToCoinContext(WalletSnapshot startSnapshot) {
    this.startSnapshot = startSnapshot;
  }

  /**
   * Register the buying asset and it's amounts.
   *
   * @param asset   The asset being bought in this transaction
   * @param amounts The amounts of individual raw changes
   * @return This transaction, for chained calls
   */
  public CoinToCoinContext buy(String asset, String... amounts) {
    buyAsset = asset;
    buyAmounts.addAll(Arrays.asList(amounts));
    return this;
  }

  /**
   * Register the selling asset and it's amounts.
   *
   * @param asset   The asset being sold in this transaction
   * @param amounts The amounts of individual raw changes
   * @return This transaction, for chained calls
   */
  public CoinToCoinContext sell(String asset, String... amounts) {
    sellAsset = asset;
    sellAmounts.addAll(Arrays.asList(amounts));
    return this;
  }

  /**
   * Register the fee asset and it's amounts.
   *
   * @param asset   The asset being used for fees in this transaction
   * @param amounts The amounts of individual raw changes
   * @return This transaction, for chained calls
   */
  public CoinToCoinContext fees(String asset, String... amounts) {
    feeAsset = asset;
    feeAmounts.addAll(Arrays.asList(amounts));
    return this;
  }

  public WalletSnapshot process() {
    long time = System.currentTimeMillis();
    Transaction t = new Transaction(time);
    for (String buyAmount : buyAmounts) {
      t.append(new RawAccountChange(time, AccountType.SPOT, Operation.BUY, buyAsset,
          new Decimal(buyAmount), ""));
    }
    for (String sellAmount : sellAmounts) {
      t.append(new RawAccountChange(time, AccountType.SPOT, Operation.SELL, sellAsset,
          new Decimal(sellAmount), ""));
    }
    for (String feeAmount : feeAmounts) {
      t.append(new RawAccountChange(time, AccountType.SPOT, Operation.FEE, feeAsset,
          new Decimal(feeAmount), ""));
    }
    Transaction coinToCoin = t.clarifyTransactionType();
    assertInstanceOf(CoinToCoinTransaction.class, coinToCoin);

    return coinToCoin.process(startSnapshot, null);
  }
}
