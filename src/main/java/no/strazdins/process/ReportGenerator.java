package no.strazdins.process;

import java.io.IOException;
import java.util.List;
import no.strazdins.data.ExtraInfo;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.RawAccountChange;
import no.strazdins.file.TransactionFileReader;
import no.strazdins.tool.TimeConverter;
import no.strazdins.transaction.Transaction;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Logic for generation of the PNL report.
 */
public class ReportGenerator {
  private static final Logger logger = LogManager.getLogger(ReportGenerator.class);

  /**
   * Analyze Transaction CSV file exported from Binance, generate a report, write it in
   * the output file.
   *
   * @param inputFilePath Path to the CVS input file (exported from Binance)
   * @param extraFilePath Path to a CSV file where necessary extra information is stored
   */
  public Report createReport(String inputFilePath, String extraFilePath, String homeCurrency)
      throws IOException {
    List<Transaction> transactions = readTransactions(inputFilePath);
    ExtraInfoHandler extraInfoHandler = new ExtraInfoHandler(extraFilePath, homeCurrency);
    ExtraInfo missingInfo = extraInfoHandler.detectMissingInfo(transactions);
    if (!missingInfo.isEmpty()) {
      printMissingInfoRequirement(missingInfo, extraFilePath);
      throw new IOException("Some information missing, can't generate the report");
    }
    return generateReport(transactions, extraInfoHandler.getUserProvidedInfo());
  }

  private List<Transaction> readTransactions(String inputFilePath) throws IOException {
    List<RawAccountChange> accountChanges = TransactionFileReader.readAccountChanges(inputFilePath);
    ReportLogic logic = new ReportLogic();
    logic.updateLendingAssets(accountChanges);
    List<Transaction> rawTransactions = logic.groupTransactionsByTimestamp(accountChanges);
    return logic.clarifyTransactionTypes(rawTransactions);
  }

  private static Report generateReport(List<Transaction> transactions, ExtraInfo extraUserInfo) {
    Report report = new Report(extraUserInfo);
    for (Transaction transaction : transactions) {
      report.process(transaction);
    }
    return report;
  }

  private static void printMissingInfoRequirement(ExtraInfo missingInfo, String extraFilePath) {
    logger.error("Provide the necessary information in the extra-info file `{}`:", extraFilePath);
    if (logger.isEnabled(Level.ERROR)) {
      for (ExtraInfoEntry mi : missingInfo.getAllEntries()) {
        logger.error("{},{},{},{},{}", mi.utcTimestamp(),
            TimeConverter.utcTimeToString(mi.utcTimestamp()), mi.type(), mi.asset(), mi.value());
      }
    }
  }

}
