package no.strazdins.transaction;

import static no.strazdins.testtools.TestTools.createChanges;
import static no.strazdins.testtools.TestTools.expectWalletState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import no.strazdins.data.Decimal;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.ExtraInfoType;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.WalletSnapshot;
import org.junit.jupiter.api.Test;

/**
 * Tests for CardPurchaseTransaction class.
 */
class CardPurchaseTransactionTest {
  private static final String BTC_AMOUNT = "0.00093949";
  private static final String EUR_AMOUNT = "14.71";
  private static final String EUR_PRICE = "1.0726";
  private static final String BTC_OBTAIN_PRICE = new Decimal(EUR_AMOUNT)
      .multiply(EUR_PRICE)
      .divide(BTC_AMOUNT).getNiceString();


  @Test
  void testClarification() {
    createCardPurchaseTransactionWithEur();
  }

  @Test
  void testInitialization() {
    CardPurchaseTransaction cardPurchase = createCardPurchaseTransactionWithEur();
    assertEquals("BTC", cardPurchase.getBaseCurrency());
    assertEquals(new Decimal(BTC_AMOUNT), cardPurchase.getBaseCurrencyAmount());
    assertEquals("EUR", cardPurchase.getQuoteCurrency());
    assertEquals(new Decimal(EUR_AMOUNT).negate(), cardPurchase.getQuoteAmount());
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

  @Test
  void testProcess() {
    tesCardPurchaseProcess("0", "0", BTC_AMOUNT, BTC_OBTAIN_PRICE);
  }

  @Test
  void testObtainPriceUpdate() {
    String newAmount = new Decimal(BTC_AMOUNT).add("0.1").getNiceString();
    tesCardPurchaseProcess("0.1", "20000", newAmount, "19970.16178703");
  }

  private void tesCardPurchaseProcess(String initialBtcAmount, String initialObtainPrice,
                                      String expectedFinalBtcAmount,
                                      String expectedAvgBtcObtainPrice) {
    WalletSnapshot startSnapshot = WalletSnapshot.createEmpty();
    Decimal initialAmount = new Decimal(initialBtcAmount);
    if (initialAmount.isPositive()) {
      startSnapshot.addAsset("BTC", initialAmount, new Decimal(initialObtainPrice));
    }
    CardPurchaseTransaction cardPurchase = createCardPurchaseTransactionWithEur();
    ExtraInfoEntry ei = new ExtraInfoEntry(cardPurchase.getUtcTime(), ExtraInfoType.ASSET_PRICE,
        "EUR", EUR_PRICE);
    WalletSnapshot newSnapshot = cardPurchase.process(startSnapshot, ei);
    expectWalletState(newSnapshot, "0", "0", expectedFinalBtcAmount, "BTC",
        expectedAvgBtcObtainPrice);
    assertEquals(new Decimal(BTC_OBTAIN_PRICE), cardPurchase.getAvgPriceInUsdt());
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
        "SPOT", "Buy Crypto", EUR_AMOUNT, quoteAsset,
        "SPOT", "Buy Crypto", "-" + EUR_AMOUNT, quoteAsset,
        "SPOT", "Buy Crypto", BTC_AMOUNT, "BTC"
    );
  }
}
