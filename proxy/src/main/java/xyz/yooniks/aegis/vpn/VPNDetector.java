package xyz.yooniks.aegis.vpn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public interface VPNDetector {

  boolean isBad(String address) throws IOException;

  int getLimit();

  int count();

  void clearCount();

  boolean isLimitable();

  String getName();

  default String query(String url, int timeout) throws IOException {
    final StringBuilder response = new StringBuilder();
    final URL website = new URL(url);
    final HttpURLConnection connection = (HttpURLConnection) website.openConnection();
    connection.setConnectTimeout(timeout);
    connection.addRequestProperty("User-Agent", "Aegis Minecraft Plugin");

    if (connection.getResponseCode() == 429) {
      throw new IOException("Too many requests");
    }

    try (final BufferedReader in = new BufferedReader(
        new InputStreamReader(
            connection.getInputStream()))) {

      while ((url = in.readLine()) != null) {
        response.append(url);
      }

    }

    return response.toString();
  }

}