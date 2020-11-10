package xyz.yooniks.aegis.queue;

import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class Queue {

  private final ProxiedPlayer player;
  private final UserConnection connection;
  private final ServerInfo targetServer;
  private int place;

  public Queue(ProxiedPlayer player, UserConnection connection,
      ServerInfo targetServer, int place) {
    this.player = player;
    this.connection = connection;
    this.targetServer = targetServer;
    this.place = place;
  }

  public ServerInfo getTargetServer() {
    return targetServer;
  }

  public UserConnection getConnection() {
    return connection;
  }

  public ProxiedPlayer getPlayer() {
    return player;
  }

  public int getPlace() {
    return place;
  }

  public void setPlace(int place) {
    this.place = place;
  }
}
