package no.strazdins.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedList;
import java.util.List;
import no.strazdins.data.AccountType;
import no.strazdins.data.Decimal;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.transaction.AutoInvestTransaction;
import no.strazdins.transaction.Transaction;
import org.junit.jupiter.api.Test;

class ReportLogicTest {
  @Test
  void testAutoInvest() {
    List<Transaction> transactions = createAutoInvestments(
        "-5", "USDT",
        "0.00141986", "BNB",
        "0.00018789", "BTC",
        "0.00030772", "ETH"
    );
    assertEquals(4, transactions.size());
    expectInvestment(transactions.get(0), "-5", "USDT");
    expectSameSubscription(transactions);
  }


  @Test
  void testTwoAutoInvestments() {
    List<Transaction> transactions = createAutoInvestments(
        "-5", "USDT",
        "0.00141986", "BNB",
        "0.00018789", "BTC",
        "0.00030772", "ETH",
        "-5", "USDT",
        "0.00019", "BTC",
        "0.0014", "BNB",
        "0.0003", "ETH"
    );
    assertEquals(8, transactions.size());
    expectInvestment(transactions.get(0), "-5", "USDT");
    expectAcquisition(transactions.get(1), "0.00141986", "BNB");
    expectAcquisition(transactions.get(2), "0.00018789", "BTC");
    expectAcquisition(transactions.get(3), "0.00030772", "ETH");
    expectInvestment(transactions.get(4), "-5", "USDT");
    expectAcquisition(transactions.get(5), "0.00019", "BTC");
    expectAcquisition(transactions.get(6), "0.0014", "BNB");
    expectAcquisition(transactions.get(7), "0.0003", "ETH");
    expectSameSubscription(transactions);
  }

  @Test
  void testAutoInvestSubscriptionChanges() {
    // TODO
  }

  @Test
  void testAutoInvestSubscriptionChangeByUsdAmount() {
    // TODO
  }

  /**
   * Create a list of auto-invest transactions.
   *
   * @param assets The asset changes in the transactions. Each change is specified as a
   *               string tuple (amount, asset). Fees and earnings subscriptions are not generated
   * @return List of auto-invest transactions
   */
  private List<Transaction> createAutoInvestments(String... assets) {
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

  private void expectInvestment(Transaction transaction, String amount, String asset) {
    assertInstanceOf(AutoInvestTransaction.class, transaction);
    AutoInvestTransaction investment = (AutoInvestTransaction) transaction;
    assertTrue(investment.isInvestment());
    assertFalse(investment.isAcquisition());
    assertNull(investment.getBoughtAsset());
    assertEquals(asset, investment.getInvestedAsset());
    assertEquals(new Decimal(amount), investment.getAmount());
  }

  private void expectAcquisition(Transaction transaction, String amount, String asset) {
    assertInstanceOf(AutoInvestTransaction.class, transaction);
    AutoInvestTransaction acquisition = (AutoInvestTransaction) transaction;
    assertTrue(acquisition.isAcquisition());
    assertFalse(acquisition.isInvestment());
    assertNull(acquisition.getInvestedAsset());
    assertEquals(asset, acquisition.getBoughtAsset());
    assertEquals(new Decimal(amount), acquisition.getAmount());

  }

  private void expectSameSubscription(List<Transaction> transactions) {
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
}
