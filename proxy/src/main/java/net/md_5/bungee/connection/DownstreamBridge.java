package net.md_5.bungee.connection;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.waterfallmc.waterfall.event.ProxyDefineCommandsEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.unix.DomainSocketAddress;
import net.md_5.bungee.ServerConnection;
import net.md_5.bungee.ServerConnection.KeepAliveData;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.score.Team;
import net.md_5.bungee.api.score.*;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.entitymap.EntityMap;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.*;
import net.md_5.bungee.tab.TabList;
import xyz.yooniks.aegis.config.Settings;

import java.io.DataInput;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

//@RequiredArgsConstructor //BotFilter - removed
public class DownstreamBridge extends PacketHandler {

  private final ProxyServer bungee;
  private final UserConnection con;
  private final ServerConnection server;

  //BotFilter start
  public DownstreamBridge(ProxyServer bungee, UserConnection con, ServerConnection server) {
    this.bungee = bungee;
    this.con = con;
    this.server = server;

    if (!con.getDelayedPluginMessages().isEmpty()) {
      for (PluginMessage msg : con.getDelayedPluginMessages()) {
        server.getCh().write(msg);
      }
      con.getDelayedPluginMessages().clear();
    }
  }
  //BotFilter end

  @Override
  public void exception(final Throwable t) {
    /*if (this.server.isObsolete()) {
      return;
    }
    final ServerInfo def = this.con.updateAndGetNextServer(this.server.getInfo());
    final ServerKickEvent event = this.bungee.getPluginManager().callEvent(
        new ServerKickEvent(this.con, this.server.getInfo(),
            TextComponent
                .fromLegacyText(this.bungee.getTranslation("server_went_down")), def,
            ServerKickEvent.State.CONNECTED, ServerKickEvent.Cause.EXCEPTION));
    if (event.isCancelled() && event.getCancelServer() != null) {
      this.server.setObsolete(true);
      this.con.connectNow(event.getCancelServer(), ServerConnectEvent.Reason.SERVER_DOWN_REDIRECT);
    } else {
      this.con.disconnect0(event.getKickReasonComponent());
    }*/

    if (this.server.isObsolete()) {
      return;
    }
    final ServerInfo def = this.con.updateAndGetNextServer(this.server.getInfo());
    final ServerKickEvent event = (ServerKickEvent)this.bungee.getPluginManager().<ServerKickEvent>callEvent(new ServerKickEvent((ProxiedPlayer)this.con, (ServerInfo)this.server.getInfo(), TextComponent
        .fromLegacyText(this.bungee.getTranslation("server_went_down")), def, ServerKickEvent.State.CONNECTED, ServerKickEvent.Cause.EXCEPTION));
    if (event.isCancelled() && event.getCancelServer() != null) {
      this.server.setObsolete(true);
      this.con.connectNow(event.getCancelServer(), ServerConnectEvent.Reason.SERVER_DOWN_REDIRECT);
    }
    else {
      this.con.disconnect0(event.getKickReasonComponent());
    }
  }

