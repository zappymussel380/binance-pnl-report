package no.strazdins.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import no.strazdins.data.AccountType;
import no.strazdins.data.Decimal;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.process.AutoInvestSubscription;
import org.junit.jupiter.api.Test;

class AutoInvestTransactionTest {
  @Test
  void testBoughtAsset() {
    AutoInvestTransaction t = createTransaction("-5", "USDT");
    assertNull(t.getBoughtAsset());
    t = createTransaction("0.02", "BNB");
    assertEquals("BNB", t.getBoughtAsset());
  }

  private AutoInvestTransaction createTransaction(String amount, String asset) {
    long time = System.currentTimeMillis();
    Decimal changeAmount = new Decimal(amount);
    AutoInvestTransaction t = new AutoInvestTransaction(new Transaction(time),
        new AutoInvestSubscription(changeAmount));
    t.append(new RawAccountChange(time, AccountType.SPOT, Operation.AUTO_INVEST, asset,
        changeAmount, ""));
    return t;
  }

  @Test
  void testIsAutoInvestOperation() {
    RawAccountChange change = new RawAccountChange(0, AccountType.SPOT, Operation.AUTO_INVEST,
        "USDT", new Decimal("-5"), "");
    assertTrue(AutoInvestTransaction.isAutoInvestOperation(change));
  }
}
