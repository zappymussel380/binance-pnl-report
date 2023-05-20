package no.strazdins.testtools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import no.strazdins.data.AccountType;
import no.strazdins.data.Decimal;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.ExtraInfoType;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.Wallet;
import no.strazdins.data.WalletDiff;
import no.strazdins.data.WalletSnapshot;
import no.strazdins.process.AutoInvestSubscription;
import no.strazdins.process.ReportLogic;
import no.strazdins.transaction.AutoInvestTransaction;
import no.strazdins.transaction.BuyTransaction;
import no.strazdins.transaction.DepositTransaction;
import no.strazdins.transaction.DistributionTransaction;
import no.strazdins.transaction.SellTransaction;
import no.strazdins.transaction.Transaction;
import no.strazdins.transaction.WithdrawTransaction;

/**
 * Different tools which make tests more readable.
 */
public class TestTools {
  private static long transactionTime = System.currentTimeMillis();

  /**
   * Expect that the provided transaction is an auto-invest transaction where an asset is
   * invested (spent).
   *
   * @param transaction The transaction to check
   * @param amount      The expected invested asset amount (should be negative)
   * @param asset       The expected invested asset
   */
  public static void expectInvestment(Transaction transaction, String amount, String asset) {
    assertInstanceOf(AutoInvestTransaction.class, transaction);
    AutoInvestTransaction investment = (AutoInvestTransaction) transaction;
    assertTrue(investment.isInvestment());
    assertFalse(investment.isAcquisition());
    assertNull(investment.getBoughtAsset());
    assertEquals(asset, investment.getInvestedAsset());
    assertEquals(new Decimal(amount), investment.getAmount());
  }

  /**
   * Expect that the provided transaction is an auto-invest transaction where an asset is
   * acquired (bought).
   *
   * @param transaction The transaction to check
   * @param amount      The expected acquired asset amount (should be positive)
   * @param asset       The expected acquired asset
   */
  public static void expectAcquisition(Transaction transaction, String amount, String asset) {
    assertInstanceOf(AutoInvestTransaction.class, transaction);
    AutoInvestTransaction acquisition = (AutoInvestTransaction) transaction;
    assertTrue(acquisition.isAcquisition());
    assertFalse(acquisition.isInvestment());
    assertNull(acquisition.getInvestedAsset());
    assertEquals(asset, acquisition.getBoughtAsset());
    assertEquals(new Decimal(amount), acquisition.getAmount());

  }

  /**
   * Expect all transactions in the list to be auto-invest transactions belonging to the
   * same subscription.
   *
   * @param transactions The transactions to check
   */
  public static void expectSameSubscription(List<Transaction> transactions) {
    assertFalse(transactions.isEmpty());
    AutoInvestSubscription subscription = null;
    for (Transaction t : transactions) {
      assertInstanceOf(AutoInvestTransaction.class, t);
      AutoInvestTransaction autoInvest = (AutoInvestTransaction) t;
      if (subscription != null) {
        assertEquals(subscription, autoInvest.getSubscription());
      } else {
        subscription = autoInvest.getSubscription();
      }
    }
  }

  /**
   * Expect transactions t1 and t2 to be auto-invest subscriptions and belong to two
   * different subscriptions.
   *
   * @param t1 First auto-invest transaction
   * @param t2 Second auto-invest transaction
   */
  public static void expectNotSameSubscription(Transaction t1, Transaction t2) {
    assertInstanceOf(AutoInvestTransaction.class, t1);
    assertInstanceOf(AutoInvestTransaction.class, t2);
    assertNotEquals(
        ((AutoInvestTransaction) t1).getSubscription(),
        ((AutoInvestTransaction) t2).getSubscription()
    );
  }


  /**
   * Create a list of auto-invest transactions.
   *
   * @param assets The asset changes in the transactions. Each change is specified as a
   *               string tuple (amount, asset). Fees and earnings subscriptions are not generated
   * @return List of auto-invest transactions
   */
  public static List<Transaction> createAutoInvestments(String... assets) {
    List<RawAccountChange> changes = new LinkedList<>();
    long time = System.currentTimeMillis();
    for (int i = 0; i < assets.length; i += 2) {
      Decimal amount = new Decimal(assets[i]);
      String asset = assets[i + 1];
      changes.add(new RawAccountChange(time, AccountType.SPOT, Operation.AUTO_INVEST,
          asset, amount, ""));
      time += 1000;
    }
    ReportLogic logic = new ReportLogic();
    List<Transaction> rawTransactions = logic.groupTransactionsByTimestamp(changes);
    return logic.clarifyTransactionTypes(rawTransactions);
  }

