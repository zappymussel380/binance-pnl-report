import no.strazdins.data.Decimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Decimal class
 */
public class DecimalTest {
  private static final double DELTA = 0.0000001;

  @Test
  void testPositive() {
    assertFalse(new Decimal("0").isPositive());
    assertFalse(new Decimal("-1").isPositive());
    assertFalse(new Decimal("00000000.0000000").isPositive());
    assertFalse(new Decimal("-0.00000000001").isPositive());
    assertFalse(new Decimal("-999999999999999999").isPositive());

    assertTrue(new Decimal("1").isPositive());
    assertTrue(new Decimal("0.00000000001").isPositive());
    assertTrue(new Decimal("999999999999999999").isPositive());
  }

  @Test
  void testNegative() {
    assertFalse(Decimal.ZERO.isNegative());
    assertFalse(new Decimal("0.0000001").isNegative());
    assertFalse(new Decimal("8").isNegative());

    assertTrue(new Decimal("-1").isNegative());
    assertTrue(new Decimal("-0.00000001").isNegative());
    assertTrue(new Decimal("-8").isNegative());
  }

  @Test
  void testZero() {
    Decimal z1 = Decimal.ZERO;
    Decimal z2 = Decimal.ZERO;
    assertEquals(z1, z2); // Test if ZERO is static

    assertTrue(Decimal.ZERO.isZero());
    assertTrue(new Decimal("0").isZero());
    assertTrue(new Decimal("0.00000").isZero());
    assertTrue(new Decimal("-0.0000").isZero());
  }

  @Test
  void testCompare() {
    Decimal d1 = new Decimal("-5");
    Decimal d2 = new Decimal("-1");
    Decimal d3 = new Decimal("0");
    Decimal d4 = new Decimal("1");
    Decimal d5 = new Decimal("3");
    assertTrue(d1.isLessThan(d2));
    assertTrue(d2.isLessThan(d3));
    assertTrue(d3.isLessThan(d4));
    assertTrue(d4.isLessThan(d5));

    assertTrue(d5.isGreaterThan(d4));
    assertTrue(d4.isGreaterThan(d3));
    assertTrue(d3.isGreaterThan(d2));
    assertTrue(d2.isGreaterThan(d1));
  }

  @Test
  void testMultiply() {
    Decimal d = new Decimal("5");
    d = d.multiply(new Decimal("7"));
    assertEquals("35", d.getNiceString());
    d = d.multiply(new Decimal("-0.2"));
    assertEquals("-7", d.getNiceString());
    d = d.multiply(new Decimal("0"));
    assertEquals(Decimal.ZERO, d);
    d = d.multiply(new Decimal("-0.2"));
    assertEquals(Decimal.ZERO, d);
  }

  @Test
  void testDivide() {
    Decimal d = new Decimal("5");
    assertEquals(Decimal.ONE, d.divide(d));
    assertEquals(d, d.divide("1"));
    assertEquals(d.negate(), d.divide("-1"));
    assertEquals(Decimal.ONE.negate(), d.divide("-5"));
    assertEquals(new Decimal("0.0005"), d.divide("10000"));
  }

  @Test
  void testAdd() {
    Decimal d = new Decimal("5");
    d = d.add("1");
    assertEquals("6", d.getNiceString());
    d = d.add("-2");
    assertEquals(new Decimal("4.0"), d);
    assertEquals(d, d.add("0"));
    assertEquals(Decimal.ZERO, d.add(d.negate()));
  }

  @Test
  void testSubtract() {
    Decimal d = new Decimal("5");
    d = d.subtract("1");
    assertEquals("4", d.getNiceString());
    d = d.subtract("-2");
    assertEquals(new Decimal("6.0"), d);
    assertEquals(d, d.subtract("0"));
    assertEquals(d, d.subtract(Decimal.ZERO));
    assertEquals(Decimal.ZERO, d.subtract(d));
  }

  @Test
  void testNegate() {
    Decimal d = new Decimal("5");
    assertEquals(new Decimal("-5"), d.negate());
    assertEquals(Decimal.ZERO, Decimal.ZERO.negate());
    assertEquals(d, d.negate().negate());
  }

  @Test
  void testCreateArray() {
    assertNull(Decimal.createArray(null));
    Decimal[] d = Decimal.createArray(new String[0]);
    assertNotNull(d);
    assertEquals(0, d.length);
    d = Decimal.createArray(new String[]{"-2", "0", "0.12", "5555"});
    Decimal[] exp = new Decimal[]{
        new Decimal("-2"),
        Decimal.ZERO,
        new Decimal("0.12"),
        new Decimal("5555")
    };
    assertEquals(exp.length, d.length);
    for (int i = 0; i < d.length; ++i) {
      assertEquals(exp[i], d[i]);
    }
  }

  @Test
  void roundTiny() {
    Decimal d1 = new Decimal("0.000012");
    Decimal d2 = new Decimal("0.001");
    Decimal d3 = d1.multiply(d2);
    assertEquals(new Decimal("0.00000001"), d3);
  }

  @Test
  void testNiceString() {
    assertEquals("6", new Decimal("6.0").getNiceString());
    assertEquals("0", new Decimal("0").getNiceString());
    assertEquals("0", new Decimal("-0").getNiceString());
    assertEquals("0", new Decimal("-0.00000").getNiceString());
    assertEquals("0", new Decimal("0.00000000000").getNiceString());
    assertEquals("0.0000000001", new Decimal("0.0000000001").getNiceString());
    assertEquals("0.1", new Decimal("0.1000000000").getNiceString());
    assertEquals("0.00708", new Decimal("0.007080000000").getNiceString());
    assertEquals("-3.28", new Decimal("-3.28").getNiceString());
    assertEquals("-3.28", new Decimal("-3.28000").getNiceString());
    assertEquals("500", new Decimal("500").getNiceString());
  }
}