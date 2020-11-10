package xyz.yooniks.aegis.auth.premium;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.UUID;
import net.md_5.bungee.BungeeCord;
import xyz.yooniks.aegis.auth.premium.PremiumManager.PremiumUser;

public final class UUIDHelper {

  private static final Gson GSON = new Gson();

  private UUIDHelper() {
  }

  public static PremiumUser requestPremiumUser(String playerName, UUID defaultUUID) {
    //System.out.println("[AegisAuth] Looking for premium user data of " + playerName + "!");

    PremiumUser user;
    try {
      final String output = callURL(
          "https://api.mojang.com/users/profiles/minecraft/" + playerName);

      //StringBuilder result = new StringBuilder();

      //readData(output, result);

      final MojangSession session = GSON.fromJson(output, MojangSession.class);

      StringBuilder uuid = new StringBuilder();

      for (int i = 0; i <= 31; i++) {
        uuid.append(session.getId().charAt(i));
        if (i == 7 || i == 11 || i == 15 || i == 19) {
          uuid.append("-");
        }
      }

      BungeeCord.getInstance().getLogger()
          .info("[Aegis Auth] Player " + playerName + " is premium player!");
      final UUID newUUID = UUID.fromString(uuid.toString());
      PremiumManager.putUser(playerName, user = new PremiumUser(true, newUUID));
      return user;
    } catch (Exception ex) {
      BungeeCord.getInstance().getLogger().info(
          "[Aegis Auth] Player " + playerName + " is non-premium player! (" + ex.getMessage()
              + ")");
      PremiumManager.putUser(playerName, user = new PremiumUser(false, defaultUUID));
      return user;
    }
  }

  private static void readData(String toRead, StringBuilder result) {
    int i = 7;

    while (i < 200) {
      if (!String.valueOf(toRead.charAt(i)).equalsIgnoreCase("\"")) {

        result.append(toRead.charAt(i));

      } else {
        break;
      }

      i++;
    }
  }

  private static String callURL(String urlName) throws Exception {
    final StringBuilder sb = new StringBuilder();

    InputStreamReader in;

    final URL url = new URL(urlName);
    final URLConnection urlConn = url.openConnection();

    if (urlConn != null) {
      urlConn.setReadTimeout(60 * 1000);
    }

    if (urlConn != null && urlConn.getInputStream() != null) {
      in = new InputStreamReader(urlConn.getInputStream(), Charset.defaultCharset());
      BufferedReader bufferedReader = new BufferedReader(in);

      int cp;

      while ((cp = bufferedReader.read()) != -1) {
        sb.append((char) cp);
      }

      bufferedReader.close();
      in.close();
    }

    return sb.toString();
  }

}
