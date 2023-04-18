import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import no.strazdins.data.AccountType;
import no.strazdins.data.Decimal;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.WalletSnapshot;
import no.strazdins.transaction.DistributionTransaction;
import no.strazdins.transaction.SellTransaction;
import no.strazdins.transaction.Transaction;
import org.junit.jupiter.api.Test;

class DistributionTest {
  @Test
  void testDistributionWithoutPreviousCoin() {
    WalletSnapshot ws1 = WalletSnapshot.createEmpty();
    long time = System.currentTimeMillis();
    Transaction t = new Transaction(time);
    assertEquals(Decimal.ZERO, ws1.getWallet().getAssetAmount("BNB"));
    t.append(new RawAccountChange(time, AccountType.SPOT, Operation.DISTRIBUTION,
        "BNB", new Decimal("1.38"), "Got 1.38 BNB as a reward"));
    DistributionTransaction distribution = new DistributionTransaction(t);

    assertEquals(Decimal.ZERO, distribution.getAvgPriceInUsdt());
    assertEquals(time, distribution.getUtcTime());
    assertEquals(Decimal.ZERO, distribution.getFee());
    assertEquals(Decimal.ZERO, distribution.getObtainPrice());
    assertEquals("BNB", distribution.getBaseCurrency());
    assertEquals(Decimal.ZERO, distribution.getQuoteAmount());
    assertEquals("", distribution.getQuoteCurrency());
    assertNull(distribution.getNecessaryExtraInfo());
    assertEquals(new Decimal("1.38"), distribution.getBaseCurrencyAmount());

    WalletSnapshot ws2 = distribution.process(ws1, null);
    assertNotNull(ws2);
    assertEquals(new Decimal("1.38"), ws2.getWallet().getAssetAmount("BNB"));
    assertEquals(Decimal.ZERO, ws2.getWallet().getAvgObtainPrice("BNB"));
    assertEquals(Decimal.ZERO, ws2.getPnl());

    Transaction t2 = new Transaction(time + 1000);
    t2.append(new RawAccountChange(time + 1000, AccountType.SPOT, Operation.TRANSACTION_RELATED,
        "BNB", new Decimal("-0.38"), "Sell 0.38 BNB"));
    t2.append(new RawAccountChange(time + 1000, AccountType.SPOT, Operation.BUY, "USDT",
        new Decimal("380.2"), "Get $380.20"));
    t2.append(new RawAccountChange(time + 1000, AccountType.SPOT, Operation.FEE, "USDT",
        new Decimal("-0.2"), "Pay $0.20 in fees"));
    SellTransaction sell = new SellTransaction(t2);

    WalletSnapshot ws3 = sell.process(ws2, null);
    assertEquals(Decimal.ONE, ws3.getWallet().getAssetAmount("BNB"));
    assertEquals(new Decimal("380"), ws3.getWallet().getAssetAmount("USDT"));
    assertEquals(new Decimal("380"), ws3.getPnl());
  }

  @Test
  void testDistributionWithPreviousCoin() {
    WalletSnapshot ws1 = WalletSnapshot.createEmpty();
    ws1.addAsset("BNB", new Decimal("2.0"), new Decimal("15"));

    long time = System.currentTimeMillis();
    Transaction t = new Transaction(time);
    t.append(new RawAccountChange(time, AccountType.SPOT, Operation.DISTRIBUTION,
        "BNB", Decimal.ONE, "Got 1 BNB as a reward"));
    DistributionTransaction distribution = new DistributionTransaction(t);

    WalletSnapshot ws2 = distribution.process(ws1, null);
    assertNotNull(ws2);
    assertEquals(new Decimal("3"), ws2.getWallet().getAssetAmount("BNB"));
    assertEquals(new Decimal("10"), ws2.getWallet().getAvgObtainPrice("BNB"));
  }


}