  @Override
  public void disconnected(final ChannelWrapper channel) throws Exception {
    /*this.server.getInfo().removePlayer(this.con);
    if (this.bungee.getReconnectHandler() != null) {
      this.bungee.getReconnectHandler().setServer(this.con);
    }

    Aegis.getInstance().getAegisClassLoaded().getSerializable().toString();

    if (!this.server.isObsolete()) {
      final ServerInfo def = this.con.updateAndGetNextServer(this.server.getInfo());
      final ServerKickEvent event = this.bungee.getPluginManager()
          .callEvent(new ServerKickEvent(this.con,
              this.server.getInfo(), TextComponent
              .fromLegacyText(this.bungee.getTranslation("lost_connection")), def,
              ServerKickEvent.State.CONNECTED, ServerKickEvent.Cause.LOST_CONNECTION));
      if (event.isCancelled() && event.getCancelServer() != null) {
        this.server.setObsolete(true);
        this.con.connectNow(event.getCancelServer());
      } else {
        this.con.disconnect0(event.getKickReasonComponent());
      }
    }
    final ServerDisconnectEvent serverDisconnectEvent = new ServerDisconnectEvent(this.con,
        this.server.getInfo());
    this.bungee.getPluginManager().callEvent(serverDisconnectEvent);
      if (Settings.IMP.AEGIS_SETTINGS.LICENSE.contains("t0r")) {
          Aegis.getInstance().disable();
      }*/
    this.server.getInfo().removePlayer(this.con);
    if (this.bungee.getReconnectHandler() != null) {
      this.bungee.getReconnectHandler().setServer(this.con);
    }

    if (!this.server.isObsolete()) {
      final ServerInfo def = this.con.updateAndGetNextServer(this.server.getInfo());
      final ServerKickEvent event = (ServerKickEvent)this.bungee.getPluginManager().<ServerKickEvent>callEvent(new ServerKickEvent((ProxiedPlayer)this.con, (ServerInfo)this.server.getInfo(), TextComponent.fromLegacyText(bungee.getTranslation( "lost_connection" )), def, ServerKickEvent.State.CONNECTED, ServerKickEvent.Cause.LOST_CONNECTION));
      if (event.isCancelled() && event.getCancelServer() != null) {
        this.server.setObsolete(true);
        this.con.connectNow(event.getCancelServer());
      }
      else {
        this.con.disconnect0(event.getKickReasonComponent());
      }
    }

    final ServerDisconnectEvent serverDisconnectEvent = new ServerDisconnectEvent(this.con, this.server.getInfo());
    this.bungee.getPluginManager().<ServerDisconnectEvent>callEvent(serverDisconnectEvent);
  }

  @Override
  public boolean shouldHandle(PacketWrapper packet) throws Exception {
    return !server.isObsolete();
  }

  @Override
  public void handle(PacketWrapper packet) throws Exception {
   // con.getEntityRewrite()
     //   .rewriteClientbound(packet.buf, con.getServerEntityId(), con.getClientEntityId(),
     //       con.getPendingConnection().getVersion());
    EntityMap rewrite = con.getEntityRewrite();
    if ( rewrite != null )
    {
      rewrite.rewriteClientbound( packet.buf, con.getServerEntityId(), con.getClientEntityId(), con.getPendingConnection().getVersion() );
    }
    con.sendPacket(packet);
  }

  @Override
  public void handle(KeepAlive alive) throws Exception {
    if (server.getKeepAlives().size() < bungee.getConfig().getTimeout()
        / 50) // Allow a theoretical maximum of 1 keepalive per tick
    {
      server.getKeepAlives()
          .add(new KeepAliveData(alive.getRandomId(), System.currentTimeMillis()));
    }
  }

  @Override
  public void handle(PlayerListItem playerList) throws Exception {
    con.getTabListHandler().onUpdate(TabList.rewrite(playerList));
    throw CancelSendSignal.INSTANCE; // Always throw because of profile rewriting
  }

  @Override
  public void handle(ScoreboardObjective objective) throws Exception {
    Scoreboard serverScoreboard = con.getServerSentScoreboard();
    switch (objective.getAction()) {
      case 0:
        //serverScoreboard.addObjective( new Objective( objective.getName(), objective.getValue(), objective.getType().toString() ) );
        serverScoreboard.addObjective(new Objective(objective.getName(), objective.getValue(),
            objective.getType() != null ? objective.getType().toString()
                : null)); // Travertine - 1.7 protocol support
        break;
      case 1:
        serverScoreboard.removeObjective(objective.getName());
        break;
      case 2:
        Objective oldObjective = serverScoreboard.getObjective(objective.getName());
        if (oldObjective != null) {
          oldObjective.setValue(objective.getValue());
          //oldObjective.setType( objective.getType().toString() );
          oldObjective.setType(objective.getType() != null ? objective.getType().toString()
              : null); // Travertine - 1.7 protocol support
        }
        break;
      default:
        break;
        //throw new IllegalArgumentException("Unknown objective action: " + objective.getAction());
    }
  }

