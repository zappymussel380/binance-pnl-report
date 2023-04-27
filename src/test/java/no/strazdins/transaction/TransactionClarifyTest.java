package no.strazdins.transaction;

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
    setupOperations(Operation.BUY, Operation.SELL, Operation.FEE);
    setupAmounts("12.52", "-0.09", "-0.00025");
    setupAssets("USDT", "LTC", "BNB");
    expectResult(SellTransaction.class, "-0.09", "LTC", "12.52", "USDT", "-0.00025", "BNB");
  }

  @Test
  void testSellWithTransactionRelated() {
    setupOperations(Operation.BUY, Operation.TRANSACTION_RELATED, Operation.FEE);
    setupAmounts("12.52", "-0.09", "-0.00025");
    setupAssets("USDT", "LTC", "BNB");
    expectResult(SellTransaction.class, "-0.09", "LTC", "12.52", "USDT", "-0.00025", "BNB");
  }


  @Test
  void testBuyWithSell() {
    setupOperations(Operation.SELL, Operation.BUY, Operation.FEE);
    setupAmounts("-12.52", "0.1", "-0.00025");
    setupAssets("USDT", "LTC", "BNB");
    expectResult(BuyTransaction.class, "0.1", "LTC", "-12.52", "USDT", "-0.00025", "BNB");
  }

  @Test
  void testBuyWithTransactionRelated() {
    setupOperations(Operation.TRANSACTION_RELATED, Operation.BUY, Operation.FEE);
    setupAmounts("-12.52", "0.1", "-0.00025");
    setupAssets("USDT", "LTC", "BNB");
    expectResult(BuyTransaction.class, "0.1", "LTC", "-12.52", "USDT", "-0.00025", "BNB");
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
