package no.strazdins.file;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormatSymbols;

/**
 * Writes output to CSV files.
 * Note: it detects the decimal separator used in the OS and writes the CSV file accordingly:
 * - If '.' is the decimal separator, separate columns with comma: ','
 * - If ',' is the decimal separator, separate columns with semicolon: ';'
 */
public class CsvFileWriter {
  final FileWriter writer;
  final int columnCount;
  final boolean useCommaForDecimalSeparator;
  final String columnSeparator;

  /**
   * Create a CSV file writer.
   *
   * @param filePath  Path to the CSV file
   * @param headerRow The strings that will be used as the header row - the colum names
   * @throws IOException When file writing fails
   */
  public CsvFileWriter(String filePath, String[] headerRow) throws IOException {
    writer = new FileWriter(filePath);
    columnCount = headerRow.length;
    useCommaForDecimalSeparator = isOsDecimalSeparatorComma();
    columnSeparator = useCommaForDecimalSeparator ? ";" : ",";
    writeRow(headerRow);
  }

  /**
   * Check whether the decimal separator in the Operating System is comma (instead of the dot).
   *
   * @return True when comma is used as a decimal separator, false otherwise
   */
  private boolean isOsDecimalSeparatorComma() {
    DecimalFormatSymbols dfs = new DecimalFormatSymbols();
    char decimalSeparator = dfs.getDecimalSeparator();
    return decimalSeparator == ',';
  }

  /**
   * Close the file for writing. Call this when all the operations are done.
   *
   * @throws IOException When the file-closing operation fails
   */
  public void close() throws IOException {
    writer.close();
  }

  /**
   * Write one row in the CSV file.
   *
   * @param columns The values of the different columns
   * @throws IOException              When file writing fails
   * @throws IllegalArgumentException If the number of columns does not correspond to
   *                                  the number of columns in the first row (header)
   */
  public void writeRow(String[] columns) throws IOException, IllegalArgumentException {
    if (columns.length != columnCount) {
      throw new IllegalArgumentException("Invalid column count: " + columns.length
          + ", must be " + columnCount + " columns");
    }
    String row = String.join(columnSeparator, columns);
    if (useCommaForDecimalSeparator) {
      row = replaceDecimalDotsWithCommas(row);
    }
    writer.write(row + "\n");
  }

  private String replaceDecimalDotsWithCommas(String row) {
    return row.replace(".", ",");
  }
}
