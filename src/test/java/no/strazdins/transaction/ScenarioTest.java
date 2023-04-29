package no.strazdins.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import no.strazdins.data.AccountType;
import no.strazdins.data.Decimal;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.ExtraInfoType;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.WalletSnapshot;
import org.junit.jupiter.api.Test;

/**
 * Test realistic scenario(s) - list of transactions.
 */
class ScenarioTest {
  private static long transactionTime = System.currentTimeMillis();

  @Test
  void testRealisticScenario() {
    WalletSnapshot ws1 = WalletSnapshot.createEmpty();
    WalletSnapshot ws2 = processDeposit(ws1, "LTC", "1.65167383", "72.22");
    expectWalletState(ws2, "0", "0", "1.65167383", "LTC", "72.22");

    WalletSnapshot ws3 = processSell(ws2, "LTC", "0.65167000",
        "47.115741", "0.04711574", "USDT");
    expectWalletState(ws3, "0.00501786", "0.00501786",
        "1.00000383", "LTC", "72.22",
        "47.06862526", "USDT", "1"
    );

    WalletSnapshot ws4 = processSell(ws3, "LTC", "1.00000000",
        "72.30000000", "0.07230000", "USDT");
    expectWalletState(ws4, "0.00770000", "0.01271786",
        "0.00000383", "LTC", "72.22",
        "119.29632526", "USDT", "1"
    );

    // Buy BNB, part of the obtained BNB is used as a fee
    WalletSnapshot ws5 = processBuy(ws4, "BNB", "1.00000000",
        "20.28760000", "USDT", "0.00075000", "BNB");
    expectWalletState(ws5, "0", "0.01271786",
        "0.00000383", "LTC", "72.22",
        "99.00872526", "USDT", "1",
        "0.99925", "BNB", "20.30282712"
    );

    // Simulate another buy, where BTC is bought, fee is paid in USDT
    WalletSnapshot ws51 = processBuy(ws4, "BTC", "0.001",
        "20", "USDT", "0.2", "USDT");
    expectWalletState(ws51, "0", "0.01271786",
        "0.00000383", "LTC", "72.22",
        "99.09632526", "USDT", "1",
        "0.001", "BTC", "20200"
    );

    // Simulate another buy, where BTC is bought, fee paid in BNB (BNB exists in the wallet)
    WalletSnapshot ws6 = processBuy(ws5, "BTC", "0.00799300",
        "79.85846265", "USDT", "0.00295735", "BNB");
    expectWalletState(ws6, "0", "0.01271786",
        "0.00000383", "LTC", "72.22",
        "19.15026261", "USDT", "1",
        "0.00799300", "BTC", "9998.56189416",
        "0.99629265", "BNB", "20.30282712"
    );

    // Simulate a sell - sell the whole BTC, no BTC left in the wallet
    WalletSnapshot ws7 = processSell(ws6, "BTC", "0.00799300",
        "79.75751106", "0.00296260", "BNB");
    expectWalletState(ws7, "-0.22114332", "-0.20842546",
        "0.00000383", "LTC", "72.22",
        "98.90777367", "USDT", "1",
        "0.99333005", "BNB", "20.30282712"
    );

    WalletSnapshot ws8 = processDeposit(ws7, "LTC", "11.98728478", "72.49245909");
    expectWalletState(ws8, "0", "-0.20842546",
        "11.98728861", "LTC", "72.49245900",
        "98.90777367", "USDT", "1",
        "0.99333005", "BNB", "20.30282712"
    );

    WalletSnapshot ws9 = processWithdraw(ws8, "LTC", "2", "82.49245900");
    expectWalletState(ws9, "20", "19.79157454",
        "9.98728861", "LTC", "72.49245900",
        "98.90777367", "USDT", "1",
        "0.99333005", "BNB", "20.30282712"
    );
  }

  @Test
  void testBtcToEur() {
    WalletSnapshot ws1 = WalletSnapshot.createEmpty();
    ws1.addAsset("BNB", new Decimal("10"), new Decimal("200"));
    ws1.addAsset("BTC", new Decimal("0.01"), new Decimal("20000"));
    CoinToCoinContext t = new CoinToCoinContext(ws1)
        .sell("BTC", "-0.008", "-0.002")
        .buy("EUR", "148", "300")
        .fees("BNB", "-0.04", "-0.08");
    WalletSnapshot ws2 = t.process();

    // 224 USD used to get 448 EUR -> avg price = 0.5
    expectWalletState(ws2, "0", "0",
        "9.88", "BNB", "200",
        "448", "EUR", "0.5"
    );
  }

  @Test
  void testCoinToCoin() {
    WalletSnapshot ws1 = WalletSnapshot.createEmpty();
    ws1.addAsset("BNB", new Decimal("10"), new Decimal("200"));
    ws1.addAsset("BTC", new Decimal("0.03"), new Decimal("20000"));
    ws1.addAsset("LTC", new Decimal("1"), new Decimal("190"));

    CoinToCoinContext t = new CoinToCoinContext(ws1)
        .sell("LTC", "-1")
        .buy("BTC", "0.02")
        .fees("BNB", "-0.05");
    WalletSnapshot ws2 = t.process();
    // LTC bought at price 190, get 0.02 BTC when selling
    // $10 used in BNB fee. In total: $200 used to get the 0.02 BTC
    // => obtain price of the new BTC is $10000. Avg obtain price of the whole BTC: $16000

    expectWalletState(ws2, "0", "0",
        "9.95", "BNB", "200",
        "0.05", "BTC", "16000"
    );
  }

