package net.md_5.bungee;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.score.Objective;
import net.md_5.bungee.api.score.Score;
import net.md_5.bungee.api.score.Scoreboard;
import net.md_5.bungee.api.score.Team;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.connection.CancelSendSignal;
import net.md_5.bungee.connection.DownstreamBridge;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.forge.ForgeConstants;
import net.md_5.bungee.forge.ForgeServerHandler;
import net.md_5.bungee.forge.ForgeUtils;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.*;
import net.md_5.bungee.util.QuietException;
import xyz.yooniks.aegis.config.Settings;
import xyz.yooniks.aegis.connection.ServerConnectorCache;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.logging.Level;

@RequiredArgsConstructor
public class ServerConnector extends PacketHandler {

    private final ProxyServer bungee;
    private final UserConnection user;
    private final BungeeServerInfo target;
    private ChannelWrapper ch;
    private State thisState = State.LOGIN_SUCCESS;
    @Getter
    private ForgeServerHandler handshakeHandler;
    private boolean obsolete;

    @Override
    public void exception(Throwable t) throws Exception {
        if (obsolete) {
            return;
        }

        String message = "Exception Connecting:" + Util.exception(t);
        if (user.getServer() == null) {
            user.disconnect(message);
        } else {
            user.sendMessage(ChatColor.RED + message);
        }
    }

    @Override
    public void connected(final ChannelWrapper channel) {

        if (Settings.IMP.AEGIS_SETTINGS.LIMIT_SERVERCONNECTOR_CONNECTS) {
            final ServerConnectorCache cache = ServerConnectorCache.getCache();
            final String address = channel.getRemoteAddress().getAddress().getHostAddress();
            if (!cache.canConnect(address)) {
                if (Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
                    bungee.getLogger()
                            .log(Level.INFO, "[Aegis] Blocked too fast ServerConnector connections of {0}!",
                                    this.user.getName());
                }
                return;
            }
            cache.join(address);
        }

        this.ch = channel;
        this.handshakeHandler = new ForgeServerHandler(this.user, this.ch, this.target);
        final Handshake originalHandshake = this.user.getPendingConnection().getHandshake();
        final Handshake copiedHandshake = new Handshake(originalHandshake.getProtocolVersion(),
                originalHandshake.getHost(), originalHandshake.getPort(), 2);
        if (BungeeCord.getInstance().config.isIpForward() && user
                .getSocketAddress() instanceof InetSocketAddress) {
            String newHost =
                    copiedHandshake.getHost() + "\u0000" + this.user.getAddress().getHostString() + "\u0000"
                            + this.user.getUUID();
            final LoginResult profile = this.user.getPendingConnection().getLoginProfile();
            LoginResult.Property[] properties = new LoginResult.Property[0];
            if (profile != null && profile.getProperties() != null
                    && profile.getProperties().length > 0) {
                properties = profile.getProperties();
            }
            //forge support here
            if (Settings.IMP.AEGIS_SETTINGS.FORGE_SUPPORT && this.user.getForgeClientHandler().isFmlTokenInHandshake()) {
                final LoginResult.Property[] newp = Arrays.copyOf(properties, properties.length + 2);
                newp[newp.length - 2] = new LoginResult.Property("forgeClient", "true", null);
                newp[newp.length - 1] = new LoginResult.Property("extraData",
                        this.user.getExtraDataInHandshake().replaceAll("\u0000", "\u0001"), "");
                properties = newp;
            }
            if (properties.length > 0) {
                newHost = newHost + "\u0000" + BungeeCord.getInstance().gson.toJson(properties);
            }
            copiedHandshake.setHost(newHost);
        } else if (!this.user.getExtraDataInHandshake().isEmpty()) {
            copiedHandshake.setHost(copiedHandshake.getHost() + this.user.getExtraDataInHandshake());
        }
        channel.write(copiedHandshake);
        channel.setProtocol(Protocol.LOGIN);
        channel.write(new LoginRequest(this.user.getName()));
    }

    @Override
    public void disconnected(ChannelWrapper channel) throws Exception {
        user.getPendingConnects().remove(target);
    }

    @Override
    public void handle(PacketWrapper packet) throws Exception {
        if (packet == null || packet.packet == null) {
            // throw new QuietException( "Unexpected packet received during server login process!\n" + BufUtil.dump( packet.buf, 16 ) );
        }
    }

    @Override
    public void handle(LoginSuccess loginSuccess) throws Exception {
        Preconditions.checkState(thisState == State.LOGIN_SUCCESS, "Not expecting LOGIN_SUCCESS");
        ch.setProtocol(Protocol.GAME);
        thisState = State.LOGIN;

        // Only reset the Forge client when:
        // 1) The user is switching servers (so has a current server)
        // 2) The handshake is complete
        // 3) The user is currently on a modded server (if we are on a vanilla server,
        //    we may be heading for another vanilla server, so we don't need to reset.)
        //
        // user.getServer() gets the user's CURRENT server, not the one we are trying
        // to connect to.
        //
        // We will reset the connection later if the current server is vanilla, and
        // we need to switch to a modded connection. However, we always need to reset the
        // connection when we have a modded server regardless of where we go - doing it
        // here makes sense.
        if (user.getServer() != null && user.getForgeClientHandler().isHandshakeComplete()
                && user.getServer().isForgeServer()) {
            user.getForgeClientHandler().resetHandshake();
        }

        throw CancelSendSignal.INSTANCE;
    }

