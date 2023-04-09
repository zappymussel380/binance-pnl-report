package no.strazdins.tool;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility class for conversion between different formats.
 */
public class Converter {
  private static final SimpleDateFormat timestampFormat
      = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  private static final SimpleDateFormat dateFormat
      = new SimpleDateFormat("yyyy-MM-dd");

  // Need this to ensure that all time strings are parsed correctly in the UTC timezone
  static {
    timestampFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  /**
   * No construction of the object is allowed.
   */
  private Converter() {
  }

  /**
   * Convert a timestamp string in the format "yyyy-MM-dd hh:mm:ss" (such as "2022-12-20 20:48:22")
   * to a unix timestamp in UTC timezone, with milliseconds.
   *
   * @param timeString The timestamp string
   * @return Unix timestamp, with milliseconds (UTC timezone)
   * @throws IllegalArgumentException When the time string format is incorrect
   */
  public static long stringToUtcTimestamp(String timeString) throws IllegalArgumentException {
    try {
      Date parsedDate = timestampFormat.parse(timeString);
      return parsedDate.getTime();
    } catch (ParseException e) {
      throw new IllegalArgumentException("Invalid time string: " + timeString);
    }
  }

  /**
   * Format a UTC timestamp as a string in the format YYYY-MM-dd HH:mm:ss.
   *
   * @param utcTimestamp UTC timestamp, including milliseconds
   * @return Formatted string
   */
  public static String utcTimeToString(long utcTimestamp) {
    return timestampFormat.format(new Date(utcTimestamp));
  }

  /**
   * Format a UTC timestamp as a date-only string in the format YYYY-MM-dd.
   *
   * @param utcTimestamp UTC timestamp, including milliseconds
   * @return Formatted string
   */
  public static String utcTimeToDateString(long utcTimestamp) {
    return dateFormat.format(new Date(utcTimestamp));
  }

  /**
   * Parse a string, check whether it is a valid decimal.
   *
   * @param s The string to check
   * @return The same string, if it contains a decimal
   * @throws IOException An exception if the number format is invalid
   */
  public static String parseDecimalString(String s) throws IOException {
    try {
      Double.parseDouble(s);
      return s;
    } catch (NumberFormatException e) {
      throw new IOException("Invalid number format: " + s);
    }
  }

  /**
   * Parse a string, treat it as a long integer.
   *
   * @param s The string to parse
   * @return The long integer value of the string
   * @throws IOException An exception if the number format is invalid
   */
  public static Long parseLong(String s) throws IOException {
    try {
      return Long.parseLong(s);
    } catch (NumberFormatException e) {
      throw new IOException("Invalid number format: " + s);
    }
  }

  /**
   * Get the year of the given timestamp.
   *
   * @param utcTime UTC timestamp, including milliseconds
   * @return Year of the timestamp, as an integer. For example: 2023
   */
  public static Integer getUtcYear(long utcTime) {
    Instant instant = Instant.ofEpochMilli(utcTime);
    return instant.atZone(ZoneOffset.UTC).getYear();
  }
}
