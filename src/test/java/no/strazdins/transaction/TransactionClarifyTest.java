package no.strazdins.transaction;

import static no.strazdins.data.Operation.BUY;
import static no.strazdins.data.Operation.FEE;
import static no.strazdins.data.Operation.SELL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import no.strazdins.data.AccountType;
import no.strazdins.data.Decimal;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionClarifyTest {
  private List<String> assets;
  private List<String> amounts;
  private List<Operation> operations;

  @BeforeEach
  void setup() {
    assets = new ArrayList<>();
    amounts = new ArrayList<>();
    operations = new ArrayList<>();
  }

  @Test
  void testSellWithBuy() {
    setupOperations(BUY, SELL, FEE);
    setupAmounts("12.52", "-0.09", "-0.00025");
    setupAssets("USDT", "LTC", "BNB");
    expectResult(SellTransaction.class, "-0.09", "LTC", "12.52", "USDT", "-0.00025", "BNB");
  }

  @Test
  void testSellWithTransactionRelated() {
    setupOperations(BUY, SELL, FEE);
    setupAmounts("12.52", "-0.09", "-0.00025");
    setupAssets("USDT", "LTC", "BNB");
    expectResult(SellTransaction.class, "-0.09", "LTC", "12.52", "USDT", "-0.00025", "BNB");
  }


  @Test
  void testBuyWithSell() {
    setupOperations(SELL, BUY, FEE);
    setupAmounts("-12.52", "0.1", "-0.00025");
    setupAssets("USDT", "LTC", "BNB");
    expectResult(BuyTransaction.class, "0.1", "LTC", "-12.52", "USDT", "-0.00025", "BNB");
  }

  @Test
  void testBuyWithTransactionRelated() {
    setupOperations(SELL, BUY, FEE);
    setupAmounts("-12.52", "0.1", "-0.00025");
    setupAssets("USDT", "LTC", "BNB");
    expectResult(BuyTransaction.class, "0.1", "LTC", "-12.52", "USDT", "-0.00025", "BNB");
  }

  @Test
  void testSellWithoutFee() {
    setupOperations(SELL, BUY);
    setupAmounts("-7.5", "15");
    setupAssets("BAKE", "USDT");
    expectResult(SellTransaction.class, "-7.5", "BAKE", "15", "USDT", "0", "");
  }

  @Test
  void testCoinToCoinWithoutFee() {
    setupOperations(SELL, BUY);
    setupAmounts("-7.5", "15");
    setupAssets("BAKE", "BUSD");
    expectResult(CoinToCoinTransaction.class, "15", "BUSD", "-7.5", "BAKE", "0", "");
  }

  @Test
  void testSellWithSoldAndRevenueAndFee() {
    setupOperations(SELL, BUY, FEE);
    setupAmounts("-3000", "318", "-0.0008");
    setupAssets("ARDR", "USDT", "BNB");
    expectResult(SellTransaction.class, "-3000", "ARDR", "318", "USDT", "-0.0008", "BNB");
  }

  @Test
  void testBuyBusd() {
    setupOperations(SELL, BUY);
    setupAmounts("-100", "100");
    setupAssets("USD", "BUSD");
    expectResult(CoinToCoinTransaction.class, "100", "BUSD", "-100", "USD", "0", "");
  }

  @Test
  void testBuyWithSpend() {
    setupOperations(SELL, BUY);
    setupAmounts("-300", "3000");
    setupAssets("USDT", "ARDR");
    expectResult(BuyTransaction.class, "3000", "ARDR", "-300", "USDT", "0", "");
  }

  @Test
  void testSlpMultiBuy() {
    setupOperations(
        FEE, FEE, SELL,
        BUY, SELL, FEE,
        BUY, FEE, BUY,
        BUY, SELL, BUY,
        SELL, SELL, FEE
    );
    setupAssets(
        "BNB", "BNB", "USDT",
        "SLP", "USDT", "BNB",
        "SLP", "BNB", "SLP",
        "SLP", "USDT", "SLP",
        "USDT", "USDT", "BNB"
    );
    setupAmounts(
        "-0.00012394", "-0.00009588", "-49.5656",
        "40", "-38.3432", "-0.00002805",
        "164", "-0.00002338", "172",
        "48", "-11.2224", "212",
        "-40.2136", "-9.352", "-0.00010056"
    );
    expectResult(BuyTransaction.class, "636", "SLP", "-148.6968", "USDT", "-0.00037181", "BNB");
  }


  private void setupOperations(Operation... operations) {
    this.operations.addAll(Arrays.asList(operations));
  }

  private void setupAmounts(String... amounts) {
    this.amounts.addAll(Arrays.asList(amounts));
  }

  private void setupAssets(String... assets) {
    this.assets.addAll(Arrays.asList(assets));
  }

  public <T extends Transaction> void expectResult(Class<T> transactionClass,
                                                   String baseAmount, String baseAsset,
                                                   String quoteAmount, String quoteAsset,
                                                   String feeAmount, String feeAsset) {
    Transaction t = createClarifiedTransaction();
    assertNotNull(t);
    assertInstanceOf(transactionClass, t);
    expectTransactionDetails(t, baseAmount, baseAsset, quoteAmount, quoteAsset,
        feeAmount, feeAsset);
  }

  private Transaction createClarifiedTransaction() {
    assertEquals(operations.size(), amounts.size(), "Operation and amount count must match");
    assertEquals(operations.size(), assets.size(), "Operation and asset count must match");
    long time = System.currentTimeMillis();
    Transaction t = new Transaction(time);
    for (int i = 0; i < operations.size(); ++i) {
      t.append(new RawAccountChange(time, AccountType.SPOT, operations.get(i), assets.get(i),
          new Decimal(amounts.get(i)), ""));
    }
    return t.clarifyTransactionType();
  }

  private static void expectTransactionDetails(Transaction t, String baseAmount, String baseAsset,
                                               String quoteAmount, String quoteAsset,
                                               String feeAmount, String feeAsset) {
    assertEquals(baseAsset, t.getBaseCurrency());
    assertEquals(quoteAsset, t.getQuoteCurrency());
    assertEquals(new Decimal(baseAmount), t.getBaseCurrencyAmount());
    assertEquals(new Decimal(quoteAmount), t.getQuoteAmount());
    assertEquals(new Decimal(feeAmount), t.getFee());
    assertEquals(feeAsset, t.getFeeCurrency());
  }
}
