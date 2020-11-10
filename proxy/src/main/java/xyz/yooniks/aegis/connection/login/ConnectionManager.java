package xyz.yooniks.aegis.connection.login;

import java.util.HashMap;
import java.util.Map;
import net.md_5.bungee.api.connection.Connection;

public class ConnectionManager {

  private final Map<String, PlayerConnection> connections = new HashMap<>();

  public PlayerConnection findConnection(Connection connection) {
    return this.connections.get(connection.getAddress().getAddress().getHostAddress());
  }

  public void addConnectionPerSecond(Connection connection) {
    final String address = connection.getAddress().getAddress().getHostAddress();
    PlayerConnection playerConnection = this.connections.get(address);
    if (playerConnection == null) {
      final long time = System.currentTimeMillis();
      playerConnection = new PlayerConnection(1, 0L, time);
    } else {
      playerConnection.setCount(playerConnection.getCount() + 1);
      if (System.currentTimeMillis() - playerConnection.getLastTotalConnection() < 1000) {
        playerConnection.setSuspiciousCount(playerConnection.getSuspiciousCount() + 1);
      } else {
        playerConnection.setSuspiciousCount(0);
      }
      playerConnection.setLastTotalConnection(System.currentTimeMillis());
    }
    this.connections.put(address, playerConnection);
  }

  public void login(Connection connection) {
    final String address = connection.getAddress().getAddress().getHostAddress();
    PlayerConnection playerConnection = this.connections.get(address);
    if (playerConnection == null) {
      playerConnection = new PlayerConnection(1, System.currentTimeMillis());
    } else {
      playerConnection.setCount(playerConnection.getCount() + 1);
      if (System.currentTimeMillis() - playerConnection.getLastConnection() < 1000) {
        playerConnection.setSuspiciousCount(playerConnection.getSuspiciousCount() + 1);
      } else {
        playerConnection.setSuspiciousCount(0);
      }
      playerConnection.setLastConnection(System.currentTimeMillis());
    }
    this.connections.put(address, playerConnection);
  }
}
