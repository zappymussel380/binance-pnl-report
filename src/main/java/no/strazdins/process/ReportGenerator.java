package no.strazdins.process;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import no.strazdins.data.ExtraInfo;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.RawAccountChange;
import no.strazdins.file.ReportFileWriter;
import no.strazdins.file.TransactionFileReader;
import no.strazdins.transaction.Transaction;


/**
 * Logic for generation of the PNL report.
 */
public class ReportGenerator {
  private static final String TRANSACTION_LOG_CSV_FILE = "transactions.csv";
  private final String inputFilePath;
  private final String homeCurrency;
  private final String extraFilePath;

  /**
   * Create a new report generator.
   *
   * @param inputFilePath  Path to the CVS input file (exported from Binance)
   * @param homeCurrency   Home currency - used for profit-and-loss calculations
   * @param extraFilePath  Path to a CSV file where necessary extra information is stored
   * @throws IOException When some error happened during input file reading or output file writing
   */
  public ReportGenerator(String inputFilePath, String homeCurrency, String extraFilePath)
      throws IOException {
    this.inputFilePath = inputFilePath;
    this.homeCurrency = homeCurrency;
    this.extraFilePath = extraFilePath;
  }

  /**
   * Analyze Transaction CSV file exported from Binance, generate a report, write it in
   * the output file.
   */
  public void createReport() throws IOException {
    List<Transaction> transactions = readTransactions();
    ExtraInfoHandler extraInfoHandler = new ExtraInfoHandler(extraFilePath);
    ExtraInfo missingInfo = extraInfoHandler.detectMissingInfo(transactions);
    if (missingInfo.isEmpty()) {
      Report report = generateReport(transactions, extraInfoHandler.getUserProvidedInfo());
      ReportFileWriter.writeTransactionLogToFile(report, TRANSACTION_LOG_CSV_FILE);
    } else {
      printMissingInfoRequirement(missingInfo);
    }
  }

  private List<Transaction> readTransactions() throws IOException {
    List<RawAccountChange> accountChanges = TransactionFileReader.readAccountChanges(inputFilePath);
    List<Transaction> rawTransactions = groupTransactionsByTimestamp(accountChanges);
    return clarifyTransactionTypes(rawTransactions);
  }

  private List<Transaction> groupTransactionsByTimestamp(List<RawAccountChange> accountChanges) {
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
  private List<Transaction> clarifyTransactionTypes(List<Transaction> rawTransactions) {
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


  private Report generateReport(List<Transaction> transactions, ExtraInfo extraUserInfo) {
    Report report = new Report(extraUserInfo);
    for (Transaction transaction : transactions) {
      report.process(transaction);
    }
    return report;
  }


  private void printMissingInfoRequirement(ExtraInfo missingInfo) {
    System.out.println("Provide the necessary information in the extra-info file `"
        + extraFilePath + "`: ");
    for (ExtraInfoEntry mi : missingInfo.getAllEntries()) {
      System.out.println(mi.utcTimestamp() + "," + mi.type() + "," + mi.value());
    }
  }

}
