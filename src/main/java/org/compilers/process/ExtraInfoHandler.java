package org.compilers.process;

import java.io.IOException;
import java.util.List;
import org.compilers.data.ExtraInfo;
import org.compilers.data.ExtraInfoEntry;
import org.compilers.data.ExtraInfoType;
import org.compilers.data.Transaction;
import org.compilers.file.CsvFileParser;
import org.compilers.tool.Converter;

/**
 * Handles extra information provided by the user (as an external CSV file).
 */
public class ExtraInfoHandler {
  private final String extraFilePath;

  private ExtraInfo userProvidedInfo;

  /**
   * Create a new ExtraInfoHandler.
   *
   * @param extraFilePath Path to the CSV file where the user has provided extra info
   * @throws IOException When something goes wrong with reading the CSV file with extra user info.
   */
  public ExtraInfoHandler(String extraFilePath) throws IOException {
    this.extraFilePath = extraFilePath;
    readUserProvidedExtraInfo();
  }

  private void readUserProvidedExtraInfo() throws IOException {
    CsvFileParser csvParser = new CsvFileParser(extraFilePath);
    userProvidedInfo = new ExtraInfo();
    while (csvParser.hasMoreRows()) {
      userProvidedInfo.add(createExtraInfoEntryFromCsvRow(csvParser.readNextRow()));
    }
  }

  /**
   * Detect which extra-info is missing (not provided by the user) for the given transactions.
   *
   * @param transactions The transactions to consider
   * @return Necessary extra info which is missing - required to process the transactions
   */
  public ExtraInfo detectMissingInfo(List<Transaction> transactions) {
    ExtraInfo necessaryInfo = detectNecessaryExtraInfo(transactions);
    ExtraInfo missingInfo = new ExtraInfo();
    for (ExtraInfoEntry necessaryEntry : necessaryInfo.getAllEntries()) {
      if (!userProvidedInfo.contains(necessaryEntry)) {
        missingInfo.add(necessaryEntry);
      }
    }
    return missingInfo;
  }

  private ExtraInfo detectNecessaryExtraInfo(List<Transaction> transactions) {
    ExtraInfo necessaryInfo = new ExtraInfo();
    for (Transaction t : transactions) {
      ExtraInfoEntry necessaryExtraInfo = t.getNecessaryExtraInfo();
      if (necessaryExtraInfo != null) {
        necessaryInfo.add(necessaryExtraInfo);
      }
    }
    return necessaryInfo;
  }

  private ExtraInfoEntry createExtraInfoEntryFromCsvRow(String[] csvRow) throws IOException {
    return new ExtraInfoEntry(
        Converter.parseLong(csvRow[0]),
        ExtraInfoType.fromString(csvRow[1]),
        Converter.parseDecimalString(csvRow[2])
    );
  }

  /**
   * Get the information provided by the user. This can't be called before detectMissingInfo()!
   *
   * @return The user provided info
   */
  public ExtraInfo getUserProvidedInfo() {
    return userProvidedInfo;
  }
}
