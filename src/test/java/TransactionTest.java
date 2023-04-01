import no.strazdins.data.AccountType;
import no.strazdins.data.Decimal;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.transaction.DepositTransaction;
import no.strazdins.transaction.Transaction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
