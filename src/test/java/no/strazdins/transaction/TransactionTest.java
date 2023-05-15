package no.strazdins.transaction;

import static no.strazdins.data.Operation.BUY;
import static no.strazdins.data.Operation.FEE;
import static no.strazdins.data.Operation.SELL;
import static no.strazdins.testtools.TestTools.createDeposit;
import static no.strazdins.testtools.TestTools.createWithdrawal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import no.strazdins.data.AccountType;
import no.strazdins.data.Decimal;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.ExtraInfoType;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.WalletDiff;
import org.junit.jupiter.api.Test;

class TransactionTest {
  @Test
  void testClarifyTransactionType() {
    long time = System.currentTimeMillis();
    Transaction t = new Transaction(time);
    assertNull(t.clarifyTransactionType());

    t.append(new RawAccountChange(time, AccountType.SPOT, Operation.DEPOSIT, "BTC",
        Decimal.ONE, ""));

    assertInstanceOf(DepositTransaction.class, t.clarifyTransactionType());
  }

  @Test
  void testMerge() {
    long thisTime = System.currentTimeMillis();
    Transaction t = new Transaction(thisTime);
    appendOperation(t, FEE, "-1", "BNB");
    appendOperation(t, BUY, "0.1", "BTC");
    appendOperation(t, SELL, "-2000", "USDT");
    appendOperation(t, BUY, "0.2", "BTC");
    appendOperation(t, SELL, "-6000", "USDT");
    appendOperation(t, SELL, "-4000", "USDT");
    appendOperation(t, BUY, "0.3", "BTC");
    appendOperation(t, FEE, "-2", "BNB");
    appendOperation(t, FEE, "-3", "BNB");
    Transaction merged = t.clarifyTransactionType();
    assertNotNull(merged);
    assertInstanceOf(BuyTransaction.class, merged);

    assertEquals("BTC", merged.getBaseCurrency());
    assertEquals("USDT", merged.getQuoteCurrency());
    assertEquals("BNB", merged.getFeeCurrency());
    assertEquals(new Decimal("0.6"), merged.getBaseCurrencyAmount());
    assertEquals(new Decimal("-12000"), merged.getQuoteAmount());
    assertEquals(new Decimal("-6"), merged.getFee());
  }

  @Test
  void testOperationDiff() {
    long thisTime = System.currentTimeMillis();
    Transaction t = new Transaction(thisTime);
    appendOperation(t, FEE, "-1", "BNB");
    appendOperation(t, BUY, "0.1", "BTC");
    appendOperation(t, SELL, "-2000", "USDT");
    appendOperation(t, BUY, "0.2", "BTC");
    appendOperation(t, SELL, "-6000", "USDT");
    appendOperation(t, BUY, "0.3", "BTC");
    WalletDiff diff = t.getOperationDiff();
    WalletDiff expectedDiff = new WalletDiff()
        .add("BNB", new Decimal("-1"))
        .add("BTC", new Decimal("0.6"))
        .add("USDT", new Decimal("-8000"));
    assertEquals(expectedDiff, diff);
  }

  @Test
  void testOperationCount() {
    long thisTime = System.currentTimeMillis();
    Transaction t = new Transaction(thisTime);
    appendOperation(t, FEE, "-1", "BNB");
    appendOperation(t, BUY, "0.1", "BTC");
    appendOperation(t, SELL, "-2000", "USDT");
    appendOperation(t, BUY, "0.2", "BTC");
    appendOperation(t, SELL, "-6000", "USDT");
    appendOperation(t, BUY, "0.3", "BTC");
    assertEquals(6, t.getTotalOperationCount());
  }

  @Test
  void testDepositExtraInfo() {
    DepositTransaction deposit = createDeposit("2", "LTC");
    ExtraInfoEntry necessaryExtraInfo = deposit.getNecessaryExtraInfo();
    assertNotNull(necessaryExtraInfo);
    assertEquals("LTC", necessaryExtraInfo.asset());
    assertEquals(deposit.getUtcTime(), necessaryExtraInfo.utcTimestamp());
    assertEquals(ExtraInfoType.ASSET_PRICE, necessaryExtraInfo.type());
  }

  @Test
  void testWithdrawalExtraInfo() {
    WithdrawTransaction withdraw = createWithdrawal("-2", "LTC");
    ExtraInfoEntry necessaryExtraInfo = withdraw.getNecessaryExtraInfo();
    assertNotNull(necessaryExtraInfo);
    assertEquals("LTC", necessaryExtraInfo.asset());
    assertEquals(withdraw.getUtcTime(), necessaryExtraInfo.utcTimestamp());
    assertEquals(ExtraInfoType.ASSET_PRICE, necessaryExtraInfo.type());
  }

  @Test
  void testUsdDepositDoesNotRequireExtraInfo() {
    DepositTransaction usdDeposit = createDeposit("2000", "USD");
    assertNull(usdDeposit.getNecessaryExtraInfo());
    usdDeposit = createDeposit("2000", "USDT");
    assertNull(usdDeposit.getNecessaryExtraInfo());
    usdDeposit = createDeposit("2000", "BUSD");
    assertNull(usdDeposit.getNecessaryExtraInfo());
  }

  @Test
  void testUsdWithdrawalDoesNotRequireExtraInfo() {
    WithdrawTransaction usdWithdrawal = createWithdrawal("-2000", "USD");
    assertNull(usdWithdrawal.getNecessaryExtraInfo());
    usdWithdrawal = createWithdrawal("-2000", "USDT");
    assertNull(usdWithdrawal.getNecessaryExtraInfo());
    usdWithdrawal = createWithdrawal("-2000", "BUSD");
    assertNull(usdWithdrawal.getNecessaryExtraInfo());
  }

  private void appendOperation(Transaction t, Operation operation, String amount, String asset) {
    t.append(new RawAccountChange(t.utcTime, AccountType.SPOT, operation, asset,
        new Decimal(amount), ""));
  }
}
