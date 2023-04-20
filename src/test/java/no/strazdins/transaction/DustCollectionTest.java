package no.strazdins.transaction;

import static org.junit.jupiter.api.Assertions.*;

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
    Transaction dust = t.clarifyTransactionType();
    assertNotNull(dust, "Dust transaction must be correctly interpreted");
    assertInstanceOf(DustCollectionTransaction.class, dust,
        "Dust transaction must be correctly interpreted");
    assertEquals("Dust collection", dust.getType());
    assertEquals(time, dust.getUtcTime());
    assertEquals(new Decimal("0.1"), dust.getQuoteAmount());
    assertEquals("BNB", dust.getQuoteCurrency());
    assertEquals("SXP", dust.getBaseCurrency());
    assertEquals(new Decimal("-8"), dust.getBaseCurrencyAmount());
    assertEquals(Decimal.ZERO, dust.getFee());
    assertEquals("", dust.getFeeCurrency());
    assertNull(dust.getNecessaryExtraInfo());
  }

  // TODO - one asset to BNB, asset has obtainPrice > 0, no previous BNB
  // TODO - one asset to BNB, asset has obtainPrice = 0, no previous BNB
  // TODO - one asset to BNB, asset has obtainPrice > 0, have BNB with obtain price > 0
  // TODO - one asset to BNB, asset has obtainPrice = 0, have BNB with obtain price > 0
  // TODO - multiple assets to multi BNB, assets have obtainPrice > 0, BNB has obtain price = 0
  // TODO - multiple assets to multi BNB, assets have obtainPrice = 0, BNB has obtain price = 0
  // TODO - multiple assets to multi BNB, assets have obtainPrice > 0, BNB has obtain price > 0
  // TODO - multiple assets to multi BNB, assets have obtainPrice = 0, BNB has obtain price > 0
  @Test
  void testOneDust() {
    expectDustResult(
        createWallet("REN", "4", "0"),
        createDustCollection("0.1", "BNB", "-3.8", "REN"),
        "REN", "0.2", "0", "BNB", "0.1", "0"
    );
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
}
