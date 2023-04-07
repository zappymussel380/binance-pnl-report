import no.strazdins.data.AccountType;
import no.strazdins.data.Decimal;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.transaction.BuyTransaction;
import no.strazdins.transaction.DepositTransaction;
import no.strazdins.transaction.Transaction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
    t.append(new RawAccountChange(thisTime, AccountType.SPOT, Operation.FEE, "BNB",
        new Decimal("-1"), ""));
    t.append(new RawAccountChange(thisTime, AccountType.SPOT, Operation.BUY, "BTC",
        new Decimal("0.1"), ""));
    t.append(new RawAccountChange(thisTime, AccountType.SPOT, Operation.TRANSACTION_RELATED, "USDT",
        new Decimal("-2000"), ""));
    t.append(new RawAccountChange(thisTime, AccountType.SPOT, Operation.BUY, "BTC",
        new Decimal("0.2"), ""));
    t.append(new RawAccountChange(thisTime, AccountType.SPOT, Operation.TRANSACTION_RELATED, "USDT",
        new Decimal("-6000"), ""));
    t.append(new RawAccountChange(thisTime, AccountType.SPOT, Operation.TRANSACTION_RELATED, "USDT",
        new Decimal("-4000"), ""));
    t.append(new RawAccountChange(thisTime, AccountType.SPOT, Operation.BUY, "BTC",
        new Decimal("0.3"), ""));
    t.append(new RawAccountChange(thisTime, AccountType.SPOT, Operation.FEE, "BNB",
        new Decimal("-2"), ""));
    t.append(new RawAccountChange(thisTime, AccountType.SPOT, Operation.FEE, "BNB",
        new Decimal("-3"), ""));
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
}
