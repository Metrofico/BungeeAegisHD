package xyz.yooniks.aegis.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public final class URLHelper {

  private URLHelper() {
  }

  public static String readContent(URL url) throws Exception {
    final StringBuilder content = new StringBuilder();

    final URLConnection urlConnection = url.openConnection();
    urlConnection.setConnectTimeout(7500);
    urlConnection.setReadTimeout(7500);

    final BufferedReader bufferedReader = new BufferedReader(
        new InputStreamReader(urlConnection.getInputStream()));

    String line;

    while ((line = bufferedReader.readLine()) != null) {
      content.append(line);
    }
    bufferedReader.close();

    return content.toString();
  }

}