  /**
   * Create a list of raw changes (for a Spot account) from a string-specification.
   *
   * @param timestamp The UTC timestamp to use for all the changes
   * @param changes   The required changes, each of them specified as a tuple:
   *                  (operation-string (Buy, Sell, etc), amount, asset)
   * @return The list of changes
   * @throws RuntimeException When an operation string is incorrect
   */
  public static List<RawAccountChange> createSpotAccountChanges(long timestamp, String... changes)
      throws RuntimeException {
    final List<RawAccountChange> changeList = new LinkedList<>();
    for (int i = 0; i < changes.length; i += 3) {
      Operation operation;
      try {
        operation = Operation.fromString(changes[i]);
      } catch (IOException e) {
        throw new RuntimeException("Could not parse operation string: " + changes[i]);
      }
      Decimal amount = new Decimal(changes[i + 1]);
      String asset = changes[i + 2];
      changeList.add(new RawAccountChange(timestamp, AccountType.SPOT,
          operation, asset, amount, ""));
    }
    return changeList;
  }

  /**
   * Create a WalletDiff.
   *
   * @param assetAdditions The additions of assets as tuples (amount, asset)
   * @return The corresponding WalletDiff
   */
  public static WalletDiff createWalletDiff(String... assetAdditions) {
    WalletDiff diff = new WalletDiff();
    for (int i = 0; i < assetAdditions.length; i += 2) {
      diff.add(assetAdditions[i + 1], new Decimal(assetAdditions[i]));
    }
    return diff;
  }

  /**
   * Create wallet with given assets.
   *
   * @param assets Each asset is specified as a triplet (amount, asset, obtainPrice)
   * @return A new wallet with the given assets
   */
  public static Wallet createWalletWith(String... assets) {
    assertEquals(0, assets.length % 3, "Each asset must be specified with three values");
    Wallet w = new Wallet();
    for (int i = 0; i < assets.length; i += 3) {
      w.addAsset(assets[i + 1], new Decimal(assets[i]), new Decimal(assets[i + 2]));
    }
    return w;
  }

  /**
   * Create an auto-invest transaction, with a new subscription.
   *
   * @param amount The amount of change in the transaction
   * @param asset  The changed asset
   * @return The created auto-invest transaction
   */
  public static AutoInvestTransaction createAutoInvestTransaction(String amount, String asset) {
    long time = System.currentTimeMillis();
    Decimal changeAmount = new Decimal(amount);
    AutoInvestTransaction t = new AutoInvestTransaction(new Transaction(time),
        new AutoInvestSubscription(time, changeAmount.negate()));
    t.append(new RawAccountChange(time, AccountType.SPOT, Operation.AUTO_INVEST, asset,
        changeAmount, ""));
    return t;
  }

  /**
   * Process a deposit transaction.
   *
   * @param startSnapshot The starting wallet snapshot, before the transaction
   * @param asset         The deposited asset
   * @param amount        The deposited amount
   * @param obtainPrice   The obtain-price of the asset. When left null, no extra-info is provided
   * @return The wallet snapshot after processing the transaction
   */
  public static WalletSnapshot processDeposit(WalletSnapshot startSnapshot,
                                              String asset, String amount, String obtainPrice) {
    ExtraInfoEntry ei = null;
    if (obtainPrice != null) {
      ei = new ExtraInfoEntry(transactionTime, ExtraInfoType.ASSET_PRICE, asset, obtainPrice);
    }
    DepositTransaction deposit = createDeposit(amount, asset);
    return deposit.process(startSnapshot, ei);
  }

  /**
   * Create a deposit transaction.
   *
   * @param amount The deposited amount
   * @param asset  The deposited asset
   * @return Deposit transaction, with a new timestamp
   */
  public static DepositTransaction createDeposit(String amount, String asset) {
    transactionTime += 1000;
    Transaction t = new Transaction(transactionTime);
    t.append(new RawAccountChange(transactionTime, AccountType.SPOT, Operation.DEPOSIT,
        asset, new Decimal(amount), "Deposit"));
    return new DepositTransaction(t);
  }

  /**
   * Process a withdrawal transaction.
   *
   * @param startSnapshot    The starting wallet snapshot, before the transaction
   * @param asset            The withdrawn asset
   * @param amount           The withdrawn amount
   * @param realizationPrice The price at which the asset is realized. When non-null, it is supplied
   *                         to the transaction as ExtraInfo, when null, no ExtraInfo is supplied
   * @return The wallet snapshot after processing the transaction
   */
  public static WalletSnapshot processWithdraw(WalletSnapshot startSnapshot, String asset,
                                               String amount, String realizationPrice) {
    WithdrawTransaction withdraw = createWithdrawal(amount, asset);
    ExtraInfoEntry ei = null;
    if (realizationPrice != null) {
      ei = new ExtraInfoEntry(transactionTime, ExtraInfoType.ASSET_PRICE, asset, realizationPrice);
    }
    return withdraw.process(startSnapshot, ei);
  }

