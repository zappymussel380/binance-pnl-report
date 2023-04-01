package no.strazdins.file;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Writes output to CSV files.
 */
public class CsvFileWriter {
  final FileWriter writer;
  final int columnCount;

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
    writeRow(headerRow);
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
    String row = String.join(",", columns);
    writer.write(row + "\n");
  }
}
