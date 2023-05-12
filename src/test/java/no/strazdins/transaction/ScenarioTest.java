package no.strazdins.transaction;

import static no.strazdins.testtools.TestTools.expectWalletState;
import static no.strazdins.testtools.TestTools.processBuy;
import static no.strazdins.testtools.TestTools.processDeposit;
import static no.strazdins.testtools.TestTools.processDistribution;
import static no.strazdins.testtools.TestTools.processSell;
import static no.strazdins.testtools.TestTools.processWithdraw;
import static org.junit.jupiter.api.Assertions.assertThrows;

import no.strazdins.data.Decimal;
import no.strazdins.data.WalletSnapshot;
import no.strazdins.testtools.CoinToCoinContext;
import org.junit.jupiter.api.Test;

/**
 * Test realistic scenario(s) - list of transactions.
 */
class ScenarioTest {

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

    WalletSnapshot ws9 = processWithdraw(ws8, "LTC", "-2", "82.49245900");
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

  @Test
  void testDepositFailWhenAssetPriceMissing() {
    WalletSnapshot ws1 = WalletSnapshot.createEmpty();
    assertThrows(IllegalArgumentException.class, () -> processDeposit(ws1, "LTC", "2", null));
  }

  @Test
  void testWithdrawFailWhenAssetPriceMissing() {
    WalletSnapshot ws1 = WalletSnapshot.createEmpty();
    ws1.addAsset("LTC", new Decimal("2"), new Decimal("200"));
    assertThrows(IllegalArgumentException.class, () -> processWithdraw(ws1, "LTC", "2", null));
  }

  @Test
  void testDepositUsdWithoutExtraInfo() {
    WalletSnapshot ws1 = WalletSnapshot.createEmpty();
    WalletSnapshot ws2 = processDeposit(ws1, "USD", "2000", null);
    expectWalletState(ws2, "0", "0", "2000", "USD", "1");

    ws2 = processDeposit(ws1, "USDT", "2000", null);
    expectWalletState(ws2, "0", "0", "2000", "USDT", "1");

    ws2 = processDeposit(ws1, "BUSD", "2000", null);
    expectWalletState(ws2, "0", "0", "2000", "BUSD", "1");
  }

  @Test
  void testWithdrawUsdWithoutExtraInfo() {
    WalletSnapshot ws1 = WalletSnapshot.createEmpty();
    ws1.addAsset("USD", new Decimal("1000"), Decimal.ONE);
    ws1.addAsset("BUSD", new Decimal("2000"), Decimal.ONE);
    ws1.addAsset("USDT", new Decimal("3000"), Decimal.ONE);

    WalletSnapshot ws2 = processWithdraw(ws1, "USD", "-500", null);
    expectWalletState(
        ws2, "0", "0",
        "500", "USD", "1",
        "2000", "BUSD", "1",
        "3000", "USDT", "1"
    );

    ws2 = processWithdraw(ws1, "BUSD", "-500", null);
    expectWalletState(
        ws2, "0", "0",
        "1000", "USD", "1",
        "1500", "BUSD", "1",
        "3000", "USDT", "1"
    );

    ws2 = processWithdraw(ws1, "USDT", "-500", null);
    expectWalletState(
        ws2, "0", "0",
        "1000", "USD", "1",
        "2000", "BUSD", "1",
        "2500", "USDT", "1"
    );
  }
}
