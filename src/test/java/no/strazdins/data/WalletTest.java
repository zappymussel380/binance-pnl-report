package no.strazdins.data;

import static no.strazdins.testtools.TestTools.createWalletWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

  @Test
  void testDiffEmpty() {
    assertEquals(new WalletDiff(), new Wallet().getDiffFrom(new Wallet()));
  }

  @Test
  void testDiff() {
    Wallet w1 = createWalletWith(
        "1.1", "BTC", "10000",
        "0.2", "BTC", "9000",
        "0.8", "BTC", "8000",
        "20", "LTC", "100",
        "10", "LTC", "120",
        "200", "USDT", "1",
        "2", "BNB", "10",
        "1", "BNB", "10"
    );
    Wallet w2 = createWalletWith(
        "1.2", "BTC", "10000", // +0.1 BTC
        "0.2", "BTC", "9000",
        "0.8", "BTC", "8000",
        "22", "LTC", "100", // -8 LTC, in one portion
        "150", "USDT", "1", // Same amount of USDT, just in two portions
        "50", "USDT", "1",
        // All of BNB removed
        // 15 XRP added, in one portion
        "15", "XRP", "3",
        // 400 SLP added, in three portions
        "100", "SLP", "0.5",
        "200", "SLP", "0.4",
        "100", "SLP", "0.3"
    );
    WalletDiff expectedDiff = new WalletDiff()
        .add("BTC", new Decimal("0.1"))
        .add("LTC", new Decimal("-8"))
        .add("BNB", new Decimal("-3"))
        .add("XRP", new Decimal("15"))
        .add("SLP", new Decimal("400"));
    assertEquals(expectedDiff, w2.getDiffFrom(w1));
  }
}
