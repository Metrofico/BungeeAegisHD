package xyz.yooniks.aegis.auth.handler;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.Util;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.packet.Chat;
import net.md_5.bungee.protocol.packet.ClientSettings;
import net.md_5.bungee.protocol.packet.KeepAlive;
import net.md_5.bungee.protocol.packet.PluginMessage;
import xyz.yooniks.aegis.Aegis;
import xyz.yooniks.aegis.auth.AuthSystem;
import xyz.yooniks.aegis.auth.premium.PremiumManager;
import xyz.yooniks.aegis.auth.premium.PremiumManager.PremiumUser;
import xyz.yooniks.aegis.auth.premium.UUIDHelper;
import xyz.yooniks.aegis.auth.user.AuthUser;
import xyz.yooniks.aegis.auth.user.AuthUser.PremiumAnswer;
import xyz.yooniks.aegis.auth.user.AuthUserManager;
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
public class AuthConnector extends MoveHandler implements SimpleConnector {

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
  private int aticks = 0, sentPings = 0;

  @Getter
  private long joinTime = System.currentTimeMillis();
  private long lastSend = 0, totalping = 9999;

  private boolean markDisconnected = false;

  @Getter
  private AuthUser user;

  @Getter
  @Setter
  private AuthMessage authMessage = AuthMessage.LOADING;

  public AuthConnector(UserConnection userConnection, Aegis aegis, boolean displayWorld) {
    this(userConnection, aegis, displayWorld, 0, 0);
  }