  @Override
  public void handle(ScoreboardScore score) throws Exception {
    Scoreboard serverScoreboard = con.getServerSentScoreboard();
    switch (score.getAction()) {
      case 0:
        Score s = new Score(score.getItemName(), score.getScoreName(), score.getValue());
        serverScoreboard.removeScore(score.getItemName());
        serverScoreboard.addScore(s);
        break;
      case 1:
        serverScoreboard.removeScore(score.getItemName());
        break;
      default:
        break;
      //throw new IllegalArgumentException( "Unknown scoreboard action: " + score.getAction() );
    }
  }

  @Override
  public void handle(ScoreboardDisplay displayScoreboard) throws Exception {
    Scoreboard serverScoreboard = con.getServerSentScoreboard();
    serverScoreboard.setName(displayScoreboard.getName());
    serverScoreboard.setPosition(Position.values()[displayScoreboard.getPosition()]);
  }

  @Override
  public void handle(net.md_5.bungee.protocol.packet.Team team) throws Exception {
    Scoreboard serverScoreboard = con.getServerSentScoreboard();
    // Remove team and move on
    if (team.getMode() == 1) {
      serverScoreboard.removeTeam(team.getName());
      return;
    }

    // Create or get old team
    Team t;
    if (team.getMode() == 0) {
      t = new Team(team.getName());
      serverScoreboard.addTeam(t);
    } else {
      t = serverScoreboard.getTeam(team.getName());
    }

    if (t != null) {
      if (team.getMode() == 0 || team.getMode() == 2) {
        t.setDisplayName(team.getDisplayName());
        t.setPrefix(team.getPrefix());
        t.setSuffix(team.getSuffix());
        t.setFriendlyFire(team.getFriendlyFire());
        t.setNameTagVisibility(team.getNameTagVisibility());
        t.setCollisionRule(team.getCollisionRule());
        t.setColor(team.getColor());
      }
      if (team.getPlayers() != null) {
        for (String s : team.getPlayers()) {
          if (team.getMode() == 0 || team.getMode() == 3) {
            t.addPlayer(s);
          } else if (team.getMode() == 4) {
            t.removePlayer(s);
          }
        }
      }
    }
  }

  private int rewriteEntityId(final int entityId) {
    if (entityId == this.con.getServerEntityId()) {
      return this.con.getClientEntityId();
    }
    return entityId;
  }

  @Override
  public void handle(final EntityEffect entityEffect) {
        if (this.con.isDisableEntityMetadataRewrite()) {
            return;
        }
        if (this.con.getForgeClientHandler().isForgeUser() && !this.con.getForgeClientHandler().isHandshakeComplete()) {
            throw CancelSendSignal.INSTANCE;
        }
        this.con.getPotions().put(this.rewriteEntityId(entityEffect.getEntityId()), entityEffect.getEffectId());
  }

  @Override
  public void handle(final EntityRemoveEffect removeEffect) {
        if (this.con.isDisableEntityMetadataRewrite()) {
            return;
        }
        this.con.getPotions().remove(this.rewriteEntityId(removeEffect.getEntityId()), removeEffect.getEffectId());
  }

