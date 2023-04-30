package no.strazdins.transaction;

import static no.strazdins.data.Operation.BUY;
import static no.strazdins.data.Operation.FEE;
import static no.strazdins.data.Operation.TRANSACTION_RELATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import no.strazdins.data.AccountType;
import no.strazdins.data.Decimal;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.WalletDiff;
import org.junit.jupiter.api.Test;

class TransactionTest {
  @Test
  void testClarifyTransactionType() {
    long time = System.currentTimeMillis();
    Transaction t = new Transaction(time);
    assertNull(t.clarifyTransactionType());

    t.append(new RawAccountChange(time, AccountType.SPOT, Operation.DEPOSIT, "BTC",
        Decimal.ONE, ""));

    assertInstanceOf(DepositTransaction.class, t.clarifyTransactionType());
  }

  @Test
  void testMerge() {
    long thisTime = System.currentTimeMillis();
    Transaction t = new Transaction(thisTime);
    appendOperation(t, FEE, "-1", "BNB");
    appendOperation(t, BUY, "0.1", "BTC");
    appendOperation(t, TRANSACTION_RELATED, "-2000", "USDT");
    appendOperation(t, BUY, "0.2", "BTC");
    appendOperation(t, TRANSACTION_RELATED, "-6000", "USDT");
    appendOperation(t, TRANSACTION_RELATED, "-4000", "USDT");
    appendOperation(t, BUY, "0.3", "BTC");
    appendOperation(t, FEE, "-2", "BNB");
    appendOperation(t, FEE, "-3", "BNB");
    Transaction merged = t.clarifyTransactionType();
    assertNotNull(merged);
    assertInstanceOf(BuyTransaction.class, merged);

    assertEquals("BTC", merged.getBaseCurrency());
    assertEquals("USDT", merged.getQuoteCurrency());
    assertEquals("BNB", merged.getFeeCurrency());
    assertEquals(new Decimal("0.6"), merged.getBaseCurrencyAmount());
    assertEquals(new Decimal("-12000"), merged.getQuoteAmount());
    assertEquals(new Decimal("-6"), merged.getFee());
  }

  @Test
  void testOperationDiff() {
    long thisTime = System.currentTimeMillis();
    Transaction t = new Transaction(thisTime);
    appendOperation(t, FEE, "-1", "BNB");
    appendOperation(t, BUY, "0.1", "BTC");
    appendOperation(t, TRANSACTION_RELATED, "-2000", "USDT");
    appendOperation(t, BUY, "0.2", "BTC");
    appendOperation(t, TRANSACTION_RELATED, "-6000", "USDT");
    appendOperation(t, BUY, "0.3", "BTC");
    WalletDiff diff = t.getOperationDiff();
    WalletDiff expectedDiff = new WalletDiff()
        .add("BNB", new Decimal("-1"))
        .add("BTC", new Decimal("0.6"))
        .add("USDT", new Decimal("-8000"));
    assertEquals(expectedDiff, diff);
  }

  private void appendOperation(Transaction t, Operation operation, String amount, String asset) {
    t.append(new RawAccountChange(t.utcTime, AccountType.SPOT, operation, asset,
        new Decimal(amount), ""));
  }
}
