import no.strazdins.data.*;
import no.strazdins.transaction.DepositTransaction;
import no.strazdins.transaction.SellTransaction;
import no.strazdins.transaction.Transaction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SellTest {
  @Test
  void testSell() {
    long currentTime = System.currentTimeMillis();
    Transaction t1 = new Transaction(currentTime);
    t1.append(new RawAccountChange(currentTime, AccountType.SPOT, Operation.DEPOSIT, "LTC",
        new Decimal("1.65167383"), "Deposit"));
    ExtraInfoEntry ei1 = new ExtraInfoEntry(currentTime, ExtraInfoType.ASSET_PRICE, "72.22");
    WalletSnapshot ws1 = WalletSnapshot.createEmpty();
    DepositTransaction deposit1 = new DepositTransaction(t1);
    WalletSnapshot ws2 = deposit1.process(ws1, ei1);

    Transaction t2 = new Transaction(currentTime + 1000);
    t2.append(new RawAccountChange(currentTime + 1000, AccountType.SPOT,
        Operation.TRANSACTION_RELATED, "LTC", new Decimal("-0.65167000"), "Sell LTC"));
    t2.append(new RawAccountChange(currentTime + 1000, AccountType.SPOT,
        Operation.BUY, "USDT", new Decimal("47.115741"), "Acquire USDT"));
    t2.append(new RawAccountChange(currentTime + 1000, AccountType.SPOT,
        Operation.FEE, "USDT", new Decimal("-0.04711574"), "Fee in USDT"));
    SellTransaction sell1 = new SellTransaction(t2);
    WalletSnapshot ws3 = sell1.process(ws2, null);

    assertEquals(new Decimal("0.00501786"), sell1.getPnl());
    assertEquals(new Decimal("0.00501786"), ws3.getPnl());
    assertEquals(2, ws3.getWallet().getAssetCount());
    assertEquals(new Decimal("1.00000383"), ws3.getWallet().getAssetAmount("LTC"));
    assertEquals(new Decimal("72.22"), ws3.getWallet().getAvgObtainPrice("LTC"));
    assertEquals(new Decimal("47.06862526"), ws3.getWallet().getAssetAmount("USDT"));
    assertEquals(Decimal.ONE, ws3.getWallet().getAvgObtainPrice("USDT"));

    Transaction t3 = new Transaction(currentTime + 1000);
    t3.append(new RawAccountChange(currentTime + 1000, AccountType.SPOT,
        Operation.TRANSACTION_RELATED, "LTC", new Decimal("-1.00000000"), "Sell LTC"));
    t3.append(new RawAccountChange(currentTime + 1000, AccountType.SPOT,
        Operation.BUY, "USDT", new Decimal("72.30000000"), "Acquire USDT"));
    t3.append(new RawAccountChange(currentTime + 1000, AccountType.SPOT,
        Operation.FEE, "USDT", new Decimal("-0.07230000"), "Fee in USDT"));
    SellTransaction sell2 = new SellTransaction(t3);
    WalletSnapshot ws4 = sell2.process(ws3, null);

    assertEquals(new Decimal("0.00770000"), sell2.getPnl());
    assertEquals(sell1.getPnl().add("0.0077"), ws4.getPnl());
    assertEquals(2, ws4.getWallet().getAssetCount());
    assertEquals(new Decimal("0.00000383"), ws4.getWallet().getAssetAmount("LTC"));
    assertEquals(new Decimal("72.22"), ws4.getWallet().getAvgObtainPrice("LTC"));
    assertEquals(new Decimal("119.29632526"), ws4.getWallet().getAssetAmount("USDT"));
    assertEquals(Decimal.ONE, ws4.getWallet().getAvgObtainPrice("USDT"));
  }
}
