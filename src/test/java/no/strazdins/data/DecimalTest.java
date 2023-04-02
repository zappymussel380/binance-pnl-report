package no.strazdins.data;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Decimal class
 */
class DecimalTest {

  @Test
  void testCreate() {
    assertEquals("1213.63895888", new Decimal("1213.63895888").getNiceString());
    assertEquals("1", new Decimal("1").getNiceString());
    assertEquals("12", new Decimal("12").getNiceString());
    assertEquals("123456789", new Decimal("123456789").getNiceString());
    assertEquals("1213", new Decimal("1213.0").getNiceString());
  }

  @Test
  void testPositive() {
    assertFalse(new Decimal("0").isPositive());
    assertFalse(new Decimal("-1").isPositive());
    assertFalse(new Decimal("00000000.0000000").isPositive());
    assertFalse(new Decimal("-0.00000000001").isPositive());
    assertFalse(new Decimal("-999999999999999999").isPositive());

    assertTrue(new Decimal("1").isPositive());
    assertTrue(new Decimal("0.00000001").isPositive());
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
  void testEquality() {
    assertEquals(new Decimal("13.63895880"), new Decimal("13.63895880"));
    assertEquals(new Decimal("13.63895880"), new Decimal("13.6389588"));
    assertEquals(new Decimal("13.6389588"), new Decimal("13.6389588"));
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
    assertEquals(new Decimal("22.52711091"),
        new Decimal("13.6389588").multiply(new Decimal("1.6516738")));
  }

  @Test
  void testDivide() {
    Decimal d = new Decimal("5");
    assertEquals(Decimal.ONE, d.divide(d));
    assertEquals(d, d.divide("1"));
    assertEquals(d.negate(), d.divide("-1"));
    assertEquals(Decimal.ONE.negate(), d.divide("-5"));
    assertEquals(new Decimal("0.0005"), d.divide("10000"));

    assertEquals(new Decimal("653.44092084"),
        new Decimal("8912.25365379").divide(new Decimal("13.63895858")));
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

    assertEquals(new Decimal("13.63895895"),
        new Decimal( "1.65167382").add(new Decimal("11.98728513")));
    assertEquals(new Decimal("133.99999999"),
        new Decimal( "111.77777777").add(new Decimal("22.22222222")));
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
    assertEquals(new Decimal("1.6516738"),
        new Decimal("13.6389588").subtract(new Decimal("11.987285")));
    assertEquals(new Decimal("119.29632526"), new Decimal("119.41574100").subtract("0.11941574"));
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
    assertEquals(0, Decimal.createArray(null).length);
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
    assertEquals("0.00000001", new Decimal("0.00000001").getNiceString());
    assertEquals("0.1", new Decimal("0.1000000000").getNiceString());
    assertEquals("0.00708", new Decimal("0.007080000000").getNiceString());
    assertEquals("-3.28", new Decimal("-3.28").getNiceString());
    assertEquals("-3.28", new Decimal("-3.28000").getNiceString());
    assertEquals("500", new Decimal("500").getNiceString());
  }

  @Test
  void testIntDigitCount() {
    assertEquals(0, Decimal.getIntDigitCount(null));
    assertEquals(0, Decimal.getIntDigitCount(""));
    assertEquals(0, Decimal.getIntDigitCount(".2"));
    assertEquals(0, Decimal.getIntDigitCount(".2"));
    assertEquals(1, Decimal.getIntDigitCount("1"));
    assertEquals(2, Decimal.getIntDigitCount("82"));
    assertEquals(6, Decimal.getIntDigitCount("666888"));
    assertEquals(1, Decimal.getIntDigitCount("1.88"));
    assertEquals(1, Decimal.getIntDigitCount("1."));
    assertEquals(2, Decimal.getIntDigitCount("-15"));
    assertEquals(2, Decimal.getIntDigitCount("-15.8"));
    assertEquals(2, Decimal.getIntDigitCount("82.667"));
    assertEquals(6, Decimal.getIntDigitCount("666888.88866655"));
  }

  @Test
  void testRemoveMinusSign() {
    assertEquals("0", Decimal.removeMinusSign("0"));
    assertEquals("12", Decimal.removeMinusSign("12"));
    assertEquals("12345678", Decimal.removeMinusSign("12345678"));
    assertEquals("8", Decimal.removeMinusSign("-8"));
    assertEquals("0.78", Decimal.removeMinusSign("-0.78"));
    assertEquals(".89", Decimal.removeMinusSign("-.89"));
    try {
      Decimal.removeMinusSign("--8");
      assertFalse(true);
    } catch (IllegalArgumentException e) {
    }
    try {
      Decimal.removeMinusSign(" -8");
      assertFalse(true);
    } catch (IllegalArgumentException e) {
    }
  }
}