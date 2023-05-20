package no.strazdins.transaction;

import static no.strazdins.testtools.TestTools.createChanges;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.ExtraInfoType;
import no.strazdins.data.RawAccountChange;
import org.junit.jupiter.api.Test;

/**
 * Tests for CardPurchaseTransaction class.
 */
class CardPurchaseTransactionTest {
  @Test
  void testClarification() {
    createCardPurchaseTransactionWithEur();
  }

  @Test
  void testInitialization() {
    CardPurchaseTransaction cardPurchase = createCardPurchaseTransactionWithEur();

  }

  @Test
  void testNoExtraInfoForUsdPurchase() {
    CardPurchaseTransaction cardPurchase = createCardPurchaseTransactionWithUsd();
    assertNull(cardPurchase.getNecessaryExtraInfo());
  }

  @Test
  void testExpectedExtraInfo() {
    CardPurchaseTransaction cardPurchase = createCardPurchaseTransactionWithEur();
    ExtraInfoEntry ei = cardPurchase.getNecessaryExtraInfo();
    assertNotNull(ei);
    assertEquals(cardPurchase.getUtcTime(), ei.utcTimestamp());
    assertEquals("EUR", ei.asset());
    assertEquals(ExtraInfoType.ASSET_PRICE, ei.type());
  }

  private static CardPurchaseTransaction createCardPurchaseTransactionWithEur() {
    return createCardPurchaseTransaction(false);
  }

  private static CardPurchaseTransaction createCardPurchaseTransactionWithUsd() {
    return createCardPurchaseTransaction(true);
  }

  private static CardPurchaseTransaction createCardPurchaseTransaction(boolean useUsd) {
    String quoteAsset = useUsd ? "USD" : "EUR";
    List<RawAccountChange> changes = createRawChanges(quoteAsset);
    Transaction t = new Transaction(changes.get(0).getUtcTime());
    for (RawAccountChange change : changes) {
      t.append(change);
    }
    Transaction cardPurchase = t.clarifyTransactionType();
    assertInstanceOf(CardPurchaseTransaction.class, cardPurchase);
    return (CardPurchaseTransaction) cardPurchase;
  }


  private static List<RawAccountChange> createRawChanges(String quoteAsset) {
    return createChanges(
        "SPOT", "Buy Crypto", "14.71", quoteAsset,
        "SPOT", "Buy Crypto", "-14.71", quoteAsset,
        "SPOT", "Buy Crypto", "0.00093949", "BTC"
    );
  }
}