    @Override
    public void handle(SetCompression setCompression) throws Exception {
        ch.setCompressionThreshold(setCompression.getThreshold());
    }

  /*
  @Override
  public void handle(Login login) throws Exception {
    Preconditions.checkState(thisState == State.LOGIN, "Not expecting LOGIN");

    ServerConnection server = new ServerConnection(ch, target);
    ServerConnectedEvent event = new ServerConnectedEvent(user, server);
    bungee.getPluginManager().callEvent(event);

    ch.write(BungeeCord.getInstance().registerChannels(user.getPendingConnection().getVersion()));
    Queue<DefinedPacket> packetQueue = target.getPacketQueue();
    synchronized (packetQueue) {
      while (!packetQueue.isEmpty()) {
        ch.write(packetQueue.poll());
      }
    }

    for (PluginMessage message : user.getPendingConnection().getRelayMessages()) {
      ch.write(message);
    }

    if (user.getSettings() != null) {
      ch.write(user.getSettings());
    }

    if (user.getForgeClientHandler().getClientModList() == null && !user.getForgeClientHandler()
        .isHandshakeComplete()) // Vanilla
    {
      user.getForgeClientHandler().setHandshakeComplete();
    }

    if (user.isNeedLogin()) //Aegis
    {
      user.setNeedLogin(false); //Aegis
      // Once again, first connection
      user.setClientEntityId(login.getEntityId());
      user.setServerEntityId(login.getEntityId());

      // Set tab list size, TODO: what shall we do about packet mutability
      Login modLogin = new Login(login.getEntityId(), login.getGameMode(),
          (byte) login.getDimension(), login.getSeed(), login.getDifficulty(),
          (byte) user.getPendingConnection().getListener().getTabListSize(),
          login.getLevelType(), login.getViewDistance(), login.isReducedDebugInfo(),
          login.isNormalRespawn());

      user.unsafe().sendPacket(modLogin);
      String brandString = "Aegis (1.7.x-1.15.2)";

      if (ProtocolConstants.isBeforeOrEq(user.getPendingConnection().getVersion(),
          ProtocolConstants.MINECRAFT_1_7_6)) {
        user.unsafe().sendPacket(
            new PluginMessage("MC|Brand", brandString.getBytes(StandardCharsets.UTF_8),
                handshakeHandler.isServerForge()));
      } else {
        ByteBuf brand = ByteBufAllocator.DEFAULT.heapBuffer();
        DefinedPacket.writeString(brandString, brand);
        user.unsafe().sendPacket(new PluginMessage(
            user.getPendingConnection().getVersion()
                >= ProtocolConstants.MINECRAFT_1_13 ? "minecraft:brand"
                : "MC|Brand", brand, handshakeHandler.isServerForge()));
        brand.release();
      }

      user.setDimension(login.getDimension());
    } else {
      if (user.getServer() != null) //Aegis
      {
        user.getServer().setObsolete(true); //Aegis
      }
      user.getTabListHandler().onServerChange();

      Scoreboard serverScoreboard = user.getServerSentScoreboard();
      for (Objective objective : serverScoreboard.getObjectives()) {
        //user.unsafe().sendPacket( new ScoreboardObjective( objective.getName(), objective.getValue(), ScoreboardObjective.HealthDisplay.fromString( objective.getType() ), (byte) 1 ) );
        user.unsafe().sendPacket(new ScoreboardObjective(objective.getName(), objective.getValue(),
            objective.getType() == null ? null
                : ScoreboardObjective.HealthDisplay.fromString(objective.getType()),
            (byte) 1)); // Travertine - 1.7
      }
      for (Score score : serverScoreboard.getScores()) {
        user.unsafe().sendPacket(
            new ScoreboardScore(score.getItemName(), (byte) 1, score.getScoreName(),
                score.getValue()));
      }
      for (Team team : serverScoreboard.getTeams()) {
        user.unsafe().sendPacket(new net.md_5.bungee.protocol.packet.Team(team.getName()));
      }
      serverScoreboard.clear();

      for (UUID bossbar : user.getSentBossBars()) {
        // Send remove bossbar packet
        user.unsafe().sendPacket(new net.md_5.bungee.protocol.packet.BossBar(bossbar, 1));
      }
      user.getSentBossBars().clear();

      // Update debug info from login packet
      user.unsafe().sendPacket(new EntityStatus(user.getClientEntityId(),
          login.isReducedDebugInfo() ? EntityStatus.DEBUG_INFO_REDUCED
              : EntityStatus.DEBUG_INFO_NORMAL));

      user.setDimensionChange(true);
      if (login.getDimension() == user.getDimension()) {
        user.unsafe().sendPacket(new Respawn((login.getDimension() >= 0 ? -1 : 0), login.getSeed(),
            login.getDifficulty(), login.getGameMode(), login.getLevelType()));
      }

      user.setServerEntityId(login.getEntityId());
      user.unsafe().sendPacket(
          new Respawn(login.getDimension(), login.getSeed(), login.getDifficulty(),
              login.getGameMode(), login.getLevelType()));

      if (user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_14) {
        user.unsafe().sendPacket(new ViewDistance(login.getViewDistance()));
      }

      user.setDimension(login.getDimension());

      // Remove from old servers
      if (this.user.getServer() != null) //Aegis
      {
        this.user.getServer().disconnect("Quitting"); //Aegis
      }
    }

    // TODO: Fix this?
    if (!user.isActive()) {
      server.disconnect("Quitting");
      // Silly server admins see stack trace and die
      bungee.getLogger().warning("No client connected for pending server!");
      return;
    }

    // Add to new server
    // TODO: Move this to the connected() method of DownstreamBridge
    target.addPlayer(user);
    user.getPendingConnects().remove(target);
    user.setServerJoinQueue(null);
    user.setDimensionChange(false);

    user.setServer(server);
    ch.getHandle().pipeline().get(HandlerBoss.class)
        .setHandler(new DownstreamBridge(bungee, user, server));

    bungee.getPluginManager().callEvent(new ServerSwitchEvent(user));

    thisState = State.FINISHED;

    throw CancelSendSignal.INSTANCE;
  }

   */

