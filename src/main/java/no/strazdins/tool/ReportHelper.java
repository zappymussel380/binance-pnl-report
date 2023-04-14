package no.strazdins.tool;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import no.strazdins.data.WalletSnapshot;

/**
 * Helper class for some report-related processing.
 */
public class ReportHelper {
  /**
   * Prohibit instantiating the class.
   */
  private ReportHelper() {
  }

  /**
   * Go through the list of provided snapshots, leave only the "last snapshot each year".
   *
   * @param snapshots Wallet snapshots which will be filtered
   * @return List of snapshots, only the last snapshot each year is kept
   */
  public static List<WalletSnapshot> filterYearEndSnapshots(List<WalletSnapshot> snapshots) {
    List<WalletSnapshot> yearEndSnapshots = new LinkedList<>();
    ListIterator<WalletSnapshot> reverseIterator = snapshots.listIterator(snapshots.size());
    int yearOfNextSnapshot = -1;
    while (reverseIterator.hasPrevious()) {
      WalletSnapshot snapshot = reverseIterator.previous();
      int snapshotYear = snapshot.getYear();
      if (snapshotYear != yearOfNextSnapshot) {
        yearEndSnapshots.add(0, snapshot);
      }
      yearOfNextSnapshot = snapshotYear;
    }
    return yearEndSnapshots;
  }
}
