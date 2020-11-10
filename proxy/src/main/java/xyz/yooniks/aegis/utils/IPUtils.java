package xyz.yooniks.aegis.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.netty.ChannelWrapper;

/**
 * @author Leymooo
 */
public class IPUtils {

  public static InetAddress getAddress(UserConnection userCon) {
    return userCon.getAddress().getAddress();
  }

  public static InetAddress getAddress(ChannelWrapper wrapper) {
    return wrapper.
        getRemoteAddress().
        getAddress();
  }

  public static InetAddress getAddress(String ip) {
    try {
      return InetAddress.getByName(ip);
    } catch (UnknownHostException ex) {
      BungeeCord.getInstance().getLogger()
          .log(Level.WARNING, "[Aegis] Could not get InetAddress for " + ip, ex);
    }
    return null;
  }

}
