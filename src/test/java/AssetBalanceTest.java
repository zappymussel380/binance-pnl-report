import no.strazdins.data.AssetBalance;
import no.strazdins.data.Decimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AssetBalanceTest {
  @Test
  void testAdd() {
    AssetBalance b = new AssetBalance(new Decimal("8"), new Decimal("10"));
    assertEquals("8", b.getAmount().getNiceString());
    assertEquals("10", b.getObtainPrice().getNiceString());

    b.add(new Decimal("2"), new Decimal("10"));
    assertEquals("10", b.getAmount().getNiceString());
    assertEquals("10", b.getObtainPrice().getNiceString());

    b.add(new Decimal("15"), new Decimal("5"));
    assertEquals("25", b.getAmount().getNiceString());
    assertEquals("7", b.getObtainPrice().getNiceString());

    b.add(new Decimal("2.7"), new Decimal("3"));
    // Money spent in the last part: 2.7 * 3 = 8.1
    // Total money spent: 175 + 8.1 = 183.1
    // Total asset amount: 25 + 2.7 = 27.7
    // Avg price: 183.1 / 27.7 = 6.61010830
    assertEquals("27.7", b.getAmount().getNiceString());
    assertEquals("6.61010830", b.getObtainPrice().toString());
    assertEquals("6.6101083", b.getObtainPrice().getNiceString());
  }
}
