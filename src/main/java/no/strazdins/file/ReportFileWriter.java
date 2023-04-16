package no.strazdins.file;

import java.io.IOException;
import java.util.List;
import no.strazdins.data.ExtraInfo;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.Wallet;
import no.strazdins.data.WalletSnapshot;
import no.strazdins.process.AnnualReport;
import no.strazdins.process.Report;
import no.strazdins.tool.TimeConverter;
import no.strazdins.transaction.Transaction;

/**
 * Writes report to a CSV file.
 */
public class ReportFileWriter {
  /**
   * Not allowed to create instances of this class.
   */
  private ReportFileWriter() {

  }

  /**
   * Write the report to a CSV file.
   *
   * @param report         The report content
   * @param outputFilePath Path to a CSV file where to store the result
   */
  public static void writeTransactionLogToFile(Report report, String outputFilePath)
      throws IOException {
    String[] header = new String[]{
        "Unix timestamp", "UTC time",
        "Transaction", "Asset",
        "Amount", "Price",
        "Quote currency", "Quote amount",
        "Fee", "Fee currency",
        "Fee in USDT", "Obtain price in USDT",
        "Transaction PNL in USDT", "Amount in Wallet",
        "Avg obtain price in USDT", "Running PNL in USDT"
    };
    CsvFileWriter writer = new CsvFileWriter(outputFilePath, header);
    for (WalletSnapshot snapshot : report) {
      long timestamp = snapshot.getTimestamp();
      Transaction t = snapshot.getTransaction();
      writer.writeRow(new String[]{
          "" + timestamp, TimeConverter.utcTimeToString(timestamp),
          t.getType(), t.getBaseCurrency(),
          t.getBaseCurrencyAmount().getNiceString(), t.getAvgPriceInUsdt().getNiceString(),
          t.getQuoteCurrency(), t.getQuoteAmount().getNiceString(),
          t.getFee().getNiceString(), t.getFeeCurrency(),
          t.getFeeInUsdt().getNiceString(), t.getObtainPrice().getNiceString(),
          t.getPnl().getNiceString(),
          snapshot.getBaseCurrencyAmountInWallet().getNiceString(),
          snapshot.getAvgBaseObtainPrice().getNiceString(), snapshot.getPnl().getNiceString()
      });
    }
    writer.close();
  }

  /**
   * Write wallet balances to a CSV file.
   *
   * @param report         The report containing the wallet snapshots
   * @param outputFilePath Path to the CSV file
   * @throws IOException When something went wrong while writing data to the file
   */
  public static void writeBalanceLogToFile(Report report, String outputFilePath)
      throws IOException {
    String[] header = new String[]{
        "Unix timestamp",
        "UTC time",
        "Balances: amount & asset & average obtain price (for each asset)"
    };
    CsvFileWriter writer = new CsvFileWriter(outputFilePath, header);
    writer.disableColumnCountChecking();
    for (WalletSnapshot snapshot : report) {
      long timestamp = snapshot.getTimestamp();
      int assetCount = snapshot.getWallet().getAssetCount();
      String[] columns = new String[2 + assetCount * 4];
      columns[0] = String.valueOf(timestamp);
      columns[1] = TimeConverter.utcTimeToString(timestamp);
      int i = 2;
      Wallet wallet = snapshot.getWallet();
      for (String asset : wallet) {
        columns[i++] = wallet.getAssetAmount(asset).getNiceString();
        columns[i++] = asset;
        columns[i++] = wallet.getAvgObtainPrice(asset).getNiceString();
        columns[i++] = "";
      }
      writer.writeRow(columns);
    }
    writer.close();
  }

  /**
   * Write annual reports to a CSV file.
   *
   * @param annualReports  List of annual reports, ordered chronologically
   * @param outputFilePath Path to the CSV file where to write the report
   * @param homeCurrency   Home currency of the user
   * @throws IOException When something goes wrong with writing the file
   */
  public static void writeAnnualReportsToFile(List<AnnualReport> annualReports,
                                              String outputFilePath,
                                              String homeCurrency) throws IOException {
    String[] header = new String[]{
        "Date",
        "Annual PNL in USD",
        homeCurrency + "/USD exchange rate",
        "Annual PNL in " + homeCurrency,
        "Held asset value in USD",
        "Held asset value in " + homeCurrency
    };
    CsvFileWriter writer = new CsvFileWriter(outputFilePath, header);
    for (AnnualReport report : annualReports) {
      writer.writeRow(new String[]{
          TimeConverter.utcTimeToDateString(report.timestamp()),
          report.pnlUsd().getNiceString(),
          report.exchangeRate().getNiceString(),
          report.pnlHc().getNiceString(),
          report.walletValueUsd().getNiceString(),
          report.walletValueHc().getNiceString()
      });
    }
    writer.close();
  }

  public static void writeExtraInfoToFile(ExtraInfo extraInfo, String outputFilePath)
      throws IOException {
    CsvFileWriter writer = new CsvFileWriter(outputFilePath);
    for (ExtraInfoEntry entry : extraInfo) {
      writer.writeRow(new String[] {
          String.valueOf(entry.utcTimestamp()),
          String.valueOf(entry.type()),
          entry.asset(),
          entry.value()
      });
    }
    writer.close();
  }
}
