package no.strazdins.tool;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Sends HTTP requests to REST APIs. Transforms response JSONs into objects.
 */
public class RestApiClient {
  private static final Logger logger = LogManager.getLogger(RestApiClient.class);

  private final String apiBaseUrl;

  private final Gson gson = new Gson();

  /**
   * Create REST API client.
   *
   * @param baseUrl The base URL of the API, will be prepended to all request URLs
   */
  public RestApiClient(String baseUrl) {
    this.apiBaseUrl = baseUrl;
  }

  /**
   * Send an HTTP GET to a REST API endpoint.
   *
   * @param apiPath       The relative API path (API BASE URL will be prepended)
   * @param responseClass The response will be parsed to an object of the given class
   * @param <T>           Generic return type
   * @return The response JSON will be parsed to an object of type T. Null returned on error
   */
  public <T> T get(String apiPath, Type responseClass) {
    String responseJson = fetch("GET", apiPath);
    if (responseJson == null) {
      return null;
    }

    return gson.fromJson(responseJson, responseClass);
  }

  private String fetch(String method, String apiPath) {
    HttpURLConnection connection = establishConnection(method, apiPath);
    String response = null;
    if (connection != null) {
      response = readResponseBody(connection);
    }

    return response;
  }

  private HttpURLConnection establishConnection(String method, String apiPath) {
    final String apiUrl = apiBaseUrl + apiPath;
    logger.info("HTTP {} {}", method, apiUrl);
    HttpURLConnection connection;
    try {
      URL url = new URL(apiUrl);
      connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod(method);
    } catch (MalformedURLException e) {
      logger.error("Wrong API URL: {}", apiUrl);
      connection = null;
    } catch (ProtocolException e) {
      logger.error("Wrong HTTP request method: {}", method);
      connection = null;
    } catch (IOException e) {
      logger.error("Could not establish connection to API: {}", e.getMessage());
      connection = null;
    }

    return connection;
  }

  private static String readResponseBody(HttpURLConnection connection) {
    StringBuilder response = new StringBuilder();
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }
      in.close();
    } catch (IOException e) {
      logger.error("Error while reading HTTP response: {}", e.getMessage());
      response = new StringBuilder();
    }

    return response.toString();
  }
}
