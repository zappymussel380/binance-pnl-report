package no.strazdins.process;

import static no.strazdins.testtools.TestTools.createChanges;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import no.strazdins.data.RawAccountChange;
import org.junit.jupiter.api.Test;

class ReportLogicTest {
  @Test
  void testLendingCurrencyUpdate() {
    List<RawAccountChange> changes = createChanges(
        "SPOT", "Savings Distribution", "2000", "LDUSDT",
        "EARN", "Simple Earn Flexible Subscription", "-2000", "USDT",
        "EARN", "Simple Earn Flexible Redemption", "-2300", "LDUSDT",
        "EARN", "Simple Earn Flexible Subscription", "0.1", "LDBTC",
        "EARN", "Simple Earn Flexible Redemption", "2300", "USDT",
        "EARN", "Simple Earn Flexible Subscription", "-0.1", "BTC"
    );
    expectChangeAssets(changes, "LDUSDT", "USDT", "LDUSDT", "LDBTC", "USDT", "BTC");

    new ReportLogic().updateLendingAssets(changes);
    expectChangeAssets(changes, "USDT", "USDT", "USDT", "BTC", "USDT", "BTC");
  }

  private void expectChangeAssets(List<RawAccountChange> changes, String... assets) {
    assertEquals(changes.size(), assets.length, "Expected " + assets.length + " changes");
    for (int i = 0; i < assets.length; ++i) {
      assertEquals(assets[i], changes.get(i).getAsset(), "expected asset[" + i + "] + to be "
          + assets[i]);
    }
  }

}
