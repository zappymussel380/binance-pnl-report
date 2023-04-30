package no.strazdins.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class WalletDiffTest {
  @Test
  void testSimpleEquality() {
    assertEquals(new WalletDiff(), new WalletDiff());
    assertNotEquals(new WalletDiff(), createDiff("1", "BTC"));
  }

  @Test
  void testEqualAdditions() {
    assertEquals(
        createDiff("1", "BTC"),
        createDiff("1", "BTC")
    );
    assertEquals(
        createDiff("1", "BTC", "2", "BTC"),
        createDiff("2", "BTC", "1", "BTC")
    );
    assertEquals(
        createDiff("2", "BTC", "3", "BTC"),
        createDiff("4", "BTC", "1", "BTC")
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
    WalletDiff diff = createDiff("1.23", "BTC");
    assertEquals(new Decimal("1.23"), diff.getAmount("BTC"));

    diff.add("BTC", new Decimal("-1.23"));
    assertEquals(Decimal.ZERO, diff.getAmount("BTC"));
  }

  @Test
  void testEqualAssets() {
    WalletDiff d1 = createDiff("2", "LTC");
    WalletDiff d2 = createDiff("2", "LTC");
    assertEquals(d1, d2);

    d1 = createDiff("1", "LTC", "2", "BTC");
    d2 = createDiff("2", "BTC", "1", "LTC");
    assertEquals(d1, d2);

    d1 = createDiff("1", "LTC", "2", "BTC", "3", "LTC", "0.1", "BTC");
    d2 = createDiff("1.1", "BTC", "8", "LTC", "1", "BTC", "-4", "LTC");
    assertEquals(d1, d2);

  }

  @Test
  void testEqualAssetsDifferentAmounts() {
    WalletDiff d1 = createDiff("1", "LTC", "2", "BTC", "3", "LTC", "0.1", "BTC");
    WalletDiff d2 = createDiff("1.2", "BTC", "8", "LTC", "1", "BTC", "-4", "LTC");
    assertNotEquals(d1, d2);

    d1 = createDiff("1", "LTC", "2", "BTC", "3", "LTC", "0.1", "BTC", "0.80000001", "BNB");
    d2 = createDiff("1", "LTC", "2", "BTC", "3", "LTC", "0.1", "BTC", "0.8", "BNB");
    assertNotEquals(d1, d2);
  }

  @Test
  void testAddAll() {
    Wallet w = WalletTest.createWalletWith(
        "1.1", "BTC", "10000",
        "20", "LTC", "100",
        "2", "BNB", "10"
    );
    WalletDiff diff = new WalletDiff().addAll(w);
    WalletDiff expectedDiff = createDiff(
        "1.1", "BTC",
        "20", "LTC",
        "2", "BNB"
    );
    assertEquals(expectedDiff, diff);
  }

  @Test
  void testRemoveAll() {
    WalletDiff diff = createDiff(
        "1.1", "BTC",
        "20", "LTC",
        "2", "BNB"
    );
    Wallet w = WalletTest.createWalletWith(
        "0.1", "BTC", "10000",
        "2", "LTC", "100"
    );
    diff.removeAll(w);

    WalletDiff expectedDiff = createDiff(
        "1", "BTC",
        "18", "LTC",
        "2", "BNB"
    );
    assertEquals(expectedDiff, diff);
  }

  /**
   * Create a WalletDiff.
   *
   * @param assetAdditions The additions of assets as tuples (amount, asset)
   * @return The corresponding WalletDiff
   */
  private WalletDiff createDiff(String... assetAdditions) {
    WalletDiff diff = new WalletDiff();
    for (int i = 0; i < assetAdditions.length; i += 2) {
      diff.add(assetAdditions[i + 1], new Decimal(assetAdditions[i]));
    }
    return diff;
  }
}
