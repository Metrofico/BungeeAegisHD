package xyz.yooniks.aegis.vpn;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.Util;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.packet.Chat;
import net.md_5.bungee.protocol.packet.ClientSettings;
import net.md_5.bungee.protocol.packet.KeepAlive;
import net.md_5.bungee.protocol.packet.PluginMessage;
import xyz.yooniks.aegis.Aegis;
import xyz.yooniks.aegis.auth.handler.AuthConnector;
import xyz.yooniks.aegis.caching.PacketUtils;
import xyz.yooniks.aegis.caching.PacketUtils.KickType;
import xyz.yooniks.aegis.caching.PacketsPosition;
import xyz.yooniks.aegis.config.Settings;
import xyz.yooniks.aegis.filter.MoveHandler;
import xyz.yooniks.aegis.filter.SimpleConnector;

/**
 * @author Leymooo
 */
@EqualsAndHashCode(callSuper = false, of =
    {
        "name"
    })
public class VPNConnector extends MoveHandler implements SimpleConnector {

  private static final Logger LOGGER = BungeeCord.getInstance().getLogger();

  //bylo 100
  public static int TOTAL_TICKS = 290;
  //bylo *50
  private static long TOTAL_TIME = (TOTAL_TICKS * 150) - 100; //TICKS * 50MS

  private final Aegis aegis;
  private final String name;
  private final int version;
  @Getter
  private UserConnection userConnection;
  @Getter
  private Channel channel;

  @Getter
  private long joinTime = System.currentTimeMillis();
  private long lastSend = 0, totalping = 9999;

  private boolean markDisconnected = false;

  @Getter
  @Setter
  private boolean checked = false;

  public VPNConnector(UserConnection userConnection, Aegis aegis, boolean displayWorld) {
    this(userConnection, aegis, displayWorld, 0, 0);
  }

  public VPNConnector(UserConnection userConnection, Aegis aegis, boolean displayWorld,
      int aticks, int ticks) {
    Preconditions.checkNotNull(aegis, "Aegis instance is null");
    this.ticks = ticks;
    this.aegis = aegis;
    this.name = userConnection.getName();
    this.channel = userConnection.getCh().getHandle();
    this.userConnection = userConnection;
    this.version = userConnection.getPendingConnection().getVersion();
    this.userConnection.setClientEntityId(PacketUtils.CLIENTID);
    this.userConnection.setDimension(0);

    sendMessage(PacketsPosition.CLEAR_CHAT);

    if (displayWorld) {
      PacketUtils
          .spawnPlayer(channel, userConnection.getPendingConnection().getVersion(), false, false);
    } else {
      channel.write(PacketUtils.getCachedPacket(6).get(version),
          channel.voidPromise()); //captcha reset
    }

    //PacketUtils.titles[6].writeTitle( channel, version );
    //channel.write( PacketUtils.getCachedPacket( PacketsPosition.LOADING_MESSAGE ).get( version ), channel.voidPromise() );
    this.sendPing();
    this.aegis.addVpnConnection(this);

    resetPosition(false);
    if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
      LOGGER.log(Level.INFO, "[Aegis AntiVPN] " + toString() + " has connected");
    }

