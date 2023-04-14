package no.strazdins.process;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import no.strazdins.data.ExtraInfo;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.RawAccountChange;
import no.strazdins.file.TransactionFileReader;
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
   * Not allowed to construct an object of this class.
   */
  private ReportGenerator() {
  }

  /**
   * Analyze Transaction CSV file exported from Binance, generate a report, write it in
   * the output file.
   *
   * @param inputFilePath Path to the CVS input file (exported from Binance)
   * @param extraFilePath Path to a CSV file where necessary extra information is stored
   */
  public static Report createReport(String inputFilePath, String extraFilePath, String homeCurrency)
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

  private static List<Transaction> readTransactions(String inputFilePath) throws IOException {
    List<RawAccountChange> accountChanges = TransactionFileReader.readAccountChanges(inputFilePath);
    List<Transaction> rawTransactions = groupTransactionsByTimestamp(accountChanges);
    return clarifyTransactionTypes(rawTransactions);
  }

  private static List<Transaction> groupTransactionsByTimestamp(
      List<RawAccountChange> accountChanges) {
    List<Transaction> transactions = new LinkedList<>();
    RawAccountChange lastChange = null;
    Transaction transaction = null;
    for (RawAccountChange change : accountChanges) {
      if (lastChange == null || lastChange.getUtcTime() != change.getUtcTime()) {
        transaction = new Transaction(change.getUtcTime());
        transactions.add(transaction);
      }
      transaction.append(change);
      lastChange = change;
    }
    return transactions;
  }

  /**
   * Go through a list of raw transactions, look at their atomic changes, decide the type of each
   * transaction: Deposit, Buy, Saving interest, etc.
   *
   * @param rawTransactions Raw transactions
   * @return List of the same transactions, but with specific types
   */
  private static List<Transaction> clarifyTransactionTypes(List<Transaction> rawTransactions) {
    List<Transaction> transactions = new LinkedList<>();
    for (Transaction rawTransaction : rawTransactions) {
      Transaction transaction = rawTransaction.clarifyTransactionType();
      if (transaction != null) {
        transactions.add(transaction);
      } else {
        // !!! throw new IllegalStateException("Unknown transaction: " + rawTransaction);
      }
    }
    return transactions;
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
        logger.error("{},{},{},{}", mi.utcTimestamp(), mi.type(), mi.asset(), mi.value());
      }
    }
  }

}
