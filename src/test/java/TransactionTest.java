import no.strazdins.data.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class TransactionTest {

  @Test
  void testClarifyTransactionType() {
    long time = System.currentTimeMillis();
    Transaction t = new Transaction(time);
    assertNull(t.clarifyTransactionType());

    t.append(new RawAccountChange(time, AccountType.SPOT, Operation.DEPOSIT, "BTC", "1.0", ""));

    assertInstanceOf(DepositTransaction.class, t.clarifyTransactionType());
  }
}
