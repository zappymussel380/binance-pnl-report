package no.strazdins.transaction;

import static no.strazdins.testtools.TestTools.*;

import no.strazdins.data.WalletSnapshot;
import org.junit.jupiter.api.Test;

/**
 * Tests for currency exchange (conversion) transactions.
 */
class ConversionTest {
  @Test
  void testSimpleExchange() {
    WalletSnapshot ws1 = WalletSnapshot.createEmpty();
    WalletSnapshot ws2 = processDeposit(ws1, "EUR", "100", "1.1");
    expectWalletState(ws2, "0", "0", "100", "EUR", "1.1");
    WalletSnapshot ws3 = processDeposit(ws2, "USDT", "100", "1");
    expectWalletState(ws3, "0", "0",
        "100", "EUR", "1.1",
        "100", "USDT", "1"
    );

    // Sell 8 EUR, get 10 USD, profit 1.2 USD
    WalletSnapshot ws41 = processConversion(ws2, "-8", "EUR", "10", "USDT");
    expectWalletState(ws41, "1.2", "1.2",
        "92", "EUR", "1.1",
        "110", "USDT", "0.979490909"
    );

    // Sell 10 EUR, get 10 USD, loss 1 USD
    WalletSnapshot ws42 = processConversion(ws2, "-10", "EUR", "10", "USDT");
    expectWalletState(ws42, "-1", "-1",
        "90", "EUR", "1.1",
        "110", "USDT", "1.019090909"
    );

    // Sell 10 USD, get 8 EUR, no profit or loss (not yet)
    WalletSnapshot ws43 = processConversion(ws2, "-10", "USDT", "8", "EUR");
    expectWalletState(ws43, "0", "0",
        "108", "EUR", "1.111111111",
        "90", "USDT", "1"
    );
  }

  // TODO - other tests:
  // * Sell USD -> EUR with profit
  // * Sell USD -> EUR with loss

  // TODO - AirDrop tests, including some where a non-existing coin is dropped, obtain-price is 0,
  //  check whether a sell afterwards is calculated correctly
}