  @Override
  public void handle(PluginMessage pluginMessage) throws Exception {
    DataInput in = pluginMessage.getStream();
    PluginMessageEvent event = new PluginMessageEvent(server, con, pluginMessage.getTag(),
        pluginMessage.getData().clone());

    if (bungee.getPluginManager().callEvent(event).isCancelled()) {
      throw CancelSendSignal.INSTANCE;
    }

    if (pluginMessage.getTag().equals(
        (this.con.getPendingConnection().getVersion() >= 393) ? "minecraft:brand" : "MC|Brand")) {
      Label_0264:
      {
        if (ProtocolConstants.isAfterOrEq(this.con.getPendingConnection().getVersion(), 47)) {
          try {
            ByteBuf brand = Unpooled.wrappedBuffer(pluginMessage.getData());
            brand.release();
            brand = ByteBufAllocator.DEFAULT.heapBuffer();
            DefinedPacket.writeString("Aegis v" + Settings.IMP.AEGIS_VERSION, brand);
            pluginMessage.setData(brand);
            brand.release();
            break Label_0264;
          } catch (Exception ProtocolHacksSuck) {
            return;
          }
        }
        pluginMessage.setData(("Aegis v" + Settings.IMP.AEGIS_VERSION).getBytes(
            StandardCharsets.UTF_8));
      }
      this.con.unsafe().sendPacket(pluginMessage);
      throw CancelSendSignal.INSTANCE;
    }

    if (pluginMessage.getTag().equals("BungeeCord")) {
      ByteArrayDataOutput out = ByteStreams.newDataOutput();
      String subChannel = in.readUTF();

      if (subChannel.equals("ForwardToPlayer")) {
        ProxiedPlayer target = bungee.getPlayer(in.readUTF());
        if (target != null) {
          // Read data from server
          String channel = in.readUTF();
          short len = in.readShort();
          byte[] data = new byte[len];
          in.readFully(data);

          // Prepare new data to send
          out.writeUTF(channel);
          out.writeShort(data.length);
          out.write(data);
          byte[] payload = out.toByteArray();

          target.getServer().sendData("BungeeCord", payload);
        }
        // Null out stream, important as we don't want to send to ourselves
        out = null;
      }
      if ( subChannel.equals( "IPOther" ) )
      {
        ProxiedPlayer player = bungee.getPlayer( in.readUTF() );
        if ( player != null )
        {
          out.writeUTF( "IPOther" );
          out.writeUTF( player.getName() );
          if ( player.getSocketAddress() instanceof InetSocketAddress )
          {
            InetSocketAddress address = (InetSocketAddress) player.getSocketAddress();
            out.writeUTF( address.getHostString() );
            out.writeInt( address.getPort() );
          } else
          {
            out.writeUTF( "unix://" + ( (DomainSocketAddress) player.getSocketAddress() ).path() );
            out.writeInt( 0 );
          }
        }
      }
      if (subChannel.equals("Forward")) {
        // Read data from server
        String target = in.readUTF();
        String channel = in.readUTF();
        short len = in.readShort();
        byte[] data = new byte[len];
        in.readFully(data);

        // Prepare new data to send
        out.writeUTF(channel);
        out.writeShort(data.length);
        out.write(data);
        byte[] payload = out.toByteArray();

        // Null out stream, important as we don't want to send to ourselves
        out = null;

        if (target.equals("ALL")) {
          for (ServerInfo server : bungee.getServers().values()) {
            if (server != this.server.getInfo()) {
              server.sendData("BungeeCord", payload);
            }
          }
        } else if (target.equals("ONLINE")) {
          for (ServerInfo server : bungee.getServers().values()) {
            if (server != this.server.getInfo()) {
              server.sendData("BungeeCord", payload, false);
            }
          }
        } else {
          ServerInfo server = bungee.getServerInfo(target);
          if (server != null) {
            server.sendData("BungeeCord", payload);
          }
        }
      }
      if (subChannel.equals("Connect")) {
        ServerInfo server = bungee.getServerInfo(in.readUTF());
        if (server != null) {
          con.connect(server, ServerConnectEvent.Reason.PLUGIN_MESSAGE);
        }
      }
      if (subChannel.equals("ConnectOther")) {
        ProxiedPlayer player = bungee.getPlayer(in.readUTF());
        if (player != null) {
          ServerInfo server = bungee.getServerInfo(in.readUTF());
          if (server != null) {
            player.connect(server);
          }
        }
      }
      if (subChannel.equals("IP")) {
        out.writeUTF("IP");
        if (con.getSocketAddress() instanceof InetSocketAddress) {
          out.writeUTF(con.getAddress().getHostString());
          out.writeInt(con.getAddress().getPort());
        } else {
          out.writeUTF("unix://" + ((DomainSocketAddress) con.getSocketAddress()).path());
          out.writeInt(0);
        }
      }
      if (subChannel.equals("PlayerCount")) {
        String target = in.readUTF();
        out.writeUTF("PlayerCount");
        if (target.equals("ALL")) {
          out.writeUTF("ALL");
          out.writeInt(bungee.getOnlineCount());
        } else {
          ServerInfo server = bungee.getServerInfo(target);
          if (server != null) {
            out.writeUTF(server.getName());
            out.writeInt(server.getPlayers().size());
          }
        }
      }
      if (subChannel.equals("PlayerList")) {
        String target = in.readUTF();
        out.writeUTF("PlayerList");
        if (target.equals("ALL")) {
          out.writeUTF("ALL");
          out.writeUTF(Util.csv(bungee.getPlayers()));
        } else {
          ServerInfo server = bungee.getServerInfo(target);
          if (server != null) {
            out.writeUTF(server.getName());
            out.writeUTF(Util.csv(server.getPlayers()));
          }
        }
      }
      if (subChannel.equals("GetServers")) {
        out.writeUTF("GetServers");
        out.writeUTF(Util.csv(bungee.getServers().keySet()));
      }
      if (subChannel.equals("Message")) {
        String target = in.readUTF();
        String message = in.readUTF();
        if (target.equals("ALL")) {
          for (ProxiedPlayer player : bungee.getPlayers()) {
            player.sendMessage(message);
          }
        } else {
          ProxiedPlayer player = bungee.getPlayer(target);
          if (player != null) {
            player.sendMessage(message);
          }
        }
      }
      if ( subChannel.equals( "MessageRaw" ) )
      {
        String target = in.readUTF();
        BaseComponent[] message = ComponentSerializer.parse( in.readUTF() );
        if ( target.equals( "ALL" ) )
        {
          for ( ProxiedPlayer player : bungee.getPlayers() )
          {
            player.sendMessage( message );
          }
        } else
        {
          ProxiedPlayer player = bungee.getPlayer( target );
          if ( player != null )
          {
            player.sendMessage( message );
          }
        }
      }
      if (subChannel.equals("GetServer")) {
        out.writeUTF("GetServer");
        out.writeUTF(server.getInfo().getName());
      }
      if (subChannel.equals("UUID")) {
        out.writeUTF("UUID");
        out.writeUTF(con.getUUID());
      }
      if (subChannel.equals("UUIDOther")) {
        ProxiedPlayer player = bungee.getPlayer(in.readUTF());
        if (player != null) {
          out.writeUTF("UUIDOther");
          out.writeUTF(player.getName());
          out.writeUTF(player.getUUID());
        }
      }
      if (subChannel.equals("ServerIP")) {
        ServerInfo info = bungee.getServerInfo(in.readUTF());
        if (info != null && !info.getAddress().isUnresolved()) {
          out.writeUTF("ServerIP");
          out.writeUTF(info.getName());
          out.writeUTF(info.getAddress().getAddress().getHostAddress());
          out.writeShort(info.getAddress().getPort());
        }
      }
      if (subChannel.equals("KickPlayer")) {
        ProxiedPlayer player = bungee.getPlayer(in.readUTF());
        if (player != null) {
          String kickReason = in.readUTF();
          player.disconnect(new TextComponent(kickReason));
        }
      }

      // Check we haven't set out to null, and we have written data, if so reply back back along the BungeeCord channel
      if (out != null) {
        byte[] b = out.toByteArray();
        if (b.length != 0) {
          server.sendData("BungeeCord", b);
        }
      }

      throw CancelSendSignal.INSTANCE;
    }
  }


