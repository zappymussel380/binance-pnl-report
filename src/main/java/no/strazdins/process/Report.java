package no.strazdins.process;

import java.util.LinkedList;
import java.util.List;
import no.strazdins.data.ExtraInfo;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.Wallet;
import no.strazdins.data.WalletSnapshot;
import no.strazdins.transaction.Transaction;

/**
 * Profit-and-loss report.
 */
public class Report {
  private final ExtraInfo extraInfo;
  private final List<WalletSnapshot> walletSnapshots = new LinkedList<>();
  private WalletSnapshot currentWalletSnapshot;

  public Report(ExtraInfo extraInfo) {
    this.extraInfo = extraInfo;
    this.currentWalletSnapshot = createEmptyWalletSnapshot();
  }

  private static WalletSnapshot createEmptyWalletSnapshot() {
    return new WalletSnapshot(null, new Wallet(), "0");
  }

  public void process(Transaction transaction) {
    currentWalletSnapshot = transaction.process(currentWalletSnapshot, getExtraInfo(transaction));
    walletSnapshots.add(currentWalletSnapshot);
  }

  private ExtraInfoEntry getExtraInfo(Transaction transaction) {
    return extraInfo.getAtTime(transaction.getUtcTime());
  }
}
