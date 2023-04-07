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

    // Buy BNB, part of the obtained BNB is used as a fee
    WalletSnapshot ws5 = processBuy(ws4, "BNB", "1.00000000",
        "20.28760000", "USDT", "0.00075000", "BNB");
    expectWalletState(ws5, 3, "0.00000383", "72.22",
        "99.00872526", "0", "0.01271786");
    expectAssetAmount(ws5, "BNB", "0.99925", "20.30282712");

    // Simulate another buy, where BTC is bought, fee is paid in USDT
    WalletSnapshot ws5_1 = processBuy(ws4, "BTC", "0.001",
        "20", "USDT", "0.2", "USDT");
    expectWalletState(ws5_1, 3, "0.00000383", "72.22",
        "99.09632526", "0", "0.01271786");
    expectAssetAmount(ws5_1, "BTC", "0.001", "20200");

    // Simulate another buy, where BTC is bought, fee paid in BNB (BNB exists in the wallet)
    WalletSnapshot ws6 = processBuy(ws5, "BTC", "0.00799300",
        "79.85846265", "USDT", "0.00295735", "BNB");
    expectWalletState(ws6, 4, "0.00000383", "72.22",
        "19.15026261", "0", "0.01271786");
    expectAssetAmount(ws6, "BTC", "0.00799300", "9998.56189416");
    expectAssetAmount(ws6, "BNB", "0.99629265", "20.30282712");

    // Simulate a sell - sell the whole BTC, no BTC left in the wallet
    WalletSnapshot ws7 = processSell(ws6, "BTC", "0.00799300",
        "79.75751106", "0.00296260", "BNB");
    expectWalletState(ws7, 3, "0.00000383", "72.22",
        "98.90777367", "-0.22114332", "-0.20842546");
    expectAssetAmount(ws7, "BNB", "0.99333005", "20.30282712");

    WalletSnapshot ws8 = processDeposit(ws7, "LTC", "11.98728478", "72.49245909");
    expectWalletState(ws8, 3, "11.98728861", "72.49245900",
        "98.90777367", "0", "-0.20842546");
    expectAssetAmount(ws8, "BNB", "0.99333005", "20.30282712");

    WalletSnapshot ws9 = processWithdraw(ws8, "LTC", "2", "82.49245900");
    expectWalletState(ws9, 3, "9.98728861", "72.49245900",
        "98.90777367", "20", "19.79157454");
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

  private WalletSnapshot processWithdraw(WalletSnapshot startSnapshot, String asset, String amount,
                                         String realizationPrice) {
    transactionTime += 1000;
    Transaction t = new Transaction(transactionTime);
    t.append(new RawAccountChange(transactionTime, AccountType.SPOT, Operation.WITHDRAW,
        asset, new Decimal(amount).negate(), "Withdraw"));
    ExtraInfoEntry ei = new ExtraInfoEntry(transactionTime, ExtraInfoType.ASSET_PRICE,
        realizationPrice);
    WithdrawTransaction withdraw = new WithdrawTransaction(t);
    return withdraw.process(startSnapshot, ei);
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
