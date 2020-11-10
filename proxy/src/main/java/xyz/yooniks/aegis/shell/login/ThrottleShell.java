package xyz.yooniks.aegis.shell.login;

import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.protocol.DefinedPacket;
import xyz.yooniks.aegis.caching.PacketUtils;
import xyz.yooniks.aegis.connection.AegisStatistics;
import xyz.yooniks.aegis.connection.login.ConnectionManager;
import xyz.yooniks.aegis.connection.login.PlayerConnection;
import xyz.yooniks.aegis.shell.BotShell;

public class ThrottleShell implements BotShell {

  private final ConnectionManager connectionManager;
  private final DefinedPacket kickPacket;
  private final int delay;

  public ThrottleShell(ConnectionManager connectionManager, String kickMessage, int delay) {
    this.connectionManager = connectionManager;
    this.kickPacket = PacketUtils.createKickPacket(kickMessage);
    this.delay = delay;
  }

  @Override
  public String getName() {
    return "connection-throttle";
  }

  @Override
  public DefinedPacket getKickPacket(PendingConnection connection) {
    return kickPacket;
  }

  @Override
  public boolean pass(PendingConnection connection) {
    if (connection == null) {
      return true;
    }
    if (this.connectionManager == null) {
      return true;
    }
    final PlayerConnection playerConnection = this.connectionManager.findConnection(connection);
    if (playerConnection == null) {
      //this.connectionManager.login(connection);
      return true;
    }
    if (System.currentTimeMillis() - playerConnection.getLastConnection() < this.delay * 1000L) {
      return false;
    }
    playerConnection.setLastConnection(System.currentTimeMillis());
    return true;
  }

  @Override
  public boolean shouldCheck(AegisStatistics statistics) {
    return true;
  }

}
