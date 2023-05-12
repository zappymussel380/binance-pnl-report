package no.strazdins.transaction;

import static no.strazdins.testtools.TestTools.createAutoInvestTransaction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import no.strazdins.data.AccountType;
import no.strazdins.data.Decimal;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import org.junit.jupiter.api.Test;

class AutoInvestTransactionTest {
  @Test
  void testBoughtAsset() {
    AutoInvestTransaction t = createAutoInvestTransaction("-5", "USDT");
    assertNull(t.getBoughtAsset());
    t = createAutoInvestTransaction("0.02", "BNB");
    assertEquals("BNB", t.getBoughtAsset());
  }

  @Test
  void testIsAutoInvestOperation() {
    RawAccountChange change = new RawAccountChange(0, AccountType.SPOT, Operation.AUTO_INVEST,
        "USDT", new Decimal("-5"), "");
    assertTrue(AutoInvestTransaction.isAutoInvestOperation(change));
  }
}