    this.aegis.getVpnSystem().getVpnRequester().runAsync(() -> {
      final String address = getUserConnection().getAddress().getAddress().getHostAddress();

      for (VPNDetector vpnDetector : this.aegis.getVpnSystem().getVpnDetectors()) {
        try {
                    /*if (vpnDetector.isLimitable() && vpnDetector.count() > vpnDetector.getLimit()) {
                        this.setChecked(true);
                        this.completeLogin();
                        return this;
                    }*/
          if (vpnDetector.isBad(address)) {
            this.setChecked(true);
            this.failed(KickType.ANTIVPN_ERROR);
            if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
              LOGGER.warning(
                  "[Aegis AntiVPN] " + getName() + " is using vpn/proxy! Checker: " + vpnDetector
                      .getName());
            }
            return this;
          }
        } catch (IOException ex) {
          if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
            LOGGER.warning(
                "[Aegis AntiVPN] Couldn't get info about " + getName() + "'s address! Checker: "
                    + vpnDetector.getName() + " -> " + ex.getMessage());
          }
                    /*this.setChecked(true);
                    this.completeLogin();*/
        }
      }
      this.setChecked(true);
      this.completeLogin();
      return this;
    });
  }

  @Override
  public void exception(Throwable t) {
    markDisconnected = true;
    this.userConnection.disconnect(Util.exception(t));
    disconnected();
  }

  @Override
  public int getVersion() {
    return this.version;
  }


  @Override
  public void disconnected(ChannelWrapper channel) {
    if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
      LOGGER.info("[Aegis AntiVPN] " + this.name + " left during verificating!");
    }
    disconnected();
    aegis.removeVPNConnection(null, this);
  }

  @Override
  public void handlerChanged() {
    disconnected();
  }

  private void disconnected() {
    channel = null;
    userConnection = null;
  }

  public void completeLogin() {
    sendMessage(PacketsPosition.CLEAR_CHAT);
    channel.flush();
    if (this.aegis.getAuthSystem() != null) {
      resetPosition(false);
      channel.write(
          PacketUtils.getCachedPacket(PacketsPosition.SETEXP_RESET).get(version),
          channel.voidPromise());

      userConnection.getCh().getHandle().pipeline().get(HandlerBoss.class)
          .setHandler(new AuthConnector(this.userConnection, this.aegis,
              false, 0, 0));
    } else {
      userConnection.setNeedLogin(false);
      userConnection.getPendingConnection().finishLogin(userConnection, true);
    }
    markDisconnected = true;
    if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
      LOGGER.log(Level.INFO,
          "[Aegis AntiVPN] " + name + " has passed AntiVPN verification");
    }
  }

  @Override
  public void onMove() {
        /*if (lastY != y && waitingTeleportId == -1) {

            resetPosition(false);
        }*/
  }

  public void sendPing() {
    if (this.lastSend == 0) {
      lastSend = System.currentTimeMillis();
      channel.writeAndFlush(PacketUtils.getCachedPacket(PacketsPosition.KEEPALIVE).get(version));
    }
  }


  private void resetPosition(boolean disableFall) {
    if (disableFall) {
      channel.write(PacketUtils.getCachedPacket(PacketsPosition.PLAYERABILITIES).get(version),
          channel.voidPromise());
    }
    waitingTeleportId = 9876;
    channel.writeAndFlush(
        PacketUtils.getCachedPacket(PacketsPosition.PLAYERPOSANDLOOK_CAPTCHA).get(version),
        channel.voidPromise());
  }

  @Override
  public void handle(Chat chat) throws Exception {
  }

  public void failed(KickType type) {
    PacketUtils.kickPlayer(type, Protocol.GAME, userConnection.getCh(), version);
    markDisconnected = true;
    if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
      LOGGER.log(Level.INFO,
          "[Aegis AntiVPN] [" + name + "] Disconnected: " + (type == KickType.ANTIVPN_ERROR
              ? "anti vpn check not passed" : "too long verification"));
    }
  }

  @Override
  public void handle(ClientSettings settings) throws Exception {
    this.userConnection.setSettings(settings);
    this.userConnection.setCallSettingsEvent(true);
  }

  @Override
  public void handle(KeepAlive keepAlive) throws Exception {
    if (keepAlive.getRandomId() == 9876) {
            /*if ( lastSend == 0 )
            {
                failed( KickType.NOTPLAYER, "Tried send fake ping" );
                return;
            }*/
      long ping = System.currentTimeMillis() - lastSend;
      totalping = totalping == 9999 ? ping : totalping + ping;
      lastSend = 0;
    }
  }

  @Override
  public void handle(PluginMessage pluginMessage) throws Exception {
    if (PluginMessage.SHOULD_RELAY.apply(pluginMessage)) {
      userConnection.getPendingConnection().getRelayMessages().add(pluginMessage);
    } else {
      userConnection.getDelayedPluginMessages().add(pluginMessage);
    }
  }


  public String getName() {
    return name.toLowerCase();
  }

  public boolean isConnected() {
    return userConnection != null && channel != null && !markDisconnected && userConnection
        .isConnected();
  }


  public void sendMessage(int index) {
    ByteBuf buf = PacketUtils.getCachedPacket(index).get(getVersion());
    if (buf != null) {
      getChannel().write(buf, getChannel().voidPromise());
    }
  }


  @Override
  public String toString() {
    return "[" + name + "] <-> Aegis AntiVPN";
  }

}