package no.strazdins.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import no.strazdins.data.AccountType;
import no.strazdins.data.Decimal;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.Wallet;
import no.strazdins.data.WalletSnapshot;
import org.junit.jupiter.api.Test;

/**
 * Tests related to dust collection transactions.
 */
class DustCollectionTest {
  @Test
  void simpleConstructionTest() {
    long time = System.currentTimeMillis();
    Transaction t = new Transaction(time);
    t.append(new RawAccountChange(time, AccountType.SPOT, Operation.SMALL_ASSETS_EXCHANGE_BNB,
        "BNB", new Decimal("0.1"), "Got BNB"));
    t.append(new RawAccountChange(time, AccountType.SPOT, Operation.SMALL_ASSETS_EXCHANGE_BNB,
        "SXP", new Decimal("-8"), "Exchanged SXP dust"));
    Transaction d = t.clarifyTransactionType();
    assertNotNull(d, "Dust transaction must be correctly interpreted");
    assertInstanceOf(DustCollectionTransaction.class, d,
        "Dust transaction must be correctly interpreted");
    assertEquals("Convert dust to BNB", d.getType());
    assertEquals(time, d.getUtcTime());
    assertEquals(new Decimal("0.1"), d.getQuoteAmount());
    assertEquals("BNB", d.getQuoteCurrency());
    assertEquals("SXP", d.getBaseCurrency());
    DustCollectionTransaction dust = (DustCollectionTransaction) d;
    assertEquals(1, dust.getDustAssetCount());
    assertEquals(new Decimal("-8"), d.getBaseCurrencyAmount());
    assertEquals(Decimal.ZERO, d.getFee());
    assertEquals("", d.getFeeCurrency());
    assertNull(d.getNecessaryExtraInfo());
  }

  @Test
  void multiDustConstructorTest() {
    DustCollectionTransaction dust = createDustCollection("0.1", "BNB",
        "-3", "SXP", "-0.2", "LEND", "-82", "SHIB", "0.2", "BNB", "0.3", "BNB",
        "-1", "SXP", "-2", "SXP");
    assertEquals("BNB", dust.getQuoteCurrency());
    assertEquals(new Decimal("0.6"), dust.getQuoteAmount());
    assertEquals(Decimal.ZERO, dust.getBaseCurrencyAmount());
    assertEquals("LEND+SHIB+SXP", dust.getBaseCurrency());
    assertEquals("Dust collect 0.6 BNB, -0.2 LEND, -82 SHIB, -6 SXP", dust.toString());
  }

  @Test
  void testOneDust() {
    expectDustResult(
        createWallet("REN", "4", "0"),
        createDustCollection("0.1", "BNB", "-3.8", "REN"),
        "REN", "0.2", "0", "BNB", "0.1", "0"
    );
    expectDustResult(
        createWallet("REN", "4", "10"),
        createDustCollection("0.1", "BNB", "-3", "REN"),
        "REN", "1", "10", "BNB", "0.1", "300"
    );
    expectDustResult(
        createWallet("REN", "4", "0", "BNB", "0.9", "100"),
        createDustCollection("0.1", "BNB", "-3.8", "REN"),
        "REN", "0.2", "0", "BNB", "1", "90"
    );
    expectDustResult(
        createWallet("REN", "4", "10", "BNB", "0.9", "100"),
        createDustCollection("0.1", "BNB", "-3", "REN"),
        "REN", "1", "10", "BNB", "1", "120"
    );
  }

  @Test
  void testMultiDust() {
    expectDustResult(
        createWallet("REN", "4", "0", "SXP", "2", "0", "BNB", "2", "0"),
        createDustCollection("2", "BNB", "-4", "REN", "-2", "SXP"),
        "BNB", "4", "0"
    );
    expectDustResult(
        createWallet("REN", "4", "0", "SXP", "2", "0", "BNB", "2", "0"),
        createDustCollection("2", "BNB", "-1", "REN", "-1", "SXP"),
        "REN", "3", "0", "SXP", "1", "0", "BNB", "4", "0"
    );

    expectDustResult(
        createWallet("REN", "4", "0", "SXP", "2", "0", "BNB", "2", "100"),
        createDustCollection("2", "BNB", "-4", "REN", "-2", "SXP"),
        "BNB", "4", "50"
    );

    expectDustResult(
        createWallet("REN", "4", "10", "SXP", "2", "30", "BNB", "2", "100"),
        createDustCollection("8", "BNB", "-4", "REN", "-2", "SXP"),
        "BNB", "10", "30"
    );
  }

