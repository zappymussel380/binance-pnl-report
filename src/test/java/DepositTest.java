import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import no.strazdins.data.AccountType;
import no.strazdins.data.Decimal;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.ExtraInfoType;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.WalletSnapshot;
import no.strazdins.transaction.DepositTransaction;
import no.strazdins.transaction.Transaction;
import org.junit.jupiter.api.Test;

class DepositTest {

  @Test
  void testDeposit() {
    long currentTime = System.currentTimeMillis();
    Transaction t1 = new Transaction(currentTime);
    t1.append(new RawAccountChange(currentTime, AccountType.SPOT, Operation.DEPOSIT, "LTC",
        new Decimal("1.6516738"), "First deposit"));
    ExtraInfoEntry ei1 = new ExtraInfoEntry(currentTime, ExtraInfoType.ASSET_PRICE,
        "LTC", "650.98");
    WalletSnapshot ws1 = WalletSnapshot.createEmpty();
    Transaction ct1 = t1.clarifyTransactionType();
    assertNotNull(ct1);
    assertInstanceOf(DepositTransaction.class, ct1);

    DepositTransaction deposit1 = (DepositTransaction) ct1;
    WalletSnapshot ws2 = deposit1.process(ws1, ei1);
    assertEquals(Decimal.ZERO, ws2.getPnl());
    assertEquals(1, ws2.getWallet().getAssetCount());
    assertEquals(new Decimal("1.6516738"), ws2.getWallet().getAssetAmount("LTC"));
    assertEquals(new Decimal("650.98"), ws2.getWallet().getAvgObtainPrice("LTC"));
    assertEquals(Decimal.ZERO, ws2.getWallet().getAssetAmount("BTC"));

    Transaction t2 = new Transaction(currentTime + 1000);
    t2.append(new RawAccountChange(currentTime + 1000, AccountType.SPOT, Operation.DEPOSIT, "LTC",
        new Decimal("11.98728478"), "Second deposit"));
    ExtraInfoEntry ei2 = new ExtraInfoEntry(currentTime + 1000,
        ExtraInfoType.ASSET_PRICE, "LTC", "653.78");

    DepositTransaction deposit2 = new DepositTransaction(t2);
    WalletSnapshot ws3 = deposit2.process(ws2, ei2);
    assertEquals(Decimal.ZERO, ws3.getPnl());
    assertEquals(1, ws3.getWallet().getAssetCount());
    assertEquals(new Decimal("13.63895858"), ws3.getWallet().getAssetAmount("LTC"));
    assertEquals(new Decimal("653.44092084"), ws3.getWallet().getAvgObtainPrice("LTC"));
    assertEquals(Decimal.ZERO, ws3.getWallet().getAssetAmount("BTC"));
  }
}
