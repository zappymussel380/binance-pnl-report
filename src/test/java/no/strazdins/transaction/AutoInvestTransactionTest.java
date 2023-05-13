package no.strazdins.transaction;

import static no.strazdins.testtools.TestTools.createAutoInvestments;
import static no.strazdins.testtools.TestTools.expectAcquisition;
import static no.strazdins.testtools.TestTools.expectInvestment;
import static no.strazdins.testtools.TestTools.expectNotSameSubscription;
import static no.strazdins.testtools.TestTools.expectSameSubscription;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import no.strazdins.data.AccountType;
import no.strazdins.data.Decimal;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import org.junit.jupiter.api.Test;

class AutoInvestTransactionTest {
  @Test
  void testBoughtAsset() {
    List<Transaction> transactions = createAutoInvestments(
        "-5", "USDT",
        "0.02", "BNB"
    );
    assertEquals(2, transactions.size());
    AutoInvestTransaction invest = (AutoInvestTransaction) transactions.get(0);
    AutoInvestTransaction acquire = (AutoInvestTransaction) transactions.get(1);
    assertNull(invest.getBoughtAsset());
    assertEquals("BNB", acquire.getBoughtAsset());
  }

  @Test
  void testInvestedAsset() {
    List<Transaction> transactions = createAutoInvestments(
        "-5", "USDT",
        "0.02", "BNB"
    );
    assertEquals(2, transactions.size());
    AutoInvestTransaction invest = (AutoInvestTransaction) transactions.get(0);
    AutoInvestTransaction acquire = (AutoInvestTransaction) transactions.get(1);
    assertNull(acquire.getInvestedAsset());
    assertEquals("USDT", invest.getInvestedAsset());
  }

  @Test
  void testIsAutoInvestOperation() {
    RawAccountChange change = new RawAccountChange(0, AccountType.SPOT, Operation.AUTO_INVEST,
        "USDT", new Decimal("-5"), "");
    assertTrue(AutoInvestTransaction.isAutoInvestOperation(change));
  }

  @Test
  void testClarifiedAsAutoInvest() {
    List<Transaction> transactions = createAutoInvestments(
        "-5", "USDT",
        "0.00141986", "BNB",
        "0.00018789", "BTC",
        "0.00030772", "ETH"
    );
    assertEquals(4, transactions.size());
    expectInvestment(transactions.get(0), "-5", "USDT");
    expectSameSubscription(transactions);
    AutoInvestTransaction invest = (AutoInvestTransaction) transactions.get(0);
    assertEquals(new Decimal("5"), invest.getSubscription().getInvestmentAmount());
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
    List<Transaction> transactions = createAutoInvestments(
        "-5", "USDT",
        "0.00141986", "BNB",
        "0.00018789", "BTC",
        "0.00030772", "ETH",
        "-5", "USDT",
        "0.00019", "BTC",
        "0.0003", "ETH",
        // We need to have one more instance of the second auto-invest, otherwise
        // the change won't be detected
        "-5", "USDT",
        "0.00019", "BTC",
        "0.0003", "ETH"
    );
    assertEquals(10, transactions.size());
    expectNotSameSubscription(transactions.get(0), transactions.get(4));
    expectSameSubscription(transactions.subList(0, 4));
    expectSameSubscription(transactions.subList(4, 10));
  }


  @Test
  void testAutoInvestSubscriptionChangeByUsdAmount() {
    List<Transaction> transactions = createAutoInvestments(
        "-5", "USDT",
        "0.00141986", "BNB",
        "0.00018789", "BTC",
        "0.00030772", "ETH",
        "-10", "USDT",
        "0.0028", "BNB",
        "0.0004", "BTC",
        "0.0006", "ETH",
        // We need to have one more instance of the second auto-invest, otherwise
        // the change won't be detected
        "-10", "USDT",
        "0.0028", "BNB",
        "0.0004", "BTC",
        "0.0006", "ETH"
    );
    assertEquals(12, transactions.size());
    expectNotSameSubscription(transactions.get(0), transactions.get(4));
    expectSameSubscription(transactions.subList(0, 4));
    expectSameSubscription(transactions.subList(4, 12));
  }
}