  @Test
  void testMerge() {
    DustCollectionTransaction dust = createDustCollection("0.1", "BNB",
        "-3", "SXP", "-0.2", "LEND", "-82", "SHIB", "0.2", "BNB", "0.3", "BNB",
        "-1", "SXP", "-2", "SXP");
    expectDustChangeAmounts(dust, "0.6", "BNB", "-0.2", "LEND", "-6", "SXP", "-82", "SHIB");
  }


  /**
   * Create a dust collection transaction.
   *
   * @param assets A list of asset changes in the dust collection. Each change
   *               is represented as two strings: the amount and the asset.
   * @return A Dust collection transaction representing the desired account change
   */
  private DustCollectionTransaction createDustCollection(String... assets) {
    long time = System.currentTimeMillis();
    Transaction t = new Transaction(time);
    if (assets.length % 2 != 0) {
      throw new IllegalArgumentException("Each asset must be represented by two strings");
    }
    for (int i = 0; i < assets.length; i += 2) {
      t.append(new RawAccountChange(time, AccountType.SPOT, Operation.SMALL_ASSETS_EXCHANGE_BNB,
          assets[i + 1], new Decimal(assets[i]), ""));
    }
    Transaction dust = t.clarifyTransactionType();
    assertNotNull(dust, "Dust transaction must be correctly interpreted");
    assertInstanceOf(DustCollectionTransaction.class, dust,
        "Dust transaction must be correctly interpreted");
    return (DustCollectionTransaction) dust;
  }


  /**
   * Create wallet with specified assets.
   *
   * @param assets Each asset is defined by three strings: the asset, amount in the wallet,
   *               and obtain price.
   * @return The wallet snapshot containing all the necessary assets
   */
  private WalletSnapshot createWallet(String... assets) {
    if (assets.length % 3 != 0) {
      throw new IllegalArgumentException("Each asset must be represented by three strings");
    }
    WalletSnapshot ws = WalletSnapshot.createEmpty();
    for (int i = 0; i < assets.length; i += 3) {
      ws.addAsset(assets[i], new Decimal(assets[i + 1]), new Decimal(assets[i + 2]));
    }
    return ws;
  }

  /**
   * Expect that after performing dustCollection on the startWallet, the final wallet contains
   * the specified assets.
   *
   * @param startWallet       Starting wallet snapshot, before the dust collection
   * @param dustCollection    The dust collection transaction
   * @param finalWalletAssets The expectation of the final result. All the assets in the wallet
   *                          must be specified here. Each asset is specified as three decimal
   *                          strings: the asset, the expected amount, the expected average
   *                          obtain price.
   */
  private void expectDustResult(WalletSnapshot startWallet,
                                DustCollectionTransaction dustCollection,
                                String... finalWalletAssets) {
    if (finalWalletAssets.length % 3 != 0) {
      throw new IllegalArgumentException("Each of final assets must be specified as three strings");
    }
    WalletSnapshot finalSnapshot = dustCollection.process(startWallet, null);
    int expectedAssetCount = finalWalletAssets.length / 3;
    Wallet wallet = finalSnapshot.getWallet();
    assertEquals(expectedAssetCount, wallet.getAssetCount(), "Wrong asset count");
    for (int i = 0; i < finalWalletAssets.length; i += 3) {
      String asset = finalWalletAssets[i];
      Decimal expectedAmount = new Decimal(finalWalletAssets[i + 1]);
      Decimal expectedAvgObtainPrice = new Decimal(finalWalletAssets[i + 2]);
      assertEquals(expectedAmount, wallet.getAssetAmount(asset), asset + " amount incorrect");
      assertEquals(expectedAvgObtainPrice, wallet.getAvgObtainPrice(asset),
          asset + " AVG price incorrect");
    }
  }

  /**
   * Expect the given merged asset changes in the dust transaction.
   *
   * @param dust    The transaction to check
   * @param changes The expected changes. Each change is specified as two strings: amount and asset
   */
  private void expectDustChangeAmounts(DustCollectionTransaction dust, String... changes) {
    for (int i = 0; i < changes.length; i += 2) {
      assertEquals(new Decimal(changes[i]), dust.getDustChangeAmount(changes[i + 1]),
          "Incorrect change amount for " + changes[i + 1]);
    }
  }

}
