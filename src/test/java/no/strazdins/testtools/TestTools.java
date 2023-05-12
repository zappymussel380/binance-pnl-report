package no.strazdins.testtools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import no.strazdins.data.AccountType;
import no.strazdins.data.Decimal;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.process.AutoInvestSubscription;
import no.strazdins.process.ReportLogic;
import no.strazdins.transaction.AutoInvestTransaction;
import no.strazdins.transaction.Transaction;

/**
 * Different tools which make tests more readable.
 */
public class TestTools {
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
}
