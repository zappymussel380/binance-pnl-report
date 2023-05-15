package no.strazdins.data;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Use this class for storing prices and other decimal numbers without losing
 * precision.
 */
public class Decimal implements Comparable<Decimal> {

  // Store 8 decimal digits for all numbers
  private static final int DEFAULT_SCALE = 8;
  // Default rounding: round values >= 0.5 up to 1.0, others to 0.0
  private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;
  // This is used for as a temporary scale for division
  private static final MathContext DIV_PRECISION
      = new MathContext(DEFAULT_SCALE * 4, DEFAULT_ROUNDING);

  public static final Decimal ZERO = new Decimal("0");
  public static final Decimal ONE = new Decimal("1");

  private final BigDecimal number;

  /**
   * Initialize the number from a string.
   *
   * @param number Decimal String. For example "12.345"
   * @throws NumberFormatException when the provided string is not a valid number
   */
  public Decimal(String number) throws NumberFormatException {
    this.number = new BigDecimal(number).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
  }

  public Decimal(BigDecimal bd) {
    this.number = bd.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
  }

  /**
   * Create a copy of d.
   *
   * @param d The number to copy
   */
  public Decimal(Decimal d) {
    this.number = d.number;
  }

  /**
   * Get number of integer digits (the digits before the decimal separator '.')
   *
   * @param decimalNumber The number to check, in decimal string representation
   * @return The number of integer digits in it or 0 if it is empty.
   * @throws IllegalArgumentException When the number format is incorrect
   */
  protected static int getIntDigitCount(String decimalNumber) {
    String positiveDec = removeMinusSign(decimalNumber);
    if (positiveDec == null) {
      return 0;
    }
    int dotPos = positiveDec.indexOf('.');
    return dotPos >= 0 ? dotPos : positiveDec.length();
  }

  /**
   * Remove the minus sign from a number.
   *
   * @param d The number, formatted as a decimal string
   * @return The same number with minus sign removed
   * @throws IllegalArgumentException When the number format is incorrect
   */
  protected static String removeMinusSign(String d) throws IllegalArgumentException {
    if (d == null) {
      return null;
    }
    int minusSignPosition = d.indexOf('-');
    if (minusSignPosition > 0) {
      throw new IllegalArgumentException("Invalid decimal number: " + d);
    }
    if (minusSignPosition == 0) {
      minusSignPosition = d.indexOf('-', 1);
      if (minusSignPosition >= 1) {
        throw new IllegalArgumentException("Can't have multiple minus signs: " + d);
      }
      return d.substring(1);
    } else {
      return d;
    }
  }

  /**
   * Return a new decimal whose value is original + d.
   *
   * @param d The decimal to add
   * @return A new decimal where the value is original + d
   */
  public Decimal add(Decimal d) {
    return new Decimal(this.number.add(d.number));
  }

  /**
   * Return a new decimal whose value is original + d.
   * (Syntactic sugar)
   *
   * @param d The decimal to add
   * @return A new decimal: original + d
   */
  public Decimal add(String d) {
    return add(new Decimal(d));
  }

  /**
   * Return a new decimal whose value is original - d.
   *
   * @param d The decimal to subtract
   * @return A new decimal: original - d
   */
  public Decimal subtract(Decimal d) {
    return new Decimal(this.number.subtract(d.number));
  }

  /**
   * Return a new decimal whose value is original - d.
   * (Syntactic sugar)
   *
   * @param d The decimal to subtract
   * @return A new decimal: original - d
   */
  public Decimal subtract(String d) {
    return subtract(new Decimal(d));
  }

  /**
   * Return a new decimal whose value is original * d.
   *
   * @param d The multiplier
   * @return A new decimal: original * d
   */
  public Decimal multiply(Decimal d) {
    return new Decimal(number.multiply(d.number));
  }

  /**
   * Return a new decimal whose value is original * d.
   * (Syntactic sugar)
   *
   * @param d The multiplier
   * @return A new decimal: original * d
   */
  public Decimal multiply(String d) {
    return multiply(new Decimal(d));
  }

  /**
   * Return a new decimal whose value is original / d.
   *
   * @param d The divisor
   * @return A new decimal: original / d
   */
  public Decimal divide(Decimal d) {
    return new Decimal(number.divide(d.number, DIV_PRECISION));
  }

  /**
   * Return a new decimal whose value is original / d.
   * (Syntactic sugar)
   *
   * @param d The divisor
   * @return A new decimal: original / d
   */
  public Decimal divide(String d) {
    return divide(new Decimal(d));
  }

  /**
   * Return a new decimal whose value is original * -1.
   *
   * @return A new decimal: -original
   */
  public Decimal negate() {
    return new Decimal(number.negate());
  }

  /**
   * Returns true if the number is positive (greater than zero).
   *
   * @return True if the number is greater than zero
   */
  public boolean isPositive() {
    return number.compareTo(BigDecimal.ZERO) > 0;
  }

  /**
   * Return true if value is negative.
   *
   * @return True if the number is less than zero
   */
  public boolean isNegative() {
    return number.compareTo(BigDecimal.ZERO) < 0;
  }

  /**
   * Return true if value is equal to zero.
   *
   * @return True if the value is equal to zero
   */
  public boolean isZero() {
    return equals(Decimal.ZERO);
  }

  /**
   * Return true if value is greater than the threshold.
   *
   * @param threshold The threshold to compare against
   * @return True if the current value is greater than the threshold, false if it is equal or lower
   */
  public boolean isGreaterThan(Decimal threshold) {
    return this.compareTo(threshold) > 0;
  }

  /**
   * Return true if value is less than the threshold.
   *
   * @param threshold The threshold to compare against
   * @return True when value is less than the threshold, false otherwise
   */
  public boolean isLessThan(Decimal threshold) {
    return this.compareTo(threshold) < 0;
  }

  @Override
  public int compareTo(Decimal d) {
    if (d == null) {
      return 1;
    }
    return this.number.compareTo(d.number);
  }

  /**
   * Compare only the numeric value, ignore the scale.
   *
   * @param o Teh object to compare against
   * @return True if this is equal to o, false otherwise
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Decimal)) {
      return false;
    }
    Decimal d = (Decimal) o;
    return d.number.compareTo(this.number) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(number);
  }

  @Override
  public String toString() {
    return number.toString();
  }

  /**
   * Get string representation, without the trailing zeros.
   *
   * @return A "nice" representation of the number with trailing zeros cut off.
   */
  public String getNiceString() {
    if (isZero()) {
      return "0";
    }
    String s = number.toPlainString();
    if (s.indexOf('.') < 0) {
      // When not a decimal, don't strip off anything
      return s;
    }
    // Find first non-zero character
    int i = s.length() - 1;
    while (i > 0 && s.charAt(i) == '0') {
      --i;
    }
    if (s.charAt(i) == '.') {
      --i;
    }
    if (i >= 0) {
      return s.substring(0, i + 1);
    } else {
      return "0";
    }
  }

  /**
   * Create an array of Decimals from an array of Strings.
   *
   * @param d Array of Decimal string values
   * @return Array of corresponding Decimal objects, or an empty array if d is null
   */
  public static Decimal[] createArray(String[] d) {
    if (d == null) {
      return new Decimal[]{};
    }
    Decimal[] res = new Decimal[d.length];
    for (int i = 0; i < d.length; ++i) {
      res[i] = new Decimal(d[i]);
    }
    return res;
  }
}