package xyz.yooniks.aegis.vpn;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import net.md_5.bungee.BungeeCord;
import xyz.yooniks.aegis.caching.PacketUtils;
import xyz.yooniks.aegis.caching.PacketUtils.KickType;
import xyz.yooniks.aegis.caching.PacketsPosition;

public class VPNThread {

  private static final Set<String> TO_REMOVE_SET = new HashSet<>();
  private static Thread thread;
  private static BungeeCord bungee = BungeeCord.getInstance();


  public static void start() {
    (thread = new Thread(() ->
    {
      while (!Thread.currentThread().isInterrupted() && sleep(1000)) {
        try {
          if (bungee == null || bungee.getAegis() == null) {
            continue;
          }
          final long currTime = System.currentTimeMillis();
          for (Map.Entry<String, VPNConnector> entryset : bungee.getAegis()
              .getConnectedVPNUsersSet().entrySet()) {
            final VPNConnector connector = entryset.getValue();
            if (!connector.isConnected()) {
              TO_REMOVE_SET.add(entryset.getKey());
              continue;
            }

            if ((currTime - connector.getJoinTime()) >= 28000) {
              connector.failed(KickType.TOO_LONG_VERIFICATION);
              TO_REMOVE_SET.add(entryset.getKey());
              continue;
            }

            final Channel channel = connector.getChannel();

            final int time = Math.abs(
                (int) ((System.currentTimeMillis() - connector.getJoinTime()) / 1000.0));

            ByteBuf expBuf = PacketUtils.expPackets.getAuth(time, connector.getVersion());
            if (expBuf != null) {
              channel.writeAndFlush(expBuf, channel.voidPromise());
            }

            if (!connector.isChecked()) {
              channel.writeAndFlush(PacketUtils
                  .getCachedPacket(PacketsPosition.ANTIVPN_CHECKING)
                  .get(connector.getVersion()));
            }

            //connector.getUserConnection().unsafe().sendPacket(new Title();

            connector.sendPing();
          }
        } catch (Exception e) {
          bungee.getLogger().log(Level.WARNING,
              "[Aegis] An exception is occurred. Please report it to the developer!", e);
        } finally {
          if (!TO_REMOVE_SET.isEmpty()) {
            for (String remove : TO_REMOVE_SET) {
              bungee.getAegis().removeVPNConnection(remove, null);
            }
            TO_REMOVE_SET.clear();
          }
        }
      }

    }, "Aegis VPN thread")).start();
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


}
