package no.strazdins.data;

import static no.strazdins.testtools.TestTools.createWalletDiff;
import static no.strazdins.testtools.TestTools.createWalletWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class WalletDiffTest {
  @Test
  void testSimpleEquality() {
    assertEquals(new WalletDiff(), new WalletDiff());
    assertNotEquals(new WalletDiff(), createWalletDiff("1", "BTC"));
  }

  @Test
  void testEqualAdditions() {
    assertEquals(
        createWalletDiff("1", "BTC"),
        createWalletDiff("1", "BTC")
    );
    assertEquals(
        createWalletDiff("1", "BTC", "2", "BTC"),
        createWalletDiff("2", "BTC", "1", "BTC")
    );
    assertEquals(
        createWalletDiff("2", "BTC", "3", "BTC"),
        createWalletDiff("4", "BTC", "1", "BTC")
    );
  }

  @Test
  void testAdd() {
    WalletDiff diff = new WalletDiff();
    assertEquals(Decimal.ZERO, diff.getAmount("BTC"));

    diff.add("BTC", new Decimal("1.23"));
    assertEquals(new Decimal("1.23"), diff.getAmount("BTC"));

    diff.add("BTC", new Decimal("4.2"));
    assertEquals(new Decimal("5.43"), diff.getAmount("BTC"));

    diff.add("BTC", new Decimal("-8.43"));
    assertEquals(new Decimal("-3"), diff.getAmount("BTC"));
  }

  @Test
  void testAddBecomesZero() {
    WalletDiff diff = createWalletDiff("1.23", "BTC");
    assertEquals(new Decimal("1.23"), diff.getAmount("BTC"));

    diff.add("BTC", new Decimal("-1.23"));
    assertEquals(Decimal.ZERO, diff.getAmount("BTC"));
  }

  @Test
  void testEqualAssets() {
    WalletDiff d1 = createWalletDiff("2", "LTC");
    WalletDiff d2 = createWalletDiff("2", "LTC");
    assertEquals(d1, d2);

    d1 = createWalletDiff("1", "LTC", "2", "BTC");
    d2 = createWalletDiff("2", "BTC", "1", "LTC");
    assertEquals(d1, d2);

    d1 = createWalletDiff("1", "LTC", "2", "BTC", "3", "LTC", "0.1", "BTC");
    d2 = createWalletDiff("1.1", "BTC", "8", "LTC", "1", "BTC", "-4", "LTC");
    assertEquals(d1, d2);

  }

  @Test
  void testEqualAssetsDifferentAmounts() {
    WalletDiff d1 = createWalletDiff("1", "LTC", "2", "BTC", "3", "LTC", "0.1", "BTC");
    WalletDiff d2 = createWalletDiff("1.2", "BTC", "8", "LTC", "1", "BTC", "-4", "LTC");
    assertNotEquals(d1, d2);

    d1 = createWalletDiff("1", "LTC", "2", "BTC", "3", "LTC", "0.1", "BTC", "0.80000001", "BNB");
    d2 = createWalletDiff("1", "LTC", "2", "BTC", "3", "LTC", "0.1", "BTC", "0.8", "BNB");
    assertNotEquals(d1, d2);
  }

  @Test
  void testAddAll() {
    Wallet w = createWalletWith(
        "1.1", "BTC", "10000",
        "20", "LTC", "100",
        "2", "BNB", "10"
    );
    WalletDiff diff = new WalletDiff().addAll(w);
    WalletDiff expectedDiff = createWalletDiff(
        "1.1", "BTC",
        "20", "LTC",
        "2", "BNB"
    );
    assertEquals(expectedDiff, diff);
  }

  @Test
  void testRemoveAll() {
    WalletDiff diff = createWalletDiff(
        "1.1", "BTC",
        "20", "LTC",
        "2", "BNB"
    );
    Wallet w = createWalletWith(
        "0.1", "BTC", "10000",
        "2", "LTC", "100"
    );
    diff.removeAll(w);

    WalletDiff expectedDiff = createWalletDiff(
        "1", "BTC",
        "18", "LTC",
        "2", "BNB"
    );
    assertEquals(expectedDiff, diff);
  }
}