    /*@Override
    public void handle(Login login) throws Exception {
      Preconditions.checkState(thisState == State.LOGIN, "Not expecting LOGIN");

      ServerConnection server = new ServerConnection(ch, target);
      ServerConnectedEvent event = new ServerConnectedEvent(user, server);
      bungee.getPluginManager().callEvent(event);

      ch.write(BungeeCord.getInstance().registerChannels(user.getPendingConnection().getVersion()));
      Queue<DefinedPacket> packetQueue = target.getPacketQueue();
      synchronized (packetQueue) {
        while (!packetQueue.isEmpty()) {
          ch.write(packetQueue.poll());
        }
      }

      for (PluginMessage message : user.getPendingConnection().getRelayMessages()) {
        ch.write(message);
      }

      if (user.getSettings() != null) {
        ch.write(user.getSettings());
      }

      if (user.getForgeClientHandler().getClientModList() == null && !user.getForgeClientHandler()
          .isHandshakeComplete()) // Vanilla
      {
        user.getForgeClientHandler().setHandshakeComplete();
      }

      if (user.isNeedLogin()) //Aegis
      {
        user.setNeedLogin(false); //Aegis
        // Once again, first connection
        user.setClientEntityId(login.getEntityId());
        user.setServerEntityId(login.getEntityId());

        // Set tab list size, TODO: what shall we do about packet mutability
        final Login modLogin = new Login(login.getEntityId(), login.getGameMode(), login.getPreviousGameMode(), login.getWorldNames(), login.getDimensions(), login.getDimension(), login.getWorldName(), login.getSeed(), login.getDifficulty(), (short)this.user.getPendingConnection().getListener().getTabListSize(), login.getLevelType(), login.getViewDistance(), login.isReducedDebugInfo(), login.isNormalRespawn(), login.isDebug(), login.isFlat());

        user.unsafe().sendPacket(modLogin);
        String brandString = "Aegis (1.7.x-1.16.x)";

        if (ProtocolConstants.isBeforeOrEq(user.getPendingConnection().getVersion(),
            ProtocolConstants.MINECRAFT_1_7_6)) {
          user.unsafe().sendPacket(
              new PluginMessage("MC|Brand", brandString.getBytes(StandardCharsets.UTF_8),
                  handshakeHandler.isServerForge()));
        } else {
          ByteBuf brand = ByteBufAllocator.DEFAULT.heapBuffer();
          DefinedPacket.writeString(brandString, brand);
          user.unsafe().sendPacket(new PluginMessage(
              user.getPendingConnection().getVersion()
                  >= ProtocolConstants.MINECRAFT_1_13 ? "minecraft:brand"
                  : "MC|Brand", brand, handshakeHandler.isServerForge()));
          brand.release();
        }

        user.setDimension(login.getDimension());
        user.getServerSentScoreboard().clear();

      } else {
        if (user.getServer() != null) //Aegis
        {
          user.getServer().setObsolete( true );
          user.getTabListHandler().onServerChange();

          user.getServerSentScoreboard().clear();

          for ( UUID bossbar : user.getSentBossBars() )
          {
            // Send remove bossbar packet
            user.unsafe().sendPacket( new net.md_5.bungee.protocol.packet.BossBar( bossbar, 1 ) );
          }
          user.getSentBossBars().clear();

          user.unsafe().sendPacket( new Respawn( login.getDimension(), login.getWorldName(), login.getSeed(), login.getDifficulty(), login.getGameMode(), login.getPreviousGameMode(), login.getLevelType(), login.isDebug(), login.isFlat(), false ) );
          user.getServer().disconnect( "Quitting" );
        }
        else { //dodalem
          final String brandString = "Aegis";
          if (ProtocolConstants.isBeforeOrEq(this.user.getPendingConnection().getVersion(), 5)) {
            this.user.unsafe().sendPacket(new PluginMessage("MC|Brand", brandString.getBytes(StandardCharsets.UTF_8), this.handshakeHandler.isServerForge()));
          }
          else {
            final ByteBuf brand = ByteBufAllocator.DEFAULT.heapBuffer();
            DefinedPacket.writeString(brandString, brand);
            this.user.unsafe().sendPacket(new PluginMessage((this.user.getPendingConnection().getVersion() >= 393) ? "minecraft:brand" : "MC|Brand", brand, this.handshakeHandler.isServerForge()));
            brand.release();
          }
        }

        this.user.setDimension(login.getDimension()); //dodalem

        this.user.getTabListHandler().onServerChange();
        user.getServerSentScoreboard().clear();

        final Scoreboard serverScoreboard = this.user.getServerSentScoreboard();
        //if (!this.user.isDisableEntityMetadataRewrite()) {
          for (final Objective objective : serverScoreboard.getObjectives()) {
            this.user.unsafe().sendPacket(new ScoreboardObjective(objective.getName(), objective.getValue(), (objective.getType() == null) ? null : ScoreboardObjective.HealthDisplay.fromString(objective.getType()), (byte)1));
          }
          for (final Score score : serverScoreboard.getScores()) {
            this.user.unsafe().sendPacket(new ScoreboardScore(score.getItemName(), (byte)1, score.getScoreName(), score.getValue()));
          }
          for (final Team team : serverScoreboard.getTeams()) {
            this.user.unsafe().sendPacket(new net.md_5.bungee.protocol.packet.Team(team.getName()));
          }
       // }
        serverScoreboard.clear();
        for (final UUID bossbar : this.user.getSentBossBars()) {
          this.user.unsafe().sendPacket(new BossBar(bossbar, 1));
        }
        this.user.getSentBossBars().clear();

        this.user.unsafe().sendPacket(new EntityStatus(this.user.getClientEntityId(),
            (byte) (login.isReducedDebugInfo() ? 22 : 23)));
        if (this.user.getPendingConnection().getVersion() >= 573) {
          this.user.unsafe()
              .sendPacket(new GameState((short) 11, login.isNormalRespawn() ? 0.0f : 1.0f));
        }
        this.user.setDimensionChange(true);
        if (!user.isDisableEntityMetadataRewrite() && login.getDimension()
            .equals(this.user.getDimension())) {
          String worldName = login.getWorldName();
          Object newDim;
          if (login.getDimension() instanceof Integer) {
            newDim = (((int) login.getDimension() >= 0) ? -1 : 0);
          } else {
            worldName = (String) (newDim = ("minecraft:overworld".equals(login.getDimension())
                ? "minecraft:the_nether" : "minecraft:overworld"));
          }
          this.user.unsafe().sendPacket(
              new Respawn(newDim, worldName, login.getSeed(), login.getDifficulty(),
                  login.getGameMode(), login.getPreviousGameMode(), login.getLevelType(),
                  login.isDebug(), login.isFlat(), false));
        }
        this.user.setServerEntityId(login.getEntityId());
        if (user.isDisableEntityMetadataRewrite()) {
          this.user.setClientEntityId(login.getEntityId());
          if (!login.getDimension().equals(this.user.getDimension())) {
            String worldName = login.getWorldName();
            Object newDim;
            if (login.getDimension() instanceof Number) {
              newDim = ((((Number) login.getDimension()).intValue() >= 0) ? -1 : 0);
            } else {
              worldName = (String) (newDim = ("minecraft:overworld".equals(login.getDimension())
                  ? "minecraft:the_nether" : "minecraft:overworld"));
            }
            this.user.unsafe().sendPacket(
                new Respawn(newDim, worldName, login.getSeed(), login.getDifficulty(),
                    login.getGameMode(), login.getPreviousGameMode(), login.getLevelType(),
                    login.isDebug(), login.isFlat(), false));
          }
          final Login modLogin2 = new Login(login.getEntityId(), login.getGameMode(),
              login.getPreviousGameMode(), login.getWorldNames(), login.getDimensions(),
              login.getDimension(), login.getWorldName(), login.getSeed(), login.getDifficulty(),
              (short) this.user.getPendingConnection().getListener().getTabListSize(),
              login.getLevelType(), login.getViewDistance(), login.isReducedDebugInfo(),
              login.isNormalRespawn(), login.isDebug(), login.isFlat());
          this.user.unsafe().sendPacket(modLogin2);
          if (login.getDimension().equals(this.user.getDimension())) {
            String worldName2 = login.getWorldName();
            Object newDim2;
            if (login.getDimension() instanceof Number) {
              newDim2 = ((((Number) login.getDimension()).intValue() >= 0) ? -1 : 0);
            } else {
              worldName2 = (String) (newDim2 = ("minecraft:overworld".equals(login.getDimension())
                  ? "minecraft:the_nether" : "minecraft:overworld"));
            }
            this.user.unsafe().sendPacket(
                new Respawn(newDim2, worldName2, login.getSeed(), login.getDifficulty(),
                    login.getGameMode(), login.getPreviousGameMode(), login.getLevelType(),
                    login.isDebug(), login.isFlat(), false));
          }
        }
        this.user.unsafe().sendPacket(
            new Respawn(login.getDimension(), login.getWorldName(), login.getSeed(),
                login.getDifficulty(), login.getGameMode(), login.getPreviousGameMode(),
                login.getLevelType(), login.isDebug(), login.isFlat(), false));
        if (this.user.getPendingConnection().getVersion() >= 477) {
          this.user.unsafe().sendPacket(new ViewDistance(login.getViewDistance()));
        }
      }
        //}

      if (!user.isActive()) {
        server.disconnect("Quitting");
        bungee.getLogger().warning("No client connected for pending server!");
        return;
      }

      // Add to new server
      // TODO: Move this to the connected() method of DownstreamBridge
      target.addPlayer(user);
      user.getPendingConnects().remove(target);
      user.setServerJoinQueue(null);
      user.setDimensionChange(false);

      final ServerInfo from = (this.user.getServer() == null) ? null : this.user.getServer().getInfo();
      user.setServer(server);
      ch.getHandle().pipeline().get(HandlerBoss.class)
          .setHandler(new DownstreamBridge(bungee, user, server));

      bungee.getPluginManager().callEvent(new ServerSwitchEvent(user, from));

      thisState = State.FINISHED;

      throw CancelSendSignal.INSTANCE;
    }*/
    @Override
    public void handle(Login login) throws Exception {
        Preconditions.checkState(thisState == State.LOGIN, "Not expecting LOGIN");

        ServerConnection server = new ServerConnection(ch, target);
        ServerConnectedEvent event = new ServerConnectedEvent(user, server);
        bungee.getPluginManager().callEvent(event);

        ch.write(BungeeCord.getInstance().registerChannels(user.getPendingConnection().getVersion()));
        Queue<DefinedPacket> packetQueue = target.getPacketQueue();
        synchronized (packetQueue) {
            while (!packetQueue.isEmpty()) {
                ch.write(packetQueue.poll());
            }
        }

        for (PluginMessage message : user.getPendingConnection().getRelayMessages()) {
            ch.write(message);
        }

        if (user.getSettings() != null) {
            ch.write(user.getSettings());
        }

        if (user.getForgeClientHandler().getClientModList() == null && !user.getForgeClientHandler()
                .isHandshakeComplete()) // Vanilla
        {
            user.getForgeClientHandler().setHandshakeComplete();
        }

        if (user.isNeedLogin() || !(login.getDimension() instanceof Integer)) //BotFilter
        {
            user.setNeedLogin(false); //BotFilter
            // Once again, first connection
            user.setClientEntityId(login.getEntityId());
            user.setServerEntityId(login.getEntityId());

            // Set tab list size, TODO: what shall we do about packet mutability
            Login modLogin = new Login(login.getEntityId(), login.isHardcore(), login.getGameMode(), login.getPreviousGameMode(), login.getWorldNames(), login.getDimensions(), login.getDimension(), login.getWorldName(), login.getSeed(), login.getDifficulty(),
                    (byte) user.getPendingConnection().getListener().getTabListSize(), login.getLevelType(), login.getViewDistance(), login.isReducedDebugInfo(), login.isNormalRespawn(), login.isDebug(), login.isFlat());


            user.unsafe().sendPacket(modLogin);

            if (!user.isNeedLogin()) //BotFilter
            {
                if (user.getServer() != null) //BotFilter
                {
                    user.getServer().setObsolete(true); //BotFilter

                }
                user.getTabListHandler().onServerChange();

                user.getServerSentScoreboard().clear();

                for (UUID bossbar : user.getSentBossBars()) {
                    // Send remove bossbar packet
                    user.unsafe().sendPacket(new BossBar(bossbar, 1));
                }
                user.getSentBossBars().clear();

                user.unsafe().sendPacket(
                        new Respawn(login.getDimension(), login.getWorldName(), login.getSeed(),
                                login.getDifficulty(), login.getGameMode(), login.getPreviousGameMode(),
                                login.getLevelType(), login.isDebug(), login.isFlat(), false));

                if (user.getServer() != null) //BotFilter
                {
                    user.getServer().disconnect("Quitting"); //BotFilter
                }
            } else if (user.isNeedLogin()) //BotFilters
            {
                ByteBuf brand = ByteBufAllocator.DEFAULT.heapBuffer();
                DefinedPacket.writeString("Aegis", brand);
                user.unsafe().sendPacket(new PluginMessage(
                        user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_13
                                ? "minecraft:brand" : "MC|Brand", DefinedPacket.toArray(brand),
                        handshakeHandler.isServerForge()));
                brand.release();
            }

            user.setDimension(login.getDimension());
        } else {
            if (user.getServer() != null) //BotFilter
            {
                user.getServer().setObsolete(true); //BotFilter
            }
            user.getTabListHandler().onServerChange();

            Scoreboard serverScoreboard = user.getServerSentScoreboard();
            for (Objective objective : serverScoreboard.getObjectives()) {
                user.unsafe().sendPacket(new ScoreboardObjective(objective.getName(), objective.getValue(),
                        ScoreboardObjective.HealthDisplay.fromString(objective.getType()), (byte) 1));
            }
            for (Score score : serverScoreboard.getScores()) {
                user.unsafe().sendPacket(
                        new ScoreboardScore(score.getItemName(), (byte) 1, score.getScoreName(),
                                score.getValue()));
            }
            for (Team team : serverScoreboard.getTeams()) {
                user.unsafe().sendPacket(new net.md_5.bungee.protocol.packet.Team(team.getName()));
            }
            serverScoreboard.clear();

            for (UUID bossbar : user.getSentBossBars()) {
                // Send remove bossbar packet
                user.unsafe().sendPacket(new BossBar(bossbar, 1));
            }
            user.getSentBossBars().clear();

            // Update debug info from login packet
            user.unsafe().sendPacket(new EntityStatus(user.getClientEntityId(),
                    login.isReducedDebugInfo() ? EntityStatus.DEBUG_INFO_REDUCED
                            : EntityStatus.DEBUG_INFO_NORMAL));
            // And immediate respawn
            if (user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_15) {
                user.unsafe().sendPacket(
                        new GameState(GameState.IMMEDIATE_RESPAWN, login.isNormalRespawn() ? 0 : 1));
            }

            user.setDimensionChange(true);
            if (login.getDimension() == user.getDimension()) {
                user.unsafe().sendPacket(
                        new Respawn((Integer) login.getDimension() >= 0 ? -1 : 0, login.getWorldName(),
                                login.getSeed(), login.getDifficulty(), login.getGameMode(),
                                login.getPreviousGameMode(), login.getLevelType(), login.isDebug(), login.isFlat(),
                                false));
            }

            user.setServerEntityId(login.getEntityId());
            user.unsafe().sendPacket(
                    new Respawn(login.getDimension(), login.getWorldName(), login.getSeed(),
                            login.getDifficulty(), login.getGameMode(), login.getPreviousGameMode(),
                            login.getLevelType(), login.isDebug(), login.isFlat(), false));
            if (user.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_14) {
                user.unsafe().sendPacket(new ViewDistance(login.getViewDistance()));
            }
            user.setDimension(login.getDimension());

            // Remove from old servers
            if (this.user.getServer() != null) //BotFilter
            {
                this.user.getServer().disconnect("Quitting"); //BotFilter
            }
        }

        // TODO: Fix this?
        if (!user.isActive()) {
            server.disconnect("Quitting");
            // Silly server admins see stack trace and die
            bungee.getLogger().warning("No client connected for pending server!");
            return;
        }

        // Add to new server
        // TODO: Move this to the connected() method of DownstreamBridge
        target.addPlayer(user);
        user.getPendingConnects().remove(target);
        user.setServerJoinQueue(null);
        user.setDimensionChange(false);

        ServerInfo from = (user.getServer() == null) ? null : user.getServer().getInfo();
        user.setServer(server);
        ch.getHandle().pipeline().get(HandlerBoss.class)
                .setHandler(new DownstreamBridge(bungee, user, server));

        bungee.getPluginManager().callEvent(new ServerSwitchEvent(user, from));

        thisState = State.FINISHED;

        throw CancelSendSignal.INSTANCE;
    }