  public AuthConnector(UserConnection userConnection, Aegis aegis, boolean displayWorld,
      int aticks, int ticks) {
    Preconditions.checkNotNull(aegis, "Aegis instance is null");
    this.ticks = ticks;
    this.aticks = ticks;
    this.aegis = aegis;
    this.name = userConnection.getName();
    this.channel = userConnection.getCh().getHandle();
    this.userConnection = userConnection;
    this.version = userConnection.getPendingConnection().getVersion();
    this.userConnection.setClientEntityId(PacketUtils.CLIENTID);
    this.userConnection.setDimension(0);

    if (Settings.IMP.AUTH.MESSAGES.CLEAR_CHAT_AFTER_LOGIN)
      sendMessage(PacketsPosition.CLEAR_CHAT);

      if (displayWorld) {
          PacketUtils
              .spawnPlayer(channel, userConnection.getPendingConnection().getVersion(), false,
                  false);
      } else {
          channel.write(PacketUtils.getCachedPacket(6).get(version),
              channel.voidPromise()); //captcha reset
      }

    PacketUtils.titles[6].writeTitle(channel, version);
    channel.write(PacketUtils.getCachedPacket(PacketsPosition.LOADING_MESSAGE).get(version),
        channel.voidPromise());
    this.sendPing();
    this.aegis.addAuthConnection(this);

    final AuthSystem authSystem = aegis.getAuthSystem();
    final AuthUserManager userManager = authSystem.getUserManager();
    final UUID id = userConnection.getUniqueId();
    this.user = userManager.getUser(id);
    if (this.user == null) {
      authSystem.getConcurrentManager().runAsync(() -> {
        try {
          AuthUser found = authSystem.getDatabase()
              .loadObject(id);

          if (found == null) {
            found = new AuthUser(id, name);

            final AuthUser similarNameUser = authSystem.getDatabase().loadByNameIgnoreCase(name);

            if (similarNameUser != null) {

              final DefinedPacket packet = PacketUtils.createKickPacket(
                  Settings.IMP.AUTH.MESSAGES.WRONG_NAME.replace("{OLD-NAME}", found.getName()));

              this.channel.write(packet);
              this.channel.flush();

              disconnected();

              //userManager.putUser(this.user = found);
              return found;
            }
          }
          found.setLogged(false);

          if (!Settings.IMP.AUTH.MESSAGES.ALLOW_PREMIUM_USERS) {
            found.setPremium(false);
            found.setPremiumAnswer(PremiumAnswer.NO);
            found.setOnlineId(null);
            found.setLogged(false);
            this.authMessage = AuthMessage.LOADED;
          } else if (userConnection.getPendingConnection().isOnlineMode()) {
            found.setPremium(true);
            found.setLogged(true);
            this.authMessage = AuthMessage.PREMIUM;
          } else {
            found.setLogged(false);
            this.authMessage = AuthMessage.LOADED;
          }

          if (!found.isCheckedIfPremium()) {
            found.setCheckedIfPremium(true);

            final boolean premium;
            if (found.getOnlineId() != null && !found.getOnlineId().equals(id)) {
              premium = true;

              if (PremiumManager.getByName(this.name) == null) {
                  if (found.getPremiumAnswer() != PremiumAnswer.NO) {
                      PremiumManager
                          .putUser(this.name, new PremiumUser(true, found.getOnlineId()));
                  }
              }
            } else {
              if (found.getOnlineId() == null) {
                final PremiumUser user = UUIDHelper
                    .requestPremiumUser(this.name, id);
                premium = user.isPremium();
                found.setOnlineId(user.getUuid());
              } else {
                premium = found.isPremium();
                found.setOnlineId(id);
              }
            }
            //PremiumManager.putUser(getName(), user);

            if (premium) {
              if (!Settings.IMP.AUTH.MESSAGES.ASK_USERS_IF_PREMIUM_WHEN_PREMIUM) {
                found.setPremiumAnswer(PremiumAnswer.YES);
              }
              if (!Settings.IMP.AUTH.MESSAGES.ALLOW_PREMIUM_USERS) {
                found.setPremium(false);
                found.setLogged(false);
                found.setPremiumAnswer(PremiumAnswer.NO);
                this.authMessage = AuthMessage.LOADED;
              }
              if (found.getPremiumAnswer() == PremiumAnswer.NONE) {
                this.authMessage = AuthMessage.ASK_IF_PREMIUM;
                found.setLogged(false);
              } else if (found.getPremiumAnswer() == PremiumAnswer.YES) {
                found.setPremium(true);
                channel.write(PacketUtils
                    .getCachedPacket(PacketsPosition.PREMIUM_NEED_RELOGIN)
                    .get(version), channel.voidPromise());
                this.channel.flush();
                if (Settings.IMP.AEGIS_SETTINGS.DELAYED_DISCONNECTIONS) {
                  this.aegis.getAuthSystem().getConcurrentManager()
                      .runAsyncDelayed(() -> {
                        try {
                          Thread.sleep(3500);
                        } catch (InterruptedException ignored) {
                        }
                        channel.close();
                        disconnected();
                        return null;
                      });
                } else {
                  channel.close();
                  disconnected();
                }
              } else {
                found.setPremium(false);
                found.setLogged(false);
                this.authMessage = AuthMessage.LOADED;
              }
            }
          }

          userManager.putUser(this.user = found);
          return found;
        } catch (SQLException ex) {
          BungeeCord.getInstance().getLogger()
              .log(Level.WARNING, "Could not load user info (AuthConnector)", ex);
        }
        return null;
      });
    } else {
            /*if (!this.user.getName().equals(name)) {

                final DefinedPacket packet = PacketUtils.createKickPacket(Settings.IMP.AUTH.MESSAGES.WRONG_NAME.replace("{OLD-NAME}", this.user.getName()));

                this.channel.write(packet);
                this.channel.flush();

                disconnected();

                return;
            }*/

      user.setLogged(false);

      if (!this.user.isCheckedIfPremium()) {
        //bylo bez zmiennej, samo assign \/

        final boolean premium;
        if (this.user.getOnlineId() != null && !this.user.getOnlineId().equals(id)) {
          premium = true;

          if (PremiumManager.getByName(this.name) == null) {
              if (this.user.getPremiumAnswer() != PremiumAnswer.NO) {
                  PremiumManager
                      .putUser(this.name, new PremiumUser(true, this.user.getOnlineId()));
              }
          }
        } else {
          if (this.user.getOnlineId() == null) {
            final PremiumUser user = UUIDHelper
                .requestPremiumUser(this.name, id);
            premium = user.isPremium();
            this.user.setOnlineId(user.getUuid());
          } else {
            //TODO: nw czy dziala
            premium = user.isPremium();//false;
            this.user.setOnlineId(id);
          }
        }

        //TODO usunalem o 18:43
        //final PremiumUser user = UUIDHelper.requestPremiumUser(this.name, id);

        // this.user.setOnlineId(user.getUuid());

        this.user.setCheckedIfPremium(true);

        if (premium) {

          if (!Settings.IMP.AUTH.MESSAGES.ASK_USERS_IF_PREMIUM_WHEN_PREMIUM) {
            this.user.setPremiumAnswer(PremiumAnswer.YES);
          }

          if (this.user.getPremiumAnswer() == PremiumAnswer.NONE) {
            this.authMessage = AuthMessage.ASK_IF_PREMIUM;
            this.user.setLogged(false);

          } else if (this.user.getPremiumAnswer() == PremiumAnswer.YES) {
            this.user.setPremium(true);
            channel.write(PacketUtils
                .getCachedPacket(PacketsPosition.PREMIUM_NEED_RELOGIN)
                .get(version), channel.voidPromise());
            this.channel.flush();

            if (Settings.IMP.AEGIS_SETTINGS.DELAYED_DISCONNECTIONS) {
              this.aegis.getAuthSystem().getConcurrentManager()
                  .runAsyncDelayed(() -> {
                    try {
                      Thread.sleep(3500);
                    } catch (InterruptedException ignored) {
                    }
                    //channel.close();
                    disconnected();
                    return null;
                  });
            } else {
              //channel.close();
              disconnected();
            }
          } else {

            this.user.setPremium(false);
            this.user.setLogged(false);
            this.authMessage = AuthMessage.LOADED;
          }

          //
          return;
        }
      }

      if (user.isPremium()) {//userConnection.getPendingConnection().isOnlineMode()) {

        if (this.user.getPremiumAnswer() == PremiumAnswer.YES) {

          user.setPremium(true);
          user.setLogged(true);
          this.authMessage = AuthMessage.PREMIUM;
        } else if (this.user.getPremiumAnswer() == PremiumAnswer.NO) {
          user.setPremium(false);
          user.setLogged(false);
          this.authMessage = AuthMessage.LOADED;
        } else {
          if (user.getPremiumAnswer() == PremiumAnswer.NONE) {
            this.authMessage = AuthMessage.ASK_IF_PREMIUM;
          }
          user.setLogged(false);
        }
      } else {
        user.setLogged(false);
        this.authMessage = AuthMessage.LOADED;
      }
    }

    resetPosition(false);

      if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
          LOGGER.log(Level.INFO, "[Aegis Auth] " + toString() + " has connected");
      }
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
          LOGGER.info("[Aegis Auth] " + this.name + " left during authenticating!");
      }

    disconnected();
    aegis.removeAuthConnection(null, this);
  }

  @Override
  public void handlerChanged() {
    disconnected();
  }

  private void disconnected() {
    if (this.authMessage == AuthMessage.ASK_IF_PREMIUM) {
      PremiumManager.removeUser(this.name);
      this.user.setPremium(true);
      this.user.setCheckedIfPremium(true);
    } else if (this.authMessage == AuthMessage.LOGIN || this.authMessage == AuthMessage.REGISTER) {
      PremiumManager.putUser(this.name, new PremiumUser(false, this.user.getOnlineId()));
    }

    channel = null;
    userConnection = null;
    if (this.user != null) {
      if (user.getPremiumAnswer() == PremiumAnswer.NO
        /*&& user.getOnlineId() != null && !user.getId().equals(user.getOnlineId())*/) {
        //if (PremiumManager.getByName(this.name) != null) {
        PremiumManager.removeUser(this.name);
        //}
      }

      //user.setLogged(false);
    }
  }

  public void completeLogin() {
    if (Settings.IMP.AUTH.MESSAGES.CLEAR_CHAT_AFTER_LOGIN)
      sendMessage(PacketsPosition.CLEAR_CHAT);
    channel.flush();

    if (Settings.IMP.AEGIS_SETTINGS.DELAYED_DISCONNECTIONS) {
      this.aegis.getAuthSystem().getConcurrentManager().runAsyncDelayed(() -> {
        try {
          Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }

        if (this.user != null && this.user.isPremium()) {
          if (this.user.getPremiumAnswer() == PremiumAnswer.YES) {
            userConnection.getPendingConnection().setUniqueId(user.getOnlineId());
          }
        }
        userConnection.setNeedLogin(false);
        userConnection.getPendingConnection().finishLogin(userConnection, true);
        markDisconnected = true;
          if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
              LOGGER.log(Level.INFO,
                  "[Aegis Auth] " + name + " has logged in and moved to default server");
          }
        return null;
      });
    } else {
      if (this.user != null && this.user.isPremium()) {
        if (this.user.getPremiumAnswer() == PremiumAnswer.YES) {
          userConnection.getPendingConnection().setUniqueId(user.getOnlineId());
        }
      }
      userConnection.setNeedLogin(false);
      userConnection.getPendingConnection().finishLogin(userConnection, true);
      markDisconnected = true;
        if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
            LOGGER
                .log(Level.INFO,
                    "[Aegis Auth] " + name + " has logged in and moved to default server");
        }
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
      sentPings++;
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

    String message = chat.getMessage();
    if (message.length() > 256) {
      return;
    }

    if (this.authMessage == AuthMessage.ASK_IF_PREMIUM) {
      if (message.equalsIgnoreCase(Settings.IMP.AUTH.MESSAGES.ASK_USER_IF_PREMIUM_YES_ANSWER)
          || Settings.IMP.AUTH.MESSAGES.ASK_USER_IF_PREMIUM_YES_ANSWER_ALIASES
          .contains(message.toLowerCase())) {
        this.user.setPremium(true);
        this.user.setPremiumAnswer(PremiumAnswer.YES);
        this.authMessage = AuthMessage.NONE;
        PremiumManager.putUser(this.name, new PremiumUser(true, this.user.getOnlineId()));
        this.channel.write(PacketUtils
            .getCachedPacket(PacketsPosition.PREMIUM_NEED_RELOGIN)
            .get(version), channel.voidPromise());
        this.channel.flush();
        this.channel.close();
        disconnected();
      } else if (message.equalsIgnoreCase(Settings.IMP.AUTH.MESSAGES.ASK_USER_IF_PREMIUM_NO_ANSWER)
          || Settings.IMP.AUTH.MESSAGES.ASK_USER_IF_PREMIUM_NO_ANSWER_ALIASES
          .contains(message.toLowerCase())) {
        this.user.setPremium(false);
        this.user.setPremiumAnswer(PremiumAnswer.NO);
        this.authMessage = AuthMessage.LOADED;
        this.joinTime = System.currentTimeMillis() + 1500L;
        this.ticks = 0;
        this.aticks = 0;
      }
      return;
    }

    if (this.user == null) {
      channel
          .write(PacketUtils.getCachedPacket(PacketsPosition.DATA_NOT_LOADED_MESSAGE).get(version),
              channel.voidPromise());
      return;
    }

    if (this.user.isPremium()) {
      channel.write(
          PacketUtils.getCachedPacket(PacketsPosition.USER_PREMIUM_CANNOT_DO_THAT).get(version),
          channel.voidPromise());
      return;
    }

    final String commandName = message.split(" ")[0].replace("/", "");

    final String[] messageSplit = message.split(" ");
    if (messageSplit.length <= 0) {
      return;
    }

    final String[] args = Arrays.copyOfRange(messageSplit, 1, messageSplit.length);

    if (commandName.equalsIgnoreCase("login") || commandName.equals("l")) {
      if (!this.user.isRegistered()) {
        channel.write(PacketUtils.getCachedPacket(PacketsPosition.REGISTER_MESSAGE).get(version),
            channel.voidPromise());
        return;
      }
        if (args.length < 1) {
            return;
        }
      final String userPassword = this.user.getPassword();
      if (this.aegis.getAuthSystem().getEncryption().match(args[0], userPassword)) {
        this.authMessage = AuthMessage.NONE;
        PacketUtils.titles[7].writeTitle(channel, version);
        channel.write(PacketUtils.getCachedPacket(PacketsPosition.LOGGED_MESSAGE).get(version),
            channel.voidPromise());
        this.user.setLogged(true);
        this.completeLogin();
      } else {
        channel
            .write(PacketUtils.getCachedPacket(PacketsPosition.PASSWORD_DO_NOT_MATCH).get(version),
                channel.voidPromise());
      }
    } else if (commandName.equalsIgnoreCase("register") || commandName.equalsIgnoreCase("reg")
        || commandName.equalsIgnoreCase("r")) {
        if (args.length < 2 || this.user.isRegistered()) {
            return;
        }
      if (!args[0].equalsIgnoreCase(args[1])) {
        channel
            .write(PacketUtils.getCachedPacket(PacketsPosition.PASSWORD_DO_NOT_MATCH).get(version),
                channel.voidPromise());
        return;
      }
        if (args[0].length() >= 59) {
            return;
        }

      this.authMessage = AuthMessage.NONE;
      final String password = this.aegis.getAuthSystem().getEncryption().hash(args[0]);
      this.user.setPassword(password);
      this.user.setRegistered(true);
      this.user.setLogged(true);
      PacketUtils.titles[7].writeTitle(channel, version);
      channel.write(PacketUtils.getCachedPacket(PacketsPosition.REGISTERED_MESSAGE).get(version),
          channel.voidPromise());
      this.completeLogin();
    }
  }

  public void failed(KickType type) {

    if (this.user != null && (this.user.getPremiumAnswer() == null
        || this.user.getPremiumAnswer() == PremiumAnswer.NONE)) {
      PremiumManager.removeUser(this.user.getName());
    }

    PacketUtils.kickPlayer(type, Protocol.GAME, userConnection.getCh(), version);
    markDisconnected = true;
    LOGGER.log(Level.INFO, "[Aegis Auth] [" + name + "] Disconnected: too long authentication");
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
    return "[" + name + "," + userConnection.getSocketAddress() + "] <-> Aegis AuthSystem";
  }

  public static enum AuthMessage {
    LOADING, LOADED, PREMIUM, REGISTER, LOGIN, NONE, ASK_IF_PREMIUM
  }

}