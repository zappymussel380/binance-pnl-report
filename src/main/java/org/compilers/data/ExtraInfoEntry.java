package org.compilers.data;

/**
 * Represents one unit of extra information for a financial transaction, provided by the user.
 *
 * @param utcTimestamp UTC timestamp, including milliseconds.
 * @param type         The type of the information
 * @param value        A numeric value, formatted as a decimal-string. The meaning of the value
 *                     depends on the type. For example: the exchange rate.
 */
public record ExtraInfoEntry(long utcTimestamp, ExtraInfoType type, String value) {
}