  /*@Override
  public void handle(final Login login) throws Exception {
    Preconditions.checkState(this.thisState == State.LOGIN, (Object)"Not expecting LOGIN");
    final ServerConnection server = new ServerConnection(this.ch, this.target);
    final ServerConnectedEvent event = new ServerConnectedEvent(this.user, server);
    this.bungee.getPluginManager().<ServerConnectedEvent>callEvent(event);
    this.ch.write(BungeeCord.getInstance().registerChannels(this.user.getPendingConnection().getVersion()));
    final Queue<DefinedPacket> packetQueue = this.target.getPacketQueue();
    synchronized (packetQueue) {
      while (!packetQueue.isEmpty()) {
        this.ch.write(packetQueue.poll());
      }
    }
    for (final PluginMessage message : this.user.getPendingConnection().getRelayMessages()) {
      this.ch.write(message);
    }
    if (!this.user.isDisableEntityMetadataRewrite() && this.user.getSettings() != null) {
      this.ch.write(this.user.getSettings());
    }
    if (this.user.getForgeClientHandler().getClientModList() == null && !this.user.getForgeClientHandler().isHandshakeComplete()) {
      this.user.getForgeClientHandler().setHandshakeComplete();
    }
    if (this.user.getServer() == null) {
      this.user.setClientEntityId(login.getEntityId());
      this.user.setServerEntityId(login.getEntityId());
      final Login modLogin = new Login(login.getEntityId(), login.getGameMode(), login.getPreviousGameMode(), login.getWorldNames(), login.getDimensions(), login.getDimension(), login.getWorldName(), login.getSeed(), login.getDifficulty(), (short)this.user.getPendingConnection().getListener().getTabListSize(), login.getLevelType(), login.getViewDistance(), login.isReducedDebugInfo(), login.isNormalRespawn(), login.isDebug(), login.isFlat());
      this.user.unsafe().sendPacket(modLogin);
      final String brandString = "Aegis";
      if (ProtocolConstants.isBeforeOrEq(this.user.getPendingConnection().getVersion(), 5)) {
        this.user.unsafe().sendPacket(new PluginMessage("MC|Brand", brandString.getBytes(StandardCharsets.UTF_8), this.handshakeHandler.isServerForge()));
      }
      else {
        final ByteBuf brand = ByteBufAllocator.DEFAULT.heapBuffer();
        DefinedPacket.writeString(brandString, brand);
        this.user.unsafe().sendPacket(new PluginMessage((this.user.getPendingConnection().getVersion() >= 393) ? "minecraft:brand" : "MC|Brand", brand, this.handshakeHandler.isServerForge()));
        brand.release();
      }
      this.user.setDimension(login.getDimension());
    }
    else {
      this.user.getServer().setObsolete(true);
      this.user.getTabListHandler().onServerChange();
      final Scoreboard serverScoreboard = this.user.getServerSentScoreboard();
      if (!user.isDisableEntityMetadataRewrite()) {
        for (final Objective objective : serverScoreboard.getObjectives()) {
          this.user.unsafe().sendPacket(new ScoreboardObjective(objective.getName(), objective.getValue(), (objective.getType() == null) ? null : ScoreboardObjective.HealthDisplay.fromString(objective.getType()), (byte)1));
        }
        for (final Score score : serverScoreboard.getScores()) {
          this.user.unsafe().sendPacket(new ScoreboardScore(score.getItemName(), (byte)1, score.getScoreName(), score.getValue()));
        }
        for (final Team team : serverScoreboard.getTeams()) {
          this.user.unsafe().sendPacket(new net.md_5.bungee.protocol.packet.Team(team.getName()));
        }
      }
      serverScoreboard.clear();
      for (final UUID bossbar : this.user.getSentBossBars()) {
        this.user.unsafe().sendPacket(new BossBar(bossbar, 1));
      }
      this.user.getSentBossBars().clear();
      this.user.unsafe().sendPacket(new EntityStatus(this.user.getClientEntityId(), (byte)(login.isReducedDebugInfo() ? 22 : 23)));
      if (this.user.getPendingConnection().getVersion() >= 573) {
        this.user.unsafe().sendPacket(new GameState((short)11, login.isNormalRespawn() ? 0.0f : 1.0f));
      }
      this.user.setDimensionChange(true);
      if (!user.isDisableEntityMetadataRewrite() && login.getDimension().equals(this.user.getDimension())) {
        String worldName = login.getWorldName();
        Object newDim;
        if (login.getDimension() instanceof Integer) {
          newDim = (((int)login.getDimension() >= 0) ? -1 : 0);
        }
        else {
          worldName = (String)(newDim = ("minecraft:overworld".equals(login.getDimension()) ? "minecraft:the_nether" : "minecraft:overworld"));
        }
        this.user.unsafe().sendPacket(new Respawn(newDim, worldName, login.getSeed(), login.getDifficulty(), login.getGameMode(), login.getPreviousGameMode(), login.getLevelType(), login.isDebug(), login.isFlat(), false));
      }
      this.user.setServerEntityId(login.getEntityId());
      if (Settings.IMP.AEGIS_SETTINGS.DISABLE_ENTITY_METADATA_REWRITE) {
        this.user.setClientEntityId(login.getEntityId());
        if (!login.getDimension().equals(this.user.getDimension())) {
          String worldName = login.getWorldName();
          Object newDim;
          if (login.getDimension() instanceof Number) {
            newDim = ((((Number)login.getDimension()).intValue() >= 0) ? -1 : 0);
          }
          else {
            worldName = (String)(newDim = ("minecraft:overworld".equals(login.getDimension()) ? "minecraft:the_nether" : "minecraft:overworld"));
          }
          this.user.unsafe().sendPacket(new Respawn(newDim, worldName, login.getSeed(), login.getDifficulty(), login.getGameMode(), login.getPreviousGameMode(), login.getLevelType(), login.isDebug(), login.isFlat(), false));
        }
        final Login modLogin2 = new Login(login.getEntityId(), login.getGameMode(), login.getPreviousGameMode(), login.getWorldNames(), login.getDimensions(), login.getDimension(), login.getWorldName(), login.getSeed(), login.getDifficulty(), (short)this.user.getPendingConnection().getListener().getTabListSize(), login.getLevelType(), login.getViewDistance(), login.isReducedDebugInfo(), login.isNormalRespawn(), login.isDebug(), login.isFlat());
        this.user.unsafe().sendPacket(modLogin2);
        if (login.getDimension().equals(this.user.getDimension())) {
          String worldName2 = login.getWorldName();
          Object newDim2;
          if (login.getDimension() instanceof Number) {
            newDim2 = ((((Number)login.getDimension()).intValue() >= 0) ? -1 : 0);
          }
          else {
            worldName2 = (String)(newDim2 = ("minecraft:overworld".equals(login.getDimension()) ? "minecraft:the_nether" : "minecraft:overworld"));
          }
          this.user.unsafe().sendPacket(new Respawn(newDim2, worldName2, login.getSeed(), login.getDifficulty(), login.getGameMode(), login.getPreviousGameMode(), login.getLevelType(), login.isDebug(), login.isFlat(), false));
        }
      }
      this.user.unsafe().sendPacket(new Respawn(login.getDimension(), login.getWorldName(), login.getSeed(), login.getDifficulty(), login.getGameMode(), login.getPreviousGameMode(), login.getLevelType(), login.isDebug(), login.isFlat(), false));
      if (this.user.getPendingConnection().getVersion() >= 477) {
        this.user.unsafe().sendPacket(new ViewDistance(login.getViewDistance()));
      }
      this.user.setDimension(login.getDimension());
      this.user.getServer().disconnect("Quitting");
    }
    if (!this.user.isActive()) {
      server.disconnect("Quitting");
      this.bungee.getLogger().warning("No client connected for pending server!");
      return;
    }
    this.target.addPlayer(this.user);
    this.user.getPendingConnects().remove(this.target);
    this.user.setServerJoinQueue(null);
    this.user.setDimensionChange(false);
    final ServerInfo from = (this.user.getServer() == null) ? null : this.user.getServer().getInfo();
    this.user.setServer(server);
    ((HandlerBoss)this.ch.getHandle().pipeline().<HandlerBoss>get(HandlerBoss.class)).setHandler(new DownstreamBridge(this.bungee, this.user, server));
    this.bungee.getPluginManager().<ServerSwitchEvent>callEvent(new ServerSwitchEvent(this.user, from));
    this.thisState = State.FINISHED;
    throw CancelSendSignal.INSTANCE;
  }*/

