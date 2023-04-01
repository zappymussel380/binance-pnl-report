import no.strazdins.data.Decimal;
import no.strazdins.data.Wallet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WalletTest {
  @Test
  void testCloning() {
    Wallet w1 = new Wallet();
    w1.addAsset("BTC", Decimal.ONE, new Decimal("24000"));

    Wallet w2 = w1.clone();
    assertEquals(w1, w2);

    w2.addAsset("BTC", new Decimal("1.2"), new Decimal("30000"));
    w2.addAsset("LTC", new Decimal("3"), new Decimal("30"));

    assertEquals(1, w1.getAssetCount());
    assertEquals(2, w2.getAssetCount());

    assertEquals("1", w1.getAssetAmount("BTC").getNiceString());
    assertEquals("2.2", w2.getAssetAmount("BTC").getNiceString());
    assertEquals(Decimal.ZERO, w1.getAssetAmount("LTC"));
    assertEquals("3", w2.getAssetAmount("LTC").getNiceString());
  }
}
