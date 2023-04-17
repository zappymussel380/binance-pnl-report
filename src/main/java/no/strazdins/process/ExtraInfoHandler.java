package no.strazdins.process;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import no.strazdins.data.ExtraInfo;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.ExtraInfoType;
import no.strazdins.file.CsvFileParser;
import no.strazdins.tool.TimeConverter;
import no.strazdins.transaction.Transaction;

/**
 * Handles extra information provided by the user (as an external CSV file).
 */
public class ExtraInfoHandler {
  private final String extraFilePath;

  private ExtraInfo userProvidedInfo;

  private final String homeCurrency;

  /**
   * Create a new ExtraInfoHandler.
   *
   * @param extraFilePath Path to the CSV file where the user has provided extra info
   * @param homeCurrency  The Home currency in which the report wil be generated
   * @throws IOException When something goes wrong with reading the CSV file with extra user info.
   */
  public ExtraInfoHandler(String extraFilePath, String homeCurrency) throws IOException {
    this.extraFilePath = extraFilePath;
    this.homeCurrency = homeCurrency;
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
    ExtraInfo extraInfo = getNecessaryTransactionExtraInfo(transactions);
    getNecessaryYearEndInfo(extraInfo, getTransactionYears(transactions));
    return extraInfo;
  }

  /**
   * Go through a list of transactions, find out which years they are covering.
   *
   * @param transactions The list of transactions to check
   * @return as set of integers representing the years of the transactions
   */
  public static Set<Integer> getTransactionYears(List<Transaction> transactions) {
    Set<Integer> years = new HashSet<>();
    for (Transaction transaction : transactions) {
      years.add(TimeConverter.getUtcYear(transaction.getUtcTime()));
    }
    return years;
  }

  private static ExtraInfo getNecessaryTransactionExtraInfo(List<Transaction> transactions) {
    ExtraInfo necessaryInfo = new ExtraInfo();
    for (Transaction t : transactions) {
      ExtraInfoEntry necessaryExtraInfo = t.getNecessaryExtraInfo();
      if (necessaryExtraInfo != null) {
        necessaryInfo.add(necessaryExtraInfo);
      }
    }
    return necessaryInfo;
  }

  /**
   * Get the necessary extra information for end of the year, covering the years of all the
   * transactions (such as HC/USD exchange rate at the end of each year).
   *
   * @param extraInfo The necessary year-end info will be added to this extraInfo object
   * @param years     The years covering the transactions as integers (for example [2019, 2020])
   */
  private void getNecessaryYearEndInfo(ExtraInfo extraInfo, Set<Integer> years) {
    for (int year : years) {
      extraInfo.add(getYearEndExchangeRateInfo(year));
    }
  }

  private ExtraInfoEntry getYearEndExchangeRateInfo(int year) {
    long yearEndTimestamp = TimeConverter.getYearEndTimestamp(year);
    return new ExtraInfoEntry(yearEndTimestamp, ExtraInfoType.ASSET_PRICE, homeCurrency,
        "<" + homeCurrency + "/USD exchange rate at the end of year " + year + ">");
  }

  private ExtraInfoEntry createExtraInfoEntryFromCsvRow(String[] csvRow) throws IOException {
    return new ExtraInfoEntry(
        TimeConverter.parseLong(csvRow[0]),
        ExtraInfoType.fromString(csvRow[2]),
        csvRow[3],
        TimeConverter.parseDecimalString(csvRow[4])
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
