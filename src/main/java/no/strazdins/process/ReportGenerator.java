package no.strazdins.process;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import no.strazdins.data.ExtraInfo;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;
import no.strazdins.file.TransactionFileReader;
import no.strazdins.transaction.AutoInvestTransaction;
import no.strazdins.transaction.Transaction;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Logic for generation of the PNL report.
 */
public class ReportGenerator {
  private static final Logger logger = LogManager.getLogger(ReportGenerator.class);

  private final List<Transaction> transactions = new ArrayList<>();

  private AutoInvestSubscription autoInvestSubscription;
  private final List<AutoInvestTransaction> autoInvestTransactions = new LinkedList<>();
  private final List<AutoInvestTransaction> previousAutoInvestTransactions = new LinkedList<>();

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
    List<Transaction> rawTransactions = groupTransactionsByTimestamp(accountChanges);
    return clarifyTransactionTypes(rawTransactions);
  }

  private List<Transaction> groupTransactionsByTimestamp(List<RawAccountChange> accountChanges) {
    RawAccountChange lastChange = null;
    Transaction transaction = null;
    for (RawAccountChange change : accountChanges) {
      if (lastChange == null || lastChange.getUtcTime() != change.getUtcTime()) {
        transaction = new Transaction(change.getUtcTime());
        transactions.add(transaction);
      }
      if (AutoInvestTransaction.isAutoInvestOperation(change)) {
        Transaction updatedTransaction = updateAutoInvest(change, transaction);
        if (updatedTransaction != transaction) {
          replaceLastTransactionWith(updatedTransaction);
          transaction = updatedTransaction;
        }
      }
      transaction.append(change);
      lastChange = change;
    }
    return transactions;
  }

  private Transaction updateAutoInvest(RawAccountChange change,
                                       Transaction transaction) {
    if (isAutoInvestSpendOperation(change)) {
      if (isNewAutoInvestSubscription()) {
        autoInvestSubscription = new AutoInvestSubscription(change.getAmount().negate());
        updateSubscriptionForCachedAutoInvestTransactions();
      }
      rememberLastAutoInvestTransactions();
    } else if (!isAutoInvestAcquireOperation(change)) {
      throw new IllegalStateException("Auto-invest but neither invest, nor acquire: " + change);
    }

    if (!(transaction instanceof AutoInvestTransaction)) {
      transaction = new AutoInvestTransaction(transaction, autoInvestSubscription);
      autoInvestTransactions.add((AutoInvestTransaction) transaction);
    }

    return transaction;
  }

  private void rememberLastAutoInvestTransactions() {
    previousAutoInvestTransactions.clear();
    previousAutoInvestTransactions.addAll(autoInvestTransactions);
    autoInvestTransactions.clear();
  }

  private void updateSubscriptionForCachedAutoInvestTransactions() {
    for (AutoInvestTransaction a : autoInvestTransactions) {
      a.setSubscription(autoInvestSubscription);
    }
  }

  /**
   * This returns true when the current auto-invest operation signals start of a new
   * subscription where either the amounts or money distribution among coins has changed.
   *
   * @return True if new auto-invest subscription has started
   */
  private boolean isNewAutoInvestSubscription() {
    if (autoInvestSubscription == null) {
      return true;
    }
    if (previousAutoInvestTransactions.isEmpty()) {
      return false;
    }
    // TODO - include USDT and it's amount as well - if amount changes, that is a new subscription
    Set<String> prevCoins = getCoins(previousAutoInvestTransactions);
    Set<String> currentCoins = getCoins(autoInvestTransactions);
    return !prevCoins.equals(currentCoins);
  }

  /**
   * Get the coins involved in the last auto-invest transaction - the bought coins.
   *
   * @param transactions The transactions for the previous auto-invest
   * @return The bought coins
   */
  private static Set<String> getCoins(List<AutoInvestTransaction> transactions) {
    Set<String> coins = new HashSet<>();
    for (AutoInvestTransaction t : transactions) {
      String boughtAsset = t.getBoughtAsset();
      if (boughtAsset != null) {
        coins.add(boughtAsset);
      }
    }
    return coins;
  }


  private boolean isAutoInvestSpendOperation(RawAccountChange change) {
    return change.getOperation().equals(Operation.AUTO_INVEST)
        && "USDT".equals(change.getAsset())
        && change.getAmount().isNegative();
  }

  private boolean isAutoInvestAcquireOperation(RawAccountChange change) {
    return change.getOperation().equals(Operation.AUTO_INVEST)
        && !"USDT".equals(change.getAsset())
        && change.getAmount().isPositive();
  }

  private void replaceLastTransactionWith(Transaction t) {
    transactions.remove(transactions.size() - 1);
    transactions.add(t);
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
        logger.error("Unknown transaction: {}", rawTransaction.getOperationMultiSet());
        throw new IllegalStateException("Unknown transaction: " + rawTransaction);
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