  @Test
  void testCoinToCoinWithoutFee() {
    WalletSnapshot ws1 = WalletSnapshot.createEmpty();
    ws1.addAsset("BAKE", new Decimal("8"), new Decimal("10"));
    CoinToCoinContext t = new CoinToCoinContext(ws1)
        .sell("BAKE", "-7.5")
        .buy("BUSD", "75");
    WalletSnapshot ws2 = t.process();
    expectWalletState(ws2, "0", "0", "0.5", "BAKE", "10", "75", "BUSD", "1");
  }

  @Test
  void testSellWithoutFee() {
    WalletSnapshot ws1 = WalletSnapshot.createEmpty();
    ws1.addAsset("BAKE", new Decimal("8"), new Decimal("10"));
    WalletSnapshot ws2 = processSell(ws1, "BAKE", "7", "80", null, null);
    expectWalletState(ws2, "10", "10", "80", "USDT", "1", "1", "BAKE", "10");
  }

  @Test
  void testSellWithFee() {
    WalletSnapshot ws1 = WalletSnapshot.createEmpty();
    ws1.addAsset("BAKE", new Decimal("8"), new Decimal("10"));
    ws1.addAsset("BNB", new Decimal("5"), new Decimal("100"));
    WalletSnapshot ws2 = processSell(ws1, "BAKE", "7", "80", "0.2", "BNB");
    expectWalletState(ws2, "-10", "-10",
        "80", "USDT", "1", "1", "BAKE", "10", "4.8", "BNB", "100");
  }

  @Test
  void testBuyWithoutFee() {
    WalletSnapshot ws1 = WalletSnapshot.createEmpty();
    ws1.addAsset("USDT", new Decimal("80"), new Decimal("1"));
    WalletSnapshot ws2 = processBuy(ws1, "BAKE", "7.5", "75", "USDT", null, null);
    expectWalletState(ws2, "0", "0", "5", "USDT", "1", "7.5", "BAKE", "10");
  }

  /**
   * At some point Binance distributed TWT and then took it away again. Test this scenario.
   */
  @Test
  void testNegativeDistribution() {
    WalletSnapshot ws1 = WalletSnapshot.createEmpty();
    WalletSnapshot ws2 = processDistribution(ws1, "100", "TWT");
    expectWalletState(ws2, "0", "0", "100", "TWT", "0");
    WalletSnapshot ws3 = processDistribution(ws2, "-100", "TWT");
    expectWalletState(ws3, "0", "0");
  }

  private WalletSnapshot processDeposit(WalletSnapshot startSnapshot,
                                        String asset, String amount, String obtainPrice) {
    transactionTime += 1000;
    Transaction t = new Transaction(transactionTime);
    t.append(new RawAccountChange(transactionTime, AccountType.SPOT, Operation.DEPOSIT,
        asset, new Decimal(amount), "Deposit"));
    ExtraInfoEntry ei = new ExtraInfoEntry(transactionTime, ExtraInfoType.ASSET_PRICE,
        asset, obtainPrice);
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
        asset, realizationPrice);
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
    if (fee != null) {
      t.append(new RawAccountChange(transactionTime, AccountType.SPOT,
          Operation.FEE, feeCurrency, new Decimal(fee).negate(), "Fee in " + feeCurrency));
    }
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
    if (fee != null) {
      t.append(new RawAccountChange(transactionTime, AccountType.SPOT,
          Operation.FEE, feeCurrency, new Decimal(fee).negate(), "Fee in " + feeCurrency));
    }
    Transaction sell = t.clarifyTransactionType();
    assertInstanceOf(SellTransaction.class, sell);
    return sell.process(startSnapshot, null);
  }

  private WalletSnapshot processDistribution(WalletSnapshot startSnapshot,
                                             String amount, String asset) {
    transactionTime += 1000;
    Transaction t = new Transaction(transactionTime);
    t.append(new RawAccountChange(transactionTime, AccountType.SPOT, Operation.DISTRIBUTION, asset,
        new Decimal(amount), "Distribute " + amount + " " + asset));
    Transaction distribute = t.clarifyTransactionType();
    assertInstanceOf(DistributionTransaction.class, distribute);
    return distribute.process(startSnapshot, null);
  }

  private void expectWalletState(WalletSnapshot ws, String transactionPnl, String runningPnl,
                                 String... assets) {
    assertEquals(0, assets.length % 3,
        "Assets must be specified with triplets (amount, asset, obtainPrice)");
    int expectedAssetCount = assets.length / 3;
    assertEquals(expectedAssetCount, ws.getWallet().getAssetCount());
    for (int i = 0; i < assets.length; i += 3) {
      String amount = assets[i];
      String asset = assets[i + 1];
      String obtainPrice = assets[i + 2];
      expectAssetAmount(ws, asset, amount, obtainPrice);
    }
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
