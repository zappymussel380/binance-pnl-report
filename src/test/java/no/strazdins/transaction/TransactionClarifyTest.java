package no.strazdins.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import no.strazdins.data.AccountType;
import no.strazdins.data.Decimal;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import org.junit.jupiter.api.Test;

class TransactionClarifyTest {
  @Test
  void testSell() {
    expectTransactionClarification(
        Operation.BUY, "12.52", "USDT",
        Operation.SELL, "-0.09", "LTC",
        Operation.FEE, "-0.00025", "BNB",
        SellTransaction.class, "-0.09", "LTC", "12.52", "USDT", "-0.00025", "BNB"
    );
    expectTransactionClarification(
        Operation.BUY, "12.52", "USDT",
        Operation.TRANSACTION_RELATED, "-0.09", "LTC",
        Operation.FEE, "-0.00025", "BNB",
        SellTransaction.class, "-0.09", "LTC", "12.52", "USDT", "-0.00025", "BNB"
    );
  }

  @Test
  void testBuy() {
    expectTransactionClarification(
        Operation.SELL, "-12.52", "USDT",
        Operation.BUY, "0.1", "LTC",
        Operation.FEE, "-0.00025", "BNB",
        BuyTransaction.class, "0.1", "LTC", "-12.52", "USDT", "-0.00025", "BNB"
    );
    expectTransactionClarification(
        Operation.TRANSACTION_RELATED, "-12.52", "USDT",
        Operation.BUY, "0.1", "LTC",
        Operation.FEE, "-0.00025", "BNB",
        BuyTransaction.class, "0.1", "LTC", "-12.52", "USDT", "-0.00025", "BNB"
    );
  }

  private void expectTransactionClarification(Operation op1, String amount1, String asset1,
                                              Operation op2, String amount2, String asset2,
                                              Operation op3, String amount3, String asset3,
                                              Class expectedClass,
                                              String baseAmount, String baseAsset,
                                              String quoteAmount, String quoteAsset,
                                              String feeAmount, String feeAsset) {
    Transaction t = createClarifiedTransaction(
        op1, amount1, asset1,
        op2, amount2, asset2,
        op3, amount3, asset3
    );
    assertNotNull(t);
    assertInstanceOf(expectedClass, t);
    expectTransactionDetails(t, baseAmount, baseAsset, quoteAmount, quoteAsset,
        feeAmount, feeAsset);
  }

  private void expectTransactionDetails(Transaction t, String baseAmount, String baseAsset,
                                        String quoteAmount, String quoteAsset,
                                        String feeAmount, String feeAsset) {
    assertEquals(baseAsset, t.getBaseCurrency());
    assertEquals(quoteAsset, t.getQuoteCurrency());
    assertEquals(new Decimal(baseAmount), t.getBaseCurrencyAmount());
    assertEquals(new Decimal(quoteAmount), t.getQuoteAmount());
    assertEquals(new Decimal(feeAmount), t.getFee());
    assertEquals(feeAsset, t.getFeeCurrency());
  }

  private Transaction createClarifiedTransaction(Operation op1, String amount1, String asset1,
                                                 Operation op2, String amount2, String asset2,
                                                 Operation op3, String amount3, String asset3) {
    long time = System.currentTimeMillis();
    Transaction t = new Transaction(time);
    t.append(new RawAccountChange(time, AccountType.SPOT, op1, asset1,
        new Decimal(amount1), "First change"));
    t.append(new RawAccountChange(time, AccountType.SPOT, op2, asset2,
        new Decimal(amount2), "Second change"));
    t.append(new RawAccountChange(time, AccountType.SPOT, op3, asset3,
        new Decimal(amount3), "Third change"));
    return t.clarifyTransactionType();
  }
}
