package no.strazdins.file;

import java.io.IOException;
import no.strazdins.data.WalletSnapshot;
import no.strazdins.process.Report;
import no.strazdins.tool.Converter;
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
   * @param homeCurrency   The Home currency in which the profit and obtain prices are calculated
   */
  public static void writeReportToFile(Report report, String outputFilePath, String homeCurrency)
      throws IOException {
    String[] header = new String[]{
        "Unix timestamp", "UTC time",
        "Transaction", "Asset",
        "Amount", "Quote currency",
        "Fee", "Fee currency",
        "Fee in " + homeCurrency, "Obtain price in " + homeCurrency,
        "PNL in " + homeCurrency, "Amount in Wallet",
        "Avg obtain price in " + homeCurrency, "Running PNL in " + homeCurrency
    };
    CsvFileWriter writer = new CsvFileWriter(outputFilePath, header);
    for (WalletSnapshot snapshot : report) {
      long timestamp = snapshot.getTimestamp();
      Transaction t = snapshot.getTransaction();
      writer.writeRow(new String[]{
          "" + timestamp, Converter.utcTimeToString(timestamp),
          t.getType(), t.getBaseCurrency(),
          t.getBaseCurrencyAmount().getNiceString(), t.getQuoteCurrency(),
          t.getFee().getNiceString(), t.getFeeCurrency(),
          t.getFeeInHomeCurrency().getNiceString(), t.getObtainPrice().getNiceString(),
          t.getPnl().getNiceString(), snapshot.getBaseCurrencyAmountInWallet().getNiceString(),
          snapshot.getAvgBaseObtainPrice().getNiceString(), snapshot.getPnl().getNiceString()
      });
    }
    writer.close();
  }
}
