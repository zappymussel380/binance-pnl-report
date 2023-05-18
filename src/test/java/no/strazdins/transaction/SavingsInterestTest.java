package no.strazdins.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import no.strazdins.data.AccountType;
import no.strazdins.data.Decimal;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.WalletSnapshot;
import org.junit.jupiter.api.Test;


class SavingsInterestTest {
  @Test
  void testUsdInterest() {
    WalletSnapshot ws1 = WalletSnapshot.createEmpty();
    ws1.getWallet().addAsset("USDT", new Decimal("4173.87761154"), Decimal.ONE);

    long time = System.currentTimeMillis();
    Transaction t = new Transaction(time);
    t.append(new RawAccountChange(time, AccountType.EARN, Operation.EARN_INTEREST,
        "USDT", new Decimal("1.38"), "Got 1.38 USD in interest"));
    SavingsInterestTransaction interest = new SavingsInterestTransaction(t);
    WalletSnapshot ws2 = interest.process(ws1, null);
    assertNotNull(ws2);
    assertEquals(new Decimal("4175.25761154"), ws2.getWallet().getAssetAmount("USDT"));
    assertEquals(new Decimal("0.99966948"), ws2.getWallet().getAvgObtainPrice("USDT"));
  }
}
