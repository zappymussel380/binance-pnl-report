package no.strazdins.data;

import static no.strazdins.data.Operation.*;
import static no.strazdins.testtools.TestTools.createSpotAccountChanges;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RawAccountChangeTest {

  @Test
  void testAmountAssertions() {
    long thisTime = System.currentTimeMillis();
    Decimal minusOne = new Decimal("-1");
    assertThrows(IllegalArgumentException.class, () -> new RawAccountChange(
        thisTime, AccountType.SPOT, BUY, "BTC", minusOne, "Negative buy"));
    assertThrows(IllegalArgumentException.class, () -> new RawAccountChange(
        thisTime, AccountType.SPOT, SELL, "BTC", Decimal.ONE, "Positive sell"));
    assertThrows(IllegalArgumentException.class, () -> new RawAccountChange(
        thisTime, AccountType.SPOT, WITHDRAW, "BTC", Decimal.ONE, "Positive withdraw"));
  }

  @Test
  void testMergeDifferentOpTypes() {
    assertRawChangeMergeThrows(
        "Buy", "1", "BTC",
        "Sell", "-0.5", "BTC"
    );
    assertRawChangeMergeThrows(
        "Buy", "1", "BTC",
        "Sell", "-1", "BTC",
        "Buy", "1", "BTC"
    );
    assertRawChangeMergeThrows(
        "Buy", "1", "BTC",
        "Buy", "1", "BTC",
        "Buy", "1", "BTC",
        "Sell", "-1", "BTC"
    );
    assertRawChangeMergeThrows(
        "Sell", "-1", "BTC",
        "Buy", "1", "BTC",
        "Buy", "1", "BTC",
        "Buy", "1", "BTC",
        "Buy", "1", "BTC"
    );
  }

  @Test
  void testMergeDifferentOpAssets() {
    assertRawChangeMergeThrows(
        "Buy", "1", "BTC",
        "Buy", "1", "LTC"
    );
    assertRawChangeMergeThrows(
        "Buy", "1", "BTC",
        "Buy", "1", "LTC",
        "Buy", "1", "BTC"
    );
    assertRawChangeMergeThrows(
        "Buy", "1", "BTC",
        "Buy", "1", "LTC",
        "Buy", "1", "BTC",
        "Buy", "1", "BTC",
        "Buy", "1", "BTC"
    );
    assertRawChangeMergeThrows(
        "Buy", "1", "BTC",
        "Buy", "1", "LTC",
        "Buy", "1", "BTC",
        "Buy", "1", "BTC",
        "Buy", "1", "BTC",
        "Buy", "1", "LTC"
    );
  }

  @Test
  void testMergeEmptyList() {
    List<RawAccountChange> emptyList = new LinkedList<>();
    assertThrows(IllegalArgumentException.class, () -> RawAccountChange.merge(emptyList));
  }

  @Test
  void testMergeNull() {
    assertThrows(IllegalArgumentException.class, () -> RawAccountChange.merge(null));
  }

  @Test
  void testMergeOne() {
    long thisTime = System.currentTimeMillis();
    RawAccountChange originalChange = new RawAccountChange(thisTime, AccountType.SPOT,
        BUY, "BTC", Decimal.ONE, "");
    RawAccountChange merged = RawAccountChange.merge(Collections.singletonList(originalChange));
    assertEquals(originalChange, merged);
  }

  @Test
  void testMergeTwo() {
    long thisTime = System.currentTimeMillis();
    List<RawAccountChange> originalChanges = createSpotAccountChanges(
        thisTime,
        "Buy", "0.1", "BTC",
        "Buy", "0.2", "BTC"
    );
    RawAccountChange merged = RawAccountChange.merge(originalChanges);
    RawAccountChange expected = new RawAccountChange(thisTime, AccountType.SPOT,
        BUY, "BTC", new Decimal("0.3"), "");
    assertEquals(expected, merged);
  }


  @Test
  void testMergeMultiple() {
    long thisTime = System.currentTimeMillis();
    List<RawAccountChange> originalChanges = createSpotAccountChanges(
        thisTime,
        "Buy", "0.1", "BTC",
        "Buy", "0.2", "BTC",
        "Buy", "0.3", "BTC",
        "Buy", "0.4", "BTC"
    );

    RawAccountChange merged = RawAccountChange.merge(originalChanges);
    RawAccountChange expected = new RawAccountChange(thisTime, AccountType.SPOT,
        BUY, "BTC", new Decimal("1.0"), "");
    assertEquals(expected, merged);
  }

  @Test
  void testMergeDifferentTimestamps() {
    long thisTime = System.currentTimeMillis();
    final List<RawAccountChange> changes = new LinkedList<>();
    changes.add(new RawAccountChange(thisTime, AccountType.SPOT,
        BUY, "BTC", Decimal.ONE, ""));
    changes.add(new RawAccountChange(thisTime + 1000, AccountType.SPOT,
        BUY, "BTC", Decimal.ONE, ""));
    assertThrows(IllegalArgumentException.class, () -> RawAccountChange.merge(changes));
  }

  private void assertRawChangeMergeThrows(String... changes) {
    final List<RawAccountChange> changeList = createSpotAccountChanges(
        System.currentTimeMillis(), changes);
    assertThrows(IllegalArgumentException.class, () -> RawAccountChange.merge(changeList));
  }
}
