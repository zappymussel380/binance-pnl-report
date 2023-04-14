import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedList;
import java.util.List;
import no.strazdins.data.Decimal;
import no.strazdins.data.WalletSnapshot;
import no.strazdins.tool.ReportHelper;
import no.strazdins.tool.TimeConverter;
import no.strazdins.transaction.Transaction;
import org.junit.jupiter.api.Test;

class ReportHelperTest {
  @Test
  void testYearEndFiltering() {
    List<WalletSnapshot> snapshots = new LinkedList<>();
    addSnapshots(snapshots, new String[]{"2021-12-10", "2021-12-12",
        "2022-02-03", "2022-02-04", "2022-02-05",
        "2023-02-03", "2023-02-08"});
    List<WalletSnapshot> expectedSnapshots = new LinkedList<>();
    addSnapshots(expectedSnapshots, new String[]{"2021-12-12", "2022-02-05", "2023-02-08"});
    List<WalletSnapshot> yearEndSnapshots = ReportHelper.filterYearEndSnapshots(snapshots);
    assertEquals(expectedSnapshots.size(), yearEndSnapshots.size());
    for (int i = 0; i < expectedSnapshots.size(); ++i) {
      assertEquals(expectedSnapshots.get(i), yearEndSnapshots.get(i));
    }
  }

  private void addSnapshots(List<WalletSnapshot> snapshots, String[] dates) {
    for (String date : dates) {
      snapshots.add(new WalletSnapshot(new Transaction(createTimestamp(date)), Decimal.ZERO));
    }
  }

  private long createTimestamp(String date) {
    return TimeConverter.stringToUtcTimestamp(date + " 12:20:00");
  }
}
