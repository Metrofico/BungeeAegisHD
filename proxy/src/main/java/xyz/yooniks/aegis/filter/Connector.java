package xyz.yooniks.aegis.filter;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import java.util.concurrent.ThreadLocalRandom;
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
import xyz.yooniks.aegis.Aegis.CheckState;
import xyz.yooniks.aegis.auth.handler.AuthConnector;
import xyz.yooniks.aegis.caching.CachedCaptcha.CaptchaHolder;
import xyz.yooniks.aegis.caching.PacketUtils;
import xyz.yooniks.aegis.caching.PacketUtils.KickType;
import xyz.yooniks.aegis.caching.PacketsPosition;
import xyz.yooniks.aegis.config.Settings;
import xyz.yooniks.aegis.utils.IPUtils;
import xyz.yooniks.aegis.utils.ManyChecksUtils;
import xyz.yooniks.aegis.vpn.VPNConnector;

/**
 * @author Leymooo
 */
@EqualsAndHashCode(callSuper = false, of =
    {
        "name"
    })
public class Connector extends MoveHandler implements SimpleConnector {

  private static final Logger LOGGER = BungeeCord.getInstance().getLogger();

  public static int TOTAL_TICKS = 100;
  private static long TOTAL_TIME = (TOTAL_TICKS * 50) - 100; //TICKS * 50MS

  private final Aegis aegis;
  private final String name;
  private final String ip;
  private final int version;
  private final ThreadLocalRandom random = ThreadLocalRandom.current();
  @Getter
  private UserConnection userConnection;
  @Getter
  @Setter
  private CheckState state = CheckState.CAPTCHA_ON_POSITION_FAILED;
  @Getter
  private Channel channel;
  private int aticks = 0, sentPings = 0, attemps = 3;
  private String captchaAnswer;
  @Getter
  private long joinTime = System.currentTimeMillis();
  private long lastSend = 0, totalping = 9999;
  private boolean markDisconnected = false;

