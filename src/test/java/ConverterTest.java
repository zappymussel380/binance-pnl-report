import static org.junit.jupiter.api.Assertions.assertEquals;

import no.strazdins.tool.TimeConverter;
import org.junit.jupiter.api.Test;

class ConverterTest {
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
}