  /**
   * Create a withdrawal transaction.
   *
   * @param amount The withdrawn amount
   * @param asset  The withdrawn asset
   * @return Withdrawal transaction, with a new timestamp
   */
  public static WithdrawTransaction createWithdrawal(String amount, String asset) {
    transactionTime += 1000;
    Transaction t = new Transaction(transactionTime);
    t.append(new RawAccountChange(transactionTime, AccountType.SPOT, Operation.WITHDRAW,
        asset, new Decimal(amount), "Withdraw"));
    return new WithdrawTransaction(t);
  }

  /**
   * Process a buy-transaction.
   *
   * @param startSnapshot The starting wallet snapshot before the transaction
   * @param asset         The bought asset
   * @param amount        The bought amount
   * @param usedQuote     The quote currency amount
   * @param quoteCurrency The quote currency
   * @param fee           Fee amount
   * @param feeCurrency   Fee currency
   * @return Wallet snapshot after processing the transaction
   */
  public static WalletSnapshot processBuy(WalletSnapshot startSnapshot, String asset, String amount,
                                          String usedQuote, String quoteCurrency, String fee,
                                          String feeCurrency) {
    transactionTime += 1000;
    Transaction t = new Transaction(transactionTime);
    t.append(new RawAccountChange(transactionTime, AccountType.SPOT,
        Operation.BUY, asset, new Decimal(amount), "Buy coin"));
    t.append(new RawAccountChange(transactionTime, AccountType.SPOT,
        Operation.SELL, quoteCurrency, new Decimal(usedQuote).negate(),
        "Sell " + quoteCurrency));
    if (fee != null) {
      t.append(new RawAccountChange(transactionTime, AccountType.SPOT,
          Operation.FEE, feeCurrency, new Decimal(fee).negate(), "Fee in " + feeCurrency));
    }
    BuyTransaction buy = new BuyTransaction(t);
    return buy.process(startSnapshot, null);
  }

  /**
   * Process a sell-transaction.
   *
   * @param startSnapshot      The wallet snapshot before the transaction
   * @param asset              The sold asset
   * @param amount             The sold amount
   * @param obtainedUsdtAmount Obtained USDT
   * @param fee                Fee amount
   * @param feeCurrency        Fee currency
   * @return Wallet snapshot after processing the transaction
   */
  public static WalletSnapshot processSell(WalletSnapshot startSnapshot, String asset,
                                           String amount, String obtainedUsdtAmount, String fee,
                                           String feeCurrency) {
    transactionTime += 1000;
    Transaction t = new Transaction(transactionTime);
    t.append(new RawAccountChange(transactionTime, AccountType.SPOT,
        Operation.SELL, asset, new Decimal(amount).negate(), "Sell coin"));
    t.append(new RawAccountChange(transactionTime, AccountType.SPOT,
        Operation.BUY, "USDT", new Decimal(obtainedUsdtAmount), "Acquire USDT"));
    if (fee != null) {
      t.append(new RawAccountChange(transactionTime, AccountType.SPOT,
          Operation.FEE, feeCurrency, new Decimal(fee).negate(), "Fee in " + feeCurrency));
    }
    Transaction sell = t.clarifyTransactionType();
    assertInstanceOf(SellTransaction.class, sell);
    return sell.process(startSnapshot, null);
  }

  /**
   * Process an asset-distribution transaction.
   *
   * @param startSnapshot Wallet snapshot before the transaction
   * @param amount        Distributed amount
   * @param asset         Distributed asset
   * @return Wallet snapshot after processing the transaction
   */
  public static WalletSnapshot processDistribution(WalletSnapshot startSnapshot,
                                                   String amount, String asset) {
    transactionTime += 1000;
    Transaction t = new Transaction(transactionTime);
    t.append(new RawAccountChange(transactionTime, AccountType.SPOT, Operation.DISTRIBUTION, asset,
        new Decimal(amount), "Distribute " + amount + " " + asset));
    Transaction distribute = t.clarifyTransactionType();
    assertInstanceOf(DistributionTransaction.class, distribute);
    return distribute.process(startSnapshot, null);
  }