    @Override
    public void handle(EncryptionRequest encryptionRequest) throws Exception {
        throw new QuietException("Server is online mode!");
    }

    @Override
    public void handle(Kick kick) throws Exception {
        ServerInfo def = user.updateAndGetNextServer(target);
        ServerKickEvent event = new ServerKickEvent(user, target,
                ComponentSerializer.parse(kick.getMessage()), def, ServerKickEvent.State.CONNECTING);
        if (event.getKickReason().toLowerCase(Locale.ROOT).contains("outdated") && def != null) {
            // Pre cancel the event if we are going to try another server
            event.setCancelled(true);
        }
        bungee.getPluginManager().callEvent(event);
        if (event.isCancelled() && event.getCancelServer() != null) {
            obsolete = true;
            user.connect(event.getCancelServer(), ServerConnectEvent.Reason.KICK_REDIRECT);
            throw CancelSendSignal.INSTANCE;
        }

        String message = bungee.getTranslation("connect_kick", target.getName(), event.getKickReason());
        if (user.isDimensionChange()) {
            user.disconnect(message);
        } else {
            user.sendMessage(message);
        }

        throw CancelSendSignal.INSTANCE;
    }

    @Override
    public void handle(PluginMessage pluginMessage) throws Exception {
        if (BungeeCord.getInstance().config.isForgeSupport()) {
            if (pluginMessage.getTag().equals(ForgeConstants.FML_REGISTER)) {
                Set<String> channels = ForgeUtils.readRegisteredChannels(pluginMessage);
                boolean isForgeServer = false;
                for (String channel : channels) {
                    if (channel.equals(ForgeConstants.FML_HANDSHAKE_TAG)) {
                        // If we have a completed handshake and we have been asked to register a FML|HS
                        // packet, let's send the reset packet now. Then, we can continue the message sending.
                        // The handshake will not be complete if we reset this earlier.
                        if (user.getServer() != null && user.getForgeClientHandler().isHandshakeComplete()) {
                            user.getForgeClientHandler().resetHandshake();
                        }

                        isForgeServer = true;
                        break;
                    }
                }

                if (isForgeServer && !this.handshakeHandler.isServerForge()) {
                    // We now set the server-side handshake handler for the client to this.
                    handshakeHandler.setServerAsForgeServer();
                    user.setForgeServerHandler(handshakeHandler);
                }
            }

            if (pluginMessage.getTag().equals(ForgeConstants.FML_HANDSHAKE_TAG) || pluginMessage.getTag()
                    .equals(ForgeConstants.FORGE_REGISTER)) {
                this.handshakeHandler.handle(pluginMessage);

                // We send the message as part of the handler, so don't send it here.
                throw CancelSendSignal.INSTANCE;
            }
        }

        // We have to forward these to the user, especially with Forge as stuff might break
        // This includes any REGISTER messages we intercepted earlier.
        user.unsafe().sendPacket(pluginMessage);
    }

    @Override
    public String toString() {
        return "[" + user.getName() + "] <-> ServerConnector [" + target.getName() + "]";
    }

    private enum State {

        LOGIN_SUCCESS, ENCRYPT_RESPONSE, LOGIN, FINISHED;
    }
}