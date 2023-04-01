package no.strazdins.data;

import no.strazdins.transaction.Transaction;

/**
 * A snapshot of the wallet.
 */
public class WalletSnapshot {
  private Transaction transaction;
  private Wallet wallet;
  private Decimal pnl;

  /**
   * Create a wallet snapshot.
   *
   * @param transaction The transaction after which this snapshot is created
   * @param pnl         Total running Profit & Loss (PNL) accumulated so far
   */
  public WalletSnapshot(Transaction transaction, Decimal pnl) {
    this.transaction = transaction;
    this.wallet = new Wallet();
    this.pnl = pnl;
  }

  /**
   * Create a snapshot of an empty wallet.
   *
   * @return An empty-wallet snapshot
   */
  public static WalletSnapshot createEmpty() {
    return new WalletSnapshot(null, Decimal.ZERO);
  }

  /**
   * Create a new wallet transaction which has the same data as this, and is ready to be
   * used as a template for "snapshot after transaction t".
   *
   * @param transaction The transaction for which the new snapshot will be created
   * @return A snapshot - copy of the current one, with the given transaction
   */
  public WalletSnapshot prepareForTransaction(Transaction transaction) {
    WalletSnapshot ws = new WalletSnapshot(transaction, new Decimal(pnl));
    ws.wallet = this.wallet.clone();
    return ws;
  }

  public void addAsset(String asset, Decimal depositAmount, Decimal obtainPrice) {
    wallet.addAsset(asset, depositAmount, obtainPrice);
  }

  @Override
  public String toString() {
    return wallet.getAssetCount() + " assets, PNL=" + pnl.getNiceString()
        + (transaction != null ? (" after " + transaction) : "");
  }

  public Wallet getWallet() {
    return wallet;
  }

  public Decimal getPnl() {
    return pnl;
  }
}
