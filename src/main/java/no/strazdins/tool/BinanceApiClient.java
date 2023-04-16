package no.strazdins.tool;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.List;
import no.strazdins.data.Decimal;

/**
 * Handles Binance REST API.
 */
public class BinanceApiClient {
  private static final String API_BASE_URL = "https://api.binance.com/api/v3";

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
    String requestUrl = "/klines?symbol=" + (asset + "USDT")
        + "&limit=1&interval=1d&startTime=" + TimeConverter.getDayStart(timestamp);
    List<List<Object>> rawResponse = client.get(requestUrl,
        new TypeToken<List<List<Object>>>() {
        }.getType());
    return getClosePriceFromSingleCandleArray(rawResponse);
  }

  private Decimal getClosePriceFromSingleCandleArray(List<List<Object>> rawResponse) {
    if (rawResponse.size() != 1 || rawResponse.get(0).size() != 12
        || !(rawResponse.get(0).get(4) instanceof String)) {
      throw new IllegalStateException("Unexpected response received from daily candle REST API: "
          + gson.toJson(rawResponse));
    }
    return new Decimal((String) rawResponse.get(0).get(4));
  }
}
