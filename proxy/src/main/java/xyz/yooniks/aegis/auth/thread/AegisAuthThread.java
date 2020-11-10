package xyz.yooniks.aegis.auth.thread;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.protocol.ProtocolConstants;
import xyz.yooniks.aegis.auth.handler.AuthConnector;
import xyz.yooniks.aegis.auth.handler.AuthConnector.AuthMessage;
import xyz.yooniks.aegis.auth.user.AuthUser;
import xyz.yooniks.aegis.caching.PacketUtils;
import xyz.yooniks.aegis.caching.PacketUtils.KickType;
import xyz.yooniks.aegis.caching.PacketsPosition;

public class AegisAuthThread {

  private static final Set<String> TO_REMOVE_SET = new HashSet<>();
  private static Thread thread;
  private static BungeeCord bungee = BungeeCord.getInstance();


  public static void start() {
    (thread = new Thread(() ->
    {
      while (!Thread.currentThread().isInterrupted() && sleep(1000)) {
        try {
          final long currTime = System.currentTimeMillis();
          for (Map.Entry<String, AuthConnector> entryset : bungee.getAegis()
              .getConnectedAuthUsersSet().entrySet()) {
            final AuthConnector connector = entryset.getValue();
            if (!connector.isConnected()
                || connector.getVersion() < ProtocolConstants.MINECRAFT_1_8) {
              TO_REMOVE_SET.add(entryset.getKey());
              continue;
            }

            if ((currTime - connector.getJoinTime()) >= 28000) {
              connector.failed(KickType.TOO_LONG_LOGGING);
              TO_REMOVE_SET.add(entryset.getKey());
              continue;
            }

            final AuthUser user = connector.getUser();
            final Channel channel = connector.getChannel();

            if (user == null) {
              channel.writeAndFlush(PacketUtils
                  .getCachedPacket(PacketsPosition.LOADING_MESSAGE)
                  .get(connector.getVersion()));
            } else {
              final int time = Math.abs(
                  (int) ((System.currentTimeMillis() - connector.getJoinTime()) / 1000.0));

              ByteBuf expBuf = PacketUtils.expPackets.getAuth(time, connector.getVersion());
              if (expBuf != null) {
                channel.writeAndFlush(expBuf, channel.voidPromise());
              }

              final AuthMessage message = connector.getAuthMessage();
              if (message == AuthMessage.PREMIUM) {
                channel.writeAndFlush(PacketUtils
                    .getCachedPacket(PacketsPosition.PREMIUM_MESSAGE)
                    .get(connector.getVersion()));
                connector.completeLogin();
                continue;
              } else if (message == AuthMessage.ASK_IF_PREMIUM) {
                channel.writeAndFlush(PacketUtils
                    .getCachedPacket(PacketsPosition.ASK_IF_PREMIUM_CHAT)
                    .get(connector.getVersion()));
                PacketUtils.titles[8].writeTitle(channel, connector.getVersion());
              } else if (message == AuthMessage.LOADED) {
                channel.writeAndFlush(PacketUtils
                    .getCachedPacket(PacketsPosition.LOADED_MESSAGE)
                    .get(connector.getVersion()));
                if (user.isRegistered()) {
                  connector.setAuthMessage(AuthMessage.LOGIN);
                } else {
                  connector.setAuthMessage(AuthMessage.REGISTER);
                }
              } else if (message == AuthMessage.REGISTER) {
                channel.writeAndFlush(PacketUtils
                    .getCachedPacket(PacketsPosition.REGISTER_MESSAGE)
                    .get(connector.getVersion()));
              } else if (message == AuthMessage.LOGIN) {
                channel.writeAndFlush(PacketUtils
                    .getCachedPacket(PacketsPosition.LOGIN_MESSAGE)
                    .get(connector.getVersion()));
              }
            }

            connector.sendPing();
          }
        } catch (Exception e) {
          bungee.getLogger().log(Level.WARNING,
              "[Aegis] An exception is occurred. Please report it to the developer!", e);
        } finally {
          if (!TO_REMOVE_SET.isEmpty()) {
            for (String remove : TO_REMOVE_SET) {
              bungee.getAegis().removeAuthConnection(remove, null);
            }
            TO_REMOVE_SET.clear();
          }
        }
      }

    }, "Aegis Auth thread")).start();
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
