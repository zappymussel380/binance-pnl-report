package no.strazdins;

import no.strazdins.data.ExtraInfo;
import no.strazdins.data.Transaction;
import no.strazdins.data.Wallet;

/**
 * Profit-and-loss report.
 */
public class Report {
  private final ExtraInfo extraInfo;
  private final Wallet wallet;

  public Report(ExtraInfo extraInfo) {
    this.extraInfo = extraInfo;
    this.wallet = new Wallet();
  }

  public void process(Transaction transaction) {
    // TODO
    throw new UnsupportedOperationException();
  }
}
