package no.strazdins.process;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import no.strazdins.data.Decimal;
import no.strazdins.data.ExtraInfo;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.WalletSnapshot;
import no.strazdins.tool.ReportHelper;
import no.strazdins.tool.TimeConverter;
import no.strazdins.transaction.Transaction;

/**
 * Profit-and-loss report.
 */
public class Report implements Iterable<WalletSnapshot> {
  private final ExtraInfo extraInfo;
  private final List<WalletSnapshot> walletSnapshots = new LinkedList<>();
  private WalletSnapshot currentWalletSnapshot;

  public Report(ExtraInfo extraInfo) {
    this.extraInfo = extraInfo;
    this.currentWalletSnapshot = WalletSnapshot.createEmpty();
  }

  public void process(Transaction transaction) {
    currentWalletSnapshot = transaction.process(currentWalletSnapshot, getExtraInfo(transaction));
    walletSnapshots.add(currentWalletSnapshot);
  }

  private ExtraInfoEntry getExtraInfo(Transaction transaction) {
    return extraInfo.getAtTime(transaction.getUtcTime());
  }

  @Override
  public Iterator<WalletSnapshot> iterator() {
    return walletSnapshots.iterator();
  }

  public List<AnnualReport> createAnnualReports() {
    List<WalletSnapshot> yearEndSnapshots = getYearEndSnapshots();
    return yearEndSnapshots.stream().map(this::createYearEndReport).toList();
  }

  private List<WalletSnapshot> getYearEndSnapshots() {
    return ReportHelper.filterYearEndSnapshots(walletSnapshots);
  }

  private AnnualReport createYearEndReport(WalletSnapshot snapshot) {
    long yearEndTimestamp = TimeConverter.getYearEndTimestamp(snapshot.getYear());
    Decimal exchangeRate = getExchangeRateAt(yearEndTimestamp);
    Decimal pnlUsd = snapshot.getPnl();
    Decimal pnlHc = pnlUsd.multiply(exchangeRate);
    Decimal walletValueUsd = snapshot.getWallet().getTotalValueAt(yearEndTimestamp, extraInfo);
    Decimal walletValueHc = walletValueUsd.multiply(exchangeRate);
    return new AnnualReport(yearEndTimestamp, pnlUsd, exchangeRate, pnlHc,
        walletValueUsd, walletValueHc);
  }

  private Decimal getExchangeRateAt(long timestamp) {
    ExtraInfoEntry ei = extraInfo.getAtTime(timestamp);
    if (ei == null) {
      throw new IllegalStateException("Did not find exchange rate at "
          + TimeConverter.utcTimeToString(timestamp));
    }
    return new Decimal(ei.value());
  }
}