  /**
   * Expect the given wallet state for the given snapshot.
   *
   * @param ws             The snapshot to check
   * @param transactionPnl Expected profit-and-loss (PNL) for the last transaction
   * @param runningPnl     The accumulated running PNL
   * @param assets         The expected assets, specified as triplets (amount, asset, obtainPrice)
   */
  public static void expectWalletState(WalletSnapshot ws, String transactionPnl, String runningPnl,
                                       String... assets) {
    assertEquals(0, assets.length % 3,
        "Assets must be specified with triplets (amount, asset, obtainPrice)");
    int expectedAssetCount = assets.length / 3;
    assertEquals(expectedAssetCount, ws.getWallet().getAssetCount());
    for (int i = 0; i < assets.length; i += 3) {
      String amount = assets[i];
      String asset = assets[i + 1];
      String obtainPrice = assets[i + 2];
      expectAssetAmount(ws, asset, amount, obtainPrice);
    }
    expectPnl(ws, transactionPnl, runningPnl);
  }

  private static void expectAssetAmount(WalletSnapshot ws, String asset, String amount,
                                        String obtainPrice) {
    Decimal expectedAmount = new Decimal(amount);
    assertEquals(new Decimal(amount), ws.getWallet().getAssetAmount(asset));
    if (expectedAmount.isPositive()) {
      assertEquals(new Decimal(obtainPrice), ws.getWallet().getAvgObtainPrice(asset));
    }
  }

  /**
   * Expect the specified profit-and-loss (PNL) for the wallet snapshot.
   *
   * @param ws             The wallet snapshot to check
   * @param transactionPnl Expected PNL in the last transaction
   * @param runningPnl     The expected accumulated running-PNL
   */
  private static void expectPnl(WalletSnapshot ws, String transactionPnl, String runningPnl) {
    assertEquals(new Decimal(transactionPnl), ws.getTransaction().getPnl());
    assertEquals(new Decimal(runningPnl), ws.getPnl());
  }

  /**
   * Create an extra-info entry for auto-invest subscription.
   *
   * @param utcTime          The UCT timestamp to use
   * @param assetProportions The asset proportions in the investment. Each asset specified as
   *                         a tuple (asset, proportion), where the proportion is a decimal in
   *                         the range from "0.0" to "1.0".
   * @return The extra info, as expected by the auto-invest subscription
   */
  public static ExtraInfoEntry createAutoInvestExtraInfo(long utcTime,
                                                         String... assetProportions) {
    List<String> assets = extractEvenElementsFrom(assetProportions);
    List<String> proportions = extractOddElementsFrom(assetProportions);

    return new ExtraInfoEntry(utcTime, ExtraInfoType.AUTO_INVEST_PROPORTIONS,
        String.join("|", assets), String.join("|", proportions));
  }

  /**
   * Create ExtraInfo as it would be expected for the given transaction.
   *
   * @param t      The auto-invest transaction which timestamp will be used
   * @param assets The expected acquired assets
   * @return The expected info entry necessary for the corresponding subscription
   */
  public static ExtraInfoEntry createExpectedAutoInvestExtraInfo(Transaction t,
                                                                 String... assets) {
    return new ExtraInfoEntry(t.getUtcTime(), ExtraInfoType.AUTO_INVEST_PROPORTIONS,
        String.join("|", assets),
        "Proportions for assets in format proportion1|proportion2|...");
  }

  /**
   * Create raw account changes.
   *
   * @param changes Information for the changes. Each change is specified as a tuple
   *                (accountType, operation, amount, asset)
   * @return List of account changes
   * @throws IllegalArgumentException When some of the values in `changes` is wrong
   */
  public static List<RawAccountChange> createChanges(String... changes)
      throws IllegalArgumentException {
    List<RawAccountChange> changeList = new ArrayList<>();
    long time = System.currentTimeMillis();
    try {
      for (int i = 0; i < changes.length; i += 4) {
        AccountType accountType = AccountType.fromString(changes[i]);
        Operation operation = Operation.fromString(changes[i + 1]);
        Decimal amount = new Decimal(changes[i + 2]);
        String asset = changes[i + 3];
        changeList.add(new RawAccountChange(time, accountType, operation, asset, amount, ""));
      }
    } catch (IOException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
    return changeList;
  }

  private static List<String> extractEvenElementsFrom(String[] values) {
    List<String> stringList = new ArrayList<>(Arrays.asList(values));
    return stringList.stream().filter(s -> stringList.indexOf(s) % 2 == 0).toList();
  }

  private static List<String> extractOddElementsFrom(String[] values) {
    List<String> stringList = new ArrayList<>(Arrays.asList(values));
    return stringList.stream().filter(s -> stringList.indexOf(s) % 2 == 1).toList();
  }
}
