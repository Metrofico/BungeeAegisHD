package xyz.yooniks.aegis.filter;

import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.protocol.ProtocolConstants;
import xyz.yooniks.aegis.Aegis;
import xyz.yooniks.aegis.Aegis.CheckState;
import xyz.yooniks.aegis.caching.PacketUtils;
import xyz.yooniks.aegis.caching.PacketUtils.KickType;
import xyz.yooniks.aegis.caching.PacketsPosition;
import xyz.yooniks.aegis.config.Settings;
import xyz.yooniks.aegis.utils.ManyChecksUtils;

/**
 * @author Leymooo
 */
public class AegisThread {

  private static final HashSet<String> TO_REMOVE_SET = new HashSet<>();
  private static Thread thread;
  private static final BungeeCord bungee = BungeeCord.getInstance();


  public static void start() {
    (thread = new Thread(() ->
    {
      while (!Thread.currentThread().isInterrupted() && sleep(1000)) {
        try {
          long currTime = System.currentTimeMillis();
            if (bungee == null || bungee.getAegis() == null) {
                continue;
            }

          for (Map.Entry<String, Connector> entryset : bungee.getAegis().getConnectedUsersSet()
              .entrySet()) {
            Connector connector = entryset.getValue();
            if (!connector.isConnected()
                || connector.getVersion() < ProtocolConstants.MINECRAFT_1_8) {
              TO_REMOVE_SET.add(entryset.getKey());
              continue;
            }
            CheckState state = connector.getState();
            switch (state) {
              case SUCCESSFULLY:
              case FAILED:
                TO_REMOVE_SET.add(entryset.getKey());
                continue;
              default:
                if ((currTime - connector.getJoinTime()) >= Settings.IMP.TIME_OUT) {

                  if (bungee.getAegis().getCaptchaFailed() != null) {
                      if (connector.getUserConnection() != null
                          && connector.getUserConnection().getAddress() != null) {
                          bungee.getAegis().getCaptchaFailed()
                              .failed(connector.getUserConnection().getAddress()
                                  .getAddress().getHostAddress());
                      }
                  }

                  connector.failed(KickType.NOTPLAYER,
                      state == CheckState.CAPTCHA_ON_POSITION_FAILED
                          ? "Too long fall check" : "Captcha not entered");
                  TO_REMOVE_SET.add(entryset.getKey());

                  continue;
                } else if (state == CheckState.CAPTCHA_ON_POSITION_FAILED
                    || state == CheckState.ONLY_POSITION) {
                  connector.getChannel().writeAndFlush(PacketUtils.getCachedPacket(
                      PacketsPosition.CHECKING).get(connector.getVersion()));
                } else {
                  connector.getChannel().writeAndFlush(PacketUtils
                      .getCachedPacket(PacketsPosition.CHECKING_CAPTCHA)
                      .get(connector.getVersion()));
                }
                connector.sendPing();
            }
          }

        } catch (Exception e) {
          bungee.getLogger().log(Level.WARNING,
              "[Aegis] An exception is occurred. Please report it to the developer!", e);
        } finally {
          if (!TO_REMOVE_SET.isEmpty()) {
            for (String remove : TO_REMOVE_SET) {
              if (bungee != null) {
                bungee.getAegis().removeConnection(remove, null);
              }
            }
            TO_REMOVE_SET.clear();
          }
        }
      }

    }, "Aegis thread")).start();
  }

  public static void stop() {
    if (thread != null) {
      thread.interrupt();
    }
  }

  private static boolean sleep(long time) {
    try {
      Thread.sleep(time);
    } catch (InterruptedException ex) {
      return false;
    }
    return true;
  }

  public static void startCleanUpThread() {
    new Thread(() ->
    {
      while (!Thread.interrupted() && sleep(Aegis.ONE_MIN)) {
        ManyChecksUtils.cleanUP();
        /*if (bungee.getConnectionThrottle() != null) {
          bungee.getConnectionThrottle().cleanUP();
        }*/
        if (bungee.getAegis() != null) {
          Aegis aegis = bungee.getAegis();
          if (aegis.getSql() != null) {
            aegis.getSql().tryCleanUP();
          }
          if (aegis.getGeoIp() != null) {
            aegis.getGeoIp().tryClenUP();
          }
        }
      }
    }, "CleanUp thread").start();

  }
}
