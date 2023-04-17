import static org.junit.jupiter.api.Assertions.assertEquals;

import no.strazdins.data.Decimal;
import no.strazdins.data.Wallet;
import org.junit.jupiter.api.Test;

class WalletTest {
  @Test
  void testCloning() {
    Wallet w1 = new Wallet();
    w1.addAsset("BTC", Decimal.ONE, new Decimal("24000"));

    Wallet w2 = new Wallet(w1);
    assertEquals(w1, w2);

    w2.addAsset("BTC", new Decimal("1.2"), new Decimal("30000"));
    w2.addAsset("LTC", new Decimal("3"), new Decimal("30"));

    assertEquals(1, w1.getAssetCount());
    assertEquals(2, w2.getAssetCount());

    assertEquals("1", w1.getAssetAmount("BTC").getNiceString());
    assertEquals("2.2", w2.getAssetAmount("BTC").getNiceString());
    assertEquals(Decimal.ZERO, w1.getAssetAmount("LTC"));
    assertEquals("3", w2.getAssetAmount("LTC").getNiceString());
  }

  @Test
  void testAddDecrease() {
    Wallet w = new Wallet();
    assertEquals(Decimal.ZERO, w.getAssetAmount("BTC"));
    w.addAsset("BTC", new Decimal("1.23"), new Decimal("20000"));
    assertEquals(new Decimal("1.23"), w.getAssetAmount("BTC"));
    assertEquals(new Decimal("20000"), w.getAvgObtainPrice("BTC"));

    w.addAsset("BTC", new Decimal("1.77"), new Decimal("10000"));
    assertEquals(new Decimal("3"), w.getAssetAmount("BTC"));
    assertEquals(new Decimal("14100"), w.getAvgObtainPrice("BTC"));

    w.decreaseAsset("BTC", new Decimal("1.88"));
    assertEquals(new Decimal("1.12"), w.getAssetAmount("BTC"));
    assertEquals(new Decimal("14100"), w.getAvgObtainPrice("BTC"));

    w.decreaseAsset("BTC", new Decimal("1.12"));
    assertEquals(Decimal.ZERO, w.getAssetAmount("BTC"));
    assertEquals(0, w.getAssetCount());
    assertEquals(Decimal.ZERO, w.getAvgObtainPrice("BTC"));
  }
}