  @Override
  public void handle(final Kick kick) throws Exception {
    ServerInfo def = this.con.updateAndGetNextServer(this.server.getInfo());
    if (Objects.equals(this.server.getInfo(), def)) {
      def = null;
    }
    final ServerKickEvent event = this.bungee
        .getPluginManager().callEvent(
            new ServerKickEvent(this.con, this.server.getInfo(),
                ComponentSerializer.parse(kick.getMessage()), def, ServerKickEvent.State.CONNECTED,
                ServerKickEvent.Cause.SERVER));
    if (event.isCancelled() && event.getCancelServer() != null) {
      this.con.connectNow(event.getCancelServer(), ServerConnectEvent.Reason.KICK_REDIRECT);
    } else {
      this.con.disconnect0(event.getKickReasonComponent());
    }
    this.server.setObsolete(true);
    throw CancelSendSignal.INSTANCE;
  }

  @Override
  public void handle(SetCompression setCompression) throws Exception {
    server.getCh().setCompressionThreshold(setCompression.getThreshold());
  }

  @Override
  public void handle(TabCompleteResponse tabCompleteResponse) throws Exception {

    List<String> commands = tabCompleteResponse.getCommands();
    if (commands == null) {
      commands = Lists.transform(tabCompleteResponse.getSuggestions().getList(),
          new Function<Suggestion, String>() {
            @Override
            public String apply(Suggestion input) {
              return input.getText();
            }
          });
    }

    TabCompleteResponseEvent tabCompleteResponseEvent = new TabCompleteResponseEvent(server, con,
        new ArrayList<>(commands));
    if (!bungee.getPluginManager().callEvent(tabCompleteResponseEvent).isCancelled()) {
      // Take action only if modified
      if (!commands.equals(tabCompleteResponseEvent.getSuggestions())) {
        if (tabCompleteResponse.getCommands() != null) {
          // Classic style
          tabCompleteResponse.setCommands(tabCompleteResponseEvent.getSuggestions());
        } else {
          // Brigadier style
          final StringRange range = tabCompleteResponse.getSuggestions().getRange();
          tabCompleteResponse.setSuggestions(new Suggestions(range, Lists
              .transform(tabCompleteResponseEvent.getSuggestions(),
                  new Function<String, Suggestion>() {
                    @Override
                    public Suggestion apply(String input) {
                      return new Suggestion(range, input);
                    }
                  })));
        }
      }

      con.unsafe().sendPacket(tabCompleteResponse);
    }

    throw CancelSendSignal.INSTANCE;
  }

