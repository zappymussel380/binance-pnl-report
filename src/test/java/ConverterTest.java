import org.compilers.tool.Converter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConverterTest {
  @Test
  void testTimestampConversion() {
    assertEquals(1571834540000L, Converter.stringToUtcTimestamp("2019-10-23 12:42:20"));
    assertEquals(1571846455000L, Converter.stringToUtcTimestamp("2019-10-23 16:00:55"));
  }
}
