import static org.junit.jupiter.api.Assertions.assertEquals;

import no.strazdins.tool.TimeConverter;
import org.junit.jupiter.api.Test;

class TimeConverterTest {
  @Test
  void testTimestampConversion() {
    assertEquals(1571834540000L, TimeConverter.stringToUtcTimestamp("2019-10-23 12:42:20"));
    assertEquals(1571846455000L, TimeConverter.stringToUtcTimestamp("2019-10-23 16:00:55"));
  }

  @Test
  void testYear() {
    assertEquals(2023, TimeConverter.getUtcYear(1681052698000L));
    assertEquals(2023, TimeConverter.getUtcYear(1672531200000L));
    assertEquals(2022, TimeConverter.getUtcYear(1672531200000L - 1L));
    assertEquals(2022, TimeConverter.getUtcYear(1649509477000L));
    assertEquals(1970, TimeConverter.getUtcYear(0));
  }

  @Test
  void testDayStart() {
    dayStartTest("2023-04-16 10:36:32");
    dayStartTest("2023-04-16 00:00:00");
    dayStartTest("2023-04-16 00:00:01");
    dayStartTest("2023-04-16 23:59:59");
    dayStartTest("2023-04-16 23:59:58");
  }

  private void dayStartTest(String timeString) {
    String[] parts = timeString.split(" ");
    String day = parts[0];
    long originalTimestamp = TimeConverter.stringToUtcTimestamp(timeString);
    long expectedTimestamp = TimeConverter.stringToUtcTimestamp(day + " 00:00:00");
    assertEquals(expectedTimestamp, TimeConverter.getDayStart(originalTimestamp));
  }
}
