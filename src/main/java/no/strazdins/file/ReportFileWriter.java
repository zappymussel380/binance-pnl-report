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
          "" + timestamp, Converter.utcTimeToString(timestamp),
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
}
