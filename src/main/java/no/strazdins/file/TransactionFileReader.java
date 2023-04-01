package no.strazdins.file;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import no.strazdins.data.AccountType;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.tool.Converter;


/**
 * Reads transactions from the Binance-generated CSV file.
 */
public class TransactionFileReader {
  /**
   * Not allowed to create instances of the class.
   */
  private TransactionFileReader() {

  }

  /**
   * Read CSV input file, return a list of raw account changes.
   *
   * @param inputFilePath Path to the CSV input file
   * @return List of atomics account changes
   * @throws IOException When something goes wrong with file reading
   */
  public static List<RawAccountChange> readAccountChanges(String inputFilePath)
      throws IOException {
    CsvFileParser csvParser = new CsvFileParser(inputFilePath);

    String[] headerRow = csvParser.readNextRow();
    checkHeaderRowFormat(headerRow);

    List<RawAccountChange> accountChanges = new LinkedList<>();
    RawAccountChange previousChange = null;
    while (csvParser.hasMoreRows()) {
      String[] row = csvParser.readNextRow();
      RawAccountChange change = createAccountChangeFromCsvRow(row);
      if (previousChange != null && previousChange.getUtcTime() > change.getUtcTime()) {
        throw new IOException("Decreasing timestamp detected: " + previousChange + " -> " + change);
      }
      accountChanges.add(change);
      previousChange = change;
    }

    return accountChanges;
  }

  private static void checkHeaderRowFormat(String[] headerRow) throws IOException {
    if (headerRow.length != 7 || !"User_ID".equals(headerRow[0])
        || !"UTC_Time".equals(headerRow[1])
        || !"Account".equals(headerRow[2])
        || !"Operation".equals(headerRow[3])
        || !"Coin".equals(headerRow[4])
        || !"Change".equals(headerRow[5])
        || !"Remark".equals(headerRow[6])) {
      throw new IOException("Invalid header row format: " + String.join(",", headerRow));
    }
  }

  private static RawAccountChange createAccountChangeFromCsvRow(String[] row) throws IOException {
    if (row.length != 7) {
      throw new IOException("Invalid row format: " + String.join(",", row));
    }
    long utcTimestamp = Converter.stringToUtcTimestamp(row[1]);
    AccountType accountType = AccountType.fromString(row[2]);
    Operation operation = Operation.fromString(row[3]);
    String asset = row[4];
    String change = Converter.parseDecimalString(row[5]);
    String remark = row[6];
    return new RawAccountChange(utcTimestamp, accountType, operation, asset, change, remark);
  }

}
