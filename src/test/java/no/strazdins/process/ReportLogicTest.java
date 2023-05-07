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
    // TODO - simple auto-invest
    List<Transaction> transactions = createAutoInvestments(
        "-5", "USDT",
        "0.00141986", "BNB",
        "0.00018789", "BTC",
        "0.00030772", "ETH"
    );
    assertEquals(4, transactions.size());
    assertInstanceOf(AutoInvestTransaction.class, transactions.get(0));
    AutoInvestTransaction firstTransaction = (AutoInvestTransaction) transactions.get(0);
    assertTrue(firstTransaction.isInvestment());
    assertFalse(firstTransaction.isAcquisition());
    assertNull(firstTransaction.getBoughtAsset());
    assertEquals("USDT", firstTransaction.getInvestedAsset());
    assertEquals(new Decimal("-5"), firstTransaction.getBaseCurrencyAmount());
  }


  @Test
  void testTwoAutoInvestments() {
    // TODO
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
    // TODO - clarify transactionTypes
    //    return logic.clarifyTransactionTypes(rawTransactions);
    return rawTransactions;
  }
}
