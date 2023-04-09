import no.strazdins.process.ExtraInfoHandler;
import no.strazdins.transaction.Transaction;
import org.junit.jupiter.api.Test;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtraInfoHandlerTest {
  @Test
  void testTransactionYears() {
    List<Transaction> transactions = new LinkedList<>();
    transactions.add(new Transaction(1681046729000L));
    transactions.add(new Transaction(1647181529000L));
    transactions.add(new Transaction(1563035131980L));
    Set<Integer> years = ExtraInfoHandler.getTransactionYears(transactions);
    assertEquals(3, years.size());
    assertTrue(years.contains(2019));
    assertTrue(years.contains(2022));
    assertTrue(years.contains(2023));
  }
}
