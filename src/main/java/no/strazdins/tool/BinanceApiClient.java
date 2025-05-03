package no.strazdins.tool;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.List;
import no.strazdins.data.Decimal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles Binance REST API.
 */
public class BinanceApiClient {
  private static final Logger logger = LogManager.getLogger(BinanceApiClient.class);
  private static final String API_BASE_URL = "https://api.binance.com/api/v3";
  private static final long DELAY_AFTER_REQUEST_MS = 500;
  private static final String DEFAULT_QUOTE_CURR = "USDC";

  private final RestApiClient client = new RestApiClient(API_BASE_URL);

  private final Gson gson = new Gson();

  /**
   * Get daily close price for a given asset, in the USDT market.
   *
   * @param asset     The asset of interest
   * @param timestamp Timestamp of interest. Must be somewhere "inside the requested day"
   * @return The daily close price of the requested price candle, or null if not found
   */
  public Decimal getDailyClosePrice(String asset, long timestamp) {
    String requestUrl = "/klines?symbol=" + (asset + DEFAULT_QUOTE_CURR)
        + "&limit=1&interval=1d&startTime=" + TimeConverter.getDayStart(timestamp);
    List<List<Object>> rawResponse = client.get(requestUrl,
        new TypeToken<List<List<Object>>>() {
        }.getType());
    sleepToAvoidRateLimitBan();
    return getClosePriceFromSingleCandleArray(rawResponse);
  }

  private void sleepToAvoidRateLimitBan() {
    try {
      Thread.sleep(DELAY_AFTER_REQUEST_MS);
    } catch (InterruptedException e) {
      logger.error("Interrupted while sleeping between REST API calls");
      Thread.currentThread().interrupt();
    }
  }

  private Decimal getClosePriceFromSingleCandleArray(List<List<Object>> rawResponse) {
    if (rawResponse == null || rawResponse.size() != 1 || rawResponse.get(0).size() != 12
        || !(rawResponse.get(0).get(4) instanceof String)) {
      String json = gson.toJson(rawResponse);
      logger.error("Unexpected response received from daily candle REST API: {}", json);
      logger.error("Assuming the coin price was zero (that is the best guess we can make)");
      logger.error("If you know a better price, specify it manually in the extra info file");
      return Decimal.ZERO;
    }
    return new Decimal((String) rawResponse.get(0).get(4));
  }
}