  @Override
  public void handle(BossBar bossBar) {
    switch (bossBar.getAction()) {
      // Handle add bossbar
      case 0:
        con.getSentBossBars().add(bossBar.getUuid());
        break;
      // Handle remove bossbar
      case 1:
        con.getSentBossBars().remove(bossBar.getUuid());
        break;
    }
  }

  @Override
  public void handle(Respawn respawn) {
    con.setDimension(respawn.getDimension());
  }

  /*@Override
  public void handle(Commands commands) throws Exception {
    boolean modified = false;

    for (Map.Entry<String, Command> command : bungee.getPluginManager().getCommands()) {
      if (!bungee.getDisabledCommands().contains(command.getKey())
          && commands.getRoot().getChild(command.getKey()) == null && command.getValue()
          .hasPermission(con)) {
        LiteralCommandNode dummy = LiteralArgumentBuilder.literal(command.getKey())
            .then(RequiredArgumentBuilder.argument("args", StringArgumentType.greedyString())
                .suggests(Commands.SuggestionRegistry.ASK_SERVER))
            .build();
        commands.getRoot().addChild(dummy);

        modified = true;
      }
    }

    if (modified) {
      con.unsafe().sendPacket(commands);
      throw CancelSendSignal.INSTANCE;
    }
  }*/

  @Override
  public void handle(final Commands commands) throws Exception {
    boolean modified = false;
    final Map<String, Command> commandMap = new HashMap<>();
    for (final Map.Entry<String, Command> commandEntry : this.bungee.getPluginManager().getCommands()) {
      if (!this.bungee.getDisabledCommands().contains(commandEntry.getKey()) && commands.getRoot().getChild(commandEntry.getKey()) == null && commandEntry.getValue()
          .hasPermission(this.con)) {
        commandMap.put(commandEntry.getKey(), commandEntry.getValue());
      }
    }
    final ProxyDefineCommandsEvent event = new ProxyDefineCommandsEvent(this.server, this.con, commandMap);
    this.bungee.getPluginManager().callEvent(event);
    for (final Map.Entry<String, Command> command : event.getCommands().entrySet()) {
      final LiteralCommandNode dummy = (LiteralArgumentBuilder.literal(command.getKey())
          .then(RequiredArgumentBuilder.argument("args",
              StringArgumentType.greedyString()).suggests(Commands.SuggestionRegistry.ASK_SERVER))).build();
      commands.getRoot().addChild(dummy);
      modified = true;
    }
    if (modified) {
      this.con.unsafe().sendPacket(commands);
      throw CancelSendSignal.INSTANCE;
    }
  }

  @Override
  public String toString() {
    final String text = con.getSocketAddress() == null ? con.getName() : con.getName() + "/" + con.getSocketAddress();
    return "[" + text + "] <-> DownstreamBridge <-> ["
        + server.getInfo().getName() + "]";
  }
}
