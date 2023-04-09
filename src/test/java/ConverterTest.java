import no.strazdins.tool.Converter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConverterTest {
  @Test
  void testTimestampConversion() {
    assertEquals(1571834540000L, Converter.stringToUtcTimestamp("2019-10-23 12:42:20"));
    assertEquals(1571846455000L, Converter.stringToUtcTimestamp("2019-10-23 16:00:55"));
  }

  @Test
  void testYear() {
    assertEquals(2023, Converter.getUtcYear(1681052698000L));
    assertEquals(2023, Converter.getUtcYear(1672531200000L));
    assertEquals(2022, Converter.getUtcYear(1672531200000L - 1L));
    assertEquals(2022, Converter.getUtcYear(1649509477000L));
    assertEquals(1970, Converter.getUtcYear(0));
  }
}