  public Connector(UserConnection userConnection, Aegis aegis) {
    Preconditions.checkNotNull(aegis, "Aegis instance is null");

    this.aegis = aegis;
    this.state = this.aegis.getCurrentCheckState();
    this.name = userConnection.getName();
    this.channel = userConnection.getCh().getHandle();
    this.userConnection = userConnection;
    this.version = userConnection.getPendingConnection().getVersion();
    this.userConnection.setClientEntityId(PacketUtils.CLIENTID);
    this.userConnection.setDimension(0);
    this.aegis.incrementBotCounter();
    this.ip = IPUtils.getAddress(this.userConnection).getHostAddress();
    if (state == CheckState.CAPTCHA_ON_POSITION_FAILED) {
      PacketUtils
          .spawnPlayer(channel, userConnection.getPendingConnection().getVersion(), false, false);
      PacketUtils.titles[0].writeTitle(channel, version);
    } else {
      PacketUtils.spawnPlayer(channel, userConnection.getPendingConnection().getVersion(),
          state == CheckState.ONLY_CAPTCHA, true);
      sendCaptcha();
      PacketUtils.titles[1].writeTitle(channel, version);
    }
    sendPing();
    //channel.writeAndFlush( PacketUtils.createPacket( new SetSlot( 0, 36, i, 1, 0 ), PacketUtils.getPacketId( new SetSlot(), version, Protocol.BotFilter ), version ), channel.voidPromise() );
      if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
          LOGGER.log(Level.INFO, toString() + " has connected");
      }
  }

  @Override
  public void exception(Throwable t) throws Exception {
    markDisconnected = true;
    if (state == CheckState.FAILED) {
      channel.close();
    } else {
      this.userConnection.disconnect(Util.exception(t));
    }
    disconnected();
  }

  @Override
  public int getVersion() {
    return version;
  }

  @Override
  public void disconnected(ChannelWrapper channel) throws Exception {
    switch (state) {
      case ONLY_CAPTCHA:
      case ONLY_POSITION:
      case CAPTCHA_POSITION:
        if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
          String info =
              "(Aegis) [" + name + "|" + ip + "] leaved from server during check";
          LOGGER.log(Level.INFO, info);
        }
        break;
    }
    aegis.removeConnection(null, this);
    disconnected();
  }

  @Override
  public void handlerChanged() {
    disconnected();
  }

  private void disconnected() {
    channel = null;
    userConnection = null;
  }

  public void completeCheck() {
    if (System.currentTimeMillis() - joinTime < TOTAL_TIME && state != CheckState.ONLY_CAPTCHA) {
      if (state == CheckState.CAPTCHA_POSITION && aticks < TOTAL_TICKS) {
        channel
            .writeAndFlush(PacketUtils.getCachedPacket(PacketsPosition.SETSLOT_RESET).get(version),
                channel.voidPromise());
        state = CheckState.ONLY_POSITION;
      } else {
        if (state == CheckState.CAPTCHA_ON_POSITION_FAILED) {
          changeStateToCaptcha();
        } else {
            if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
                failed(KickType.NOTPLAYER, "Too fast check passed");
            }
        }
      }
      return;
    }
    int devide = lastSend == 0 ? sentPings : sentPings - 1;
    if (aegis.checkBigPing(totalping / (devide <= 0 ? 1 : devide))) {
        if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
            failed(KickType.PING, "Big ping");
        }
      return;
    }
    if (Settings.IMP.PROTECTION.CHECK_SETTINGS && !this.userConnection.isCallSettingsEvent()) {
        if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
            failed(KickType.MC_BRAND, "Did not send Settings packet after falling and captcha check");
        }
      return;
    }
    state = CheckState.SUCCESSFULLY;
    PacketUtils.titles[2].writeTitle(channel, version);
    channel.flush();
    aegis.removeConnection(null, this);
    sendMessage(PacketsPosition.CHECK_SUS);

    aegis.saveUser(getName(), IPUtils.getAddress(userConnection));


        /*if (this.aegis.getAuthSystem() != null) {
            resetPosition(false);
            channel.write( PacketUtils.getCachedPacket( PacketsPosition.SETEXP_RESET ).get( version ), channel.voidPromise() );

            userConnection.getCh().getHandle().pipeline().get(HandlerBoss.class)
              .setHandler(new AuthConnector(this.userConnection, this.aegis,
                  false, 0, 0));
        }*/
    if (this.aegis.getVpnSystem() != null) {
      resetPosition(false);
      channel.write(PacketUtils.getCachedPacket(PacketsPosition.SETEXP_RESET).get(version),
          channel.voidPromise());

      userConnection.getCh().getHandle().pipeline().get(HandlerBoss.class)
          .setHandler(new VPNConnector(this.userConnection, this.aegis,
              false, 0, 0));
    } else {
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
    }

    markDisconnected = true;

      if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
          LOGGER.log(Level.INFO, "[Aegis BotFilter] " + name + "/" + ip + " has connected");
      }
  }

  @Override
  public void onMove() {
    if (lastY == -1 || state == CheckState.FAILED || state == CheckState.SUCCESSFULLY || onGround) {
      return;
    }
    if (state == CheckState.ONLY_CAPTCHA) {
      if (lastY != y && waitingTeleportId == -1) {
        resetPosition(true);
      }
      return;
    }
    // System.out.println( "lastY=" + lastY + "; y=" + y + "; diff=" + formatDouble( lastY - y ) + "; need=" + getSpeed( ticks ) +"; ticks=" + ticks );
    if (formatDouble(lastY - y) != getSpeed(ticks)) {
      if (state == CheckState.CAPTCHA_ON_POSITION_FAILED) {
        changeStateToCaptcha();
      } else {
          if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
              failed(KickType.NOTPLAYER, "Failed position check");
          }
      }
      return;
    }
    if (y <= 60 && state == CheckState.CAPTCHA_POSITION && waitingTeleportId == -1) {
      resetPosition(false);
    }
    if (aticks >= TOTAL_TICKS && state != CheckState.CAPTCHA_POSITION) {
      completeCheck();
      return;
    }
    if (state == CheckState.CAPTCHA_ON_POSITION_FAILED) {
      ByteBuf expBuf = PacketUtils.expPackets.get(aticks, version);
      if (expBuf != null) {
        channel.writeAndFlush(expBuf, channel.voidPromise());
      }
    }
    ticks++;
    aticks++;
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
    if (state != CheckState.CAPTCHA_ON_POSITION_FAILED) {
      String message = chat.getMessage();
      if (message.length() > 256) {
        failed(KickType.NOTPLAYER, "Too long message");
        return;
      }
      if (message.replace("/", "").equals(String.valueOf(captchaAnswer))) {
        completeCheck();
      } else if (--attemps != 0) {
        ByteBuf buf = attemps == 2 ? PacketUtils.getCachedPacket(PacketsPosition.CAPTCHA_FAILED_2)
            .get(version)
            : PacketUtils.getCachedPacket(PacketsPosition.CAPTCHA_FAILED_1).get(version);
        if (buf != null) {
          channel.write(buf, channel.voidPromise());
        }
        sendCaptcha();
      } else {
          if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
              failed(KickType.NOTPLAYER, "Failed captcha check");
          }
          if (aegis.getCaptchaFailed() != null) {
              aegis.getCaptchaFailed()
                  .failed(userConnection.getAddress().getAddress().getHostAddress());
          }
      }
    } else if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
        BungeeCord.getInstance().getLogger().info(
            "[Aegis] " + this.name + " is trying to type \"" + chat.getMessage()
                + "\" on chat but invalid state!");
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
      if (lastSend == 0) {
          if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
              failed(KickType.NOTPLAYER, "Tried send fake ping");
          }
        return;
      }
      long ping = System.currentTimeMillis() - lastSend;
      totalping = totalping == 9999 ? ping : totalping + ping;
      lastSend = 0;
    }
  }

  @Override
  public void handle(PluginMessage pluginMessage) throws Exception {
    final String tag = pluginMessage.getTag().toLowerCase();
    if (tag.contains("bedit") || tag.contains("bsign")) {
      return;
    }
    if (PluginMessage.SHOULD_RELAY.apply(pluginMessage)) {
      userConnection.getPendingConnection().getRelayMessages().add(pluginMessage);
    } else {
      userConnection.getDelayedPluginMessages().add(pluginMessage);
    }

  }

  public void sendPing() {
    if (this.lastSend == 0 && !(state == CheckState.FAILED || state == CheckState.SUCCESSFULLY)) {
      lastSend = System.currentTimeMillis();
      sentPings++;
      channel.writeAndFlush(PacketUtils.getCachedPacket(PacketsPosition.KEEPALIVE).get(version));
    }
  }

  private void sendCaptcha() {
    CaptchaHolder captchaHolder = PacketUtils.captchas.randomCaptcha();
    captchaAnswer = captchaHolder.getAnswer();
    channel.write( PacketUtils.getCachedPacket( PacketsPosition.SETSLOT_MAP ).get( version ), channel.voidPromise() );
    captchaHolder.write( channel, version, true );
    //captchaAnswer = random.nextInt(100, 999);
    //channel.write(PacketUtils.getCachedPacket(PacketsPosition.SETSLOT_MAP).get(version),
    //    channel.voidPromise());
    //channel.writeAndFlush(PacketUtils.captchas.get(version, captchaAnswer), channel.voidPromise());
  }

  private void changeStateToCaptcha() {
    state = CheckState.ONLY_CAPTCHA;
    joinTime = System.currentTimeMillis() + 3500;
    channel.write(PacketUtils.getCachedPacket(PacketsPosition.SETEXP_RESET).get(version),
        channel.voidPromise());
    PacketUtils.titles[1].writeTitle(channel, version);
    resetPosition(true);
    sendCaptcha();
  }

  public String getName() {
    return name.toLowerCase();
  }

  public boolean isConnected() {
    return userConnection != null && channel != null && !markDisconnected && userConnection
        .isConnected();
  }

  public void failed(KickType type, String kickMessage) {
    ManyChecksUtils.IncreaseOrAdd(IPUtils.getAddress(this.userConnection));

    state = CheckState.FAILED;
    PacketUtils.kickPlayer(type, Protocol.GAME, userConnection.getCh(), version);
    markDisconnected = true;
      if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
          LOGGER.log(Level.INFO, "[Aegis BotFilter] [" + name + "/" + ip + "] check failed: " + kickMessage);
      }
  }

  public void sendMessage(int index) {
    ByteBuf buf = PacketUtils.getCachedPacket(index).get(getVersion());
    if (buf != null) {
      getChannel().write(buf, getChannel().voidPromise());
    }
  }


  @Override
  public String toString() {
    return "[" + name + "," + userConnection.getSocketAddress() + "] <-> Aegis BotFilter";
  }
}