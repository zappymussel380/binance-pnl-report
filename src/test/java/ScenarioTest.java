import no.strazdins.data.*;
import no.strazdins.transaction.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test realistic scenario(s) - list of transactions.
 */
class ScenarioTest {
  private static long transactionTime = System.currentTimeMillis();

  @Test
  void testScenario() {
    WalletSnapshot ws1 = WalletSnapshot.createEmpty();
    WalletSnapshot ws2 = processDeposit(ws1, "LTC", "1.65167383", "72.22");
    expectWalletState(ws2, 1, "1.65167383", "72.22",
        "0", "0", "0");

    WalletSnapshot ws3 = processSell(ws2, "LTC", "0.65167000",
        "47.115741", "0.04711574", "USDT");
    expectWalletState(ws3, 2, "1.00000383", "72.22",
        "47.06862526","0.00501786", "0.00501786");

    WalletSnapshot ws4 = processSell(ws3, "LTC", "1.00000000",
        "72.30000000", "0.07230000", "USDT");
    expectWalletState(ws4, 2, "0.00000383", "72.22",
        "119.29632526", "0.00770000", "0.01271786");

    WalletSnapshot ws5 = processBuy(ws4, "BNB", "1.00000000",
        "20.28760000", "USDT", "0.00075000", "BNB");
    expectWalletState(ws5, 3, "0.00000383", "72.22",
        "99.00872526", "0", "0.01271786");
    expectAssetAmount(ws5, "BNB", "0.99925", "20.30282712");
  }

  private WalletSnapshot processDeposit(WalletSnapshot startSnapshot,
                                        String asset, String amount, String obtainPrice) {
    transactionTime += 1000;
    Transaction t = new Transaction(transactionTime);
    t.append(new RawAccountChange(transactionTime, AccountType.SPOT, Operation.DEPOSIT,
        asset, new Decimal(amount), "Deposit"));
    ExtraInfoEntry ei = new ExtraInfoEntry(transactionTime, ExtraInfoType.ASSET_PRICE, obtainPrice);
    DepositTransaction deposit = new DepositTransaction(t);
    return deposit.process(startSnapshot, ei);
  }

  private WalletSnapshot processBuy(WalletSnapshot startSnapshot, String asset, String amount,
                                    String usedQuote, String quoteCurrency, String fee,
                                    String feeCurrency) {
    transactionTime += 1000;
    Transaction t = new Transaction(transactionTime);
    t.append(new RawAccountChange(transactionTime, AccountType.SPOT,
        Operation.BUY, asset, new Decimal(amount), "Buy coin"));
    t.append(new RawAccountChange(transactionTime, AccountType.SPOT,
        Operation.TRANSACTION_RELATED, quoteCurrency, new Decimal(usedQuote).negate(),
        "Sell " + quoteCurrency));
    t.append(new RawAccountChange(transactionTime, AccountType.SPOT,
        Operation.FEE, feeCurrency, new Decimal(fee).negate(), "Fee in " + feeCurrency));
    BuyTransaction buy = new BuyTransaction(t);
    return buy.process(startSnapshot, null);

  }

  private WalletSnapshot processSell(WalletSnapshot startSnapshot, String asset, String amount,
                                     String obtainedUsdtAmount, String fee, String feeCurrency) {
    transactionTime += 1000;
    Transaction t = new Transaction(transactionTime);
    t.append(new RawAccountChange(transactionTime, AccountType.SPOT,
        Operation.TRANSACTION_RELATED, asset, new Decimal(amount).negate(), "Sell coin"));
    t.append(new RawAccountChange(transactionTime, AccountType.SPOT,
        Operation.BUY, "USDT", new Decimal(obtainedUsdtAmount), "Acquire USDT"));
    t.append(new RawAccountChange(transactionTime, AccountType.SPOT,
        Operation.FEE, feeCurrency, new Decimal(fee).negate(), "Fee in " + feeCurrency));
    SellTransaction sell = new SellTransaction(t);
    return sell.process(startSnapshot, null);
  }

  private void expectWalletState(WalletSnapshot ws, int assetCount,
                                 String ltcAmount, String ltcObtainPrice, String usdtAmount,
                                 String transactionPnl, String runningPnl) {
    assertEquals(assetCount, ws.getWallet().getAssetCount());
    expectAssetAmount(ws, "LTC", ltcAmount, ltcObtainPrice);
    expectAssetAmount(ws, "USDT", usdtAmount, "1.0");
    expectPnl(ws, transactionPnl, runningPnl);
  }

  private void expectAssetAmount(WalletSnapshot ws, String asset, String amount,
                                 String obtainPrice) {
    Decimal expectedAmount = new Decimal(amount);
    assertEquals(new Decimal(amount), ws.getWallet().getAssetAmount(asset));
    if (expectedAmount.isPositive()) {
      assertEquals(new Decimal(obtainPrice), ws.getWallet().getAvgObtainPrice(asset));
    }
  }

  private void expectPnl(WalletSnapshot ws, String transactionPnl, String runningPnl) {
    assertEquals(new Decimal(transactionPnl), ws.getTransaction().getPnl());
    assertEquals(new Decimal(runningPnl), ws.getPnl());
  }
}
