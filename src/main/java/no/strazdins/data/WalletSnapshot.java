package no.strazdins.data;

import no.strazdins.transaction.Transaction;

/**
 * Snapshot of the wallet at a specific time moment, after processing a given transaction.
 *
 * @param transaction The transaction after which the snapshot was taken
 * @param wallet      The wallet containing all the assets
 * @param pnl         Profit & Loss (PNL) - the current running total PNL for all
 *                    transactions so far
 */
public record WalletSnapshot(Transaction transaction, Wallet wallet, String pnl) {
  public WalletSnapshot(WalletSnapshot walletSnapshot) {
    this(walletSnapshot.transaction, walletSnapshot.wallet, walletSnapshot.pnl);
  }

  public void addAsset(String asset, String depositAmount, String obtainPrice) {
    wallet.addAsset(asset, depositAmount, obtainPrice);
  }
}
