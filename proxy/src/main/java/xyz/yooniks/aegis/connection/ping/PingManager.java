package xyz.yooniks.aegis.connection.ping;

import java.util.HashMap;
import java.util.Map;
import net.md_5.bungee.api.connection.Connection;

public class PingManager {

  private final Map<String, Ping> pings = new HashMap<>();

  public Ping findPing(String address) {
    return this.pings.get(address);
  }

  public Ping findPing(Connection connection) {
    return this.pings.get(connection.getAddress().getAddress().getHostAddress());
  }

  public void ping(Connection connection) {
    final String address = connection.getAddress().getAddress().getHostAddress();
    Ping ping = this.pings.get(address);
    if (ping == null) {
      ping = new Ping(1, System.currentTimeMillis());
    } else {
      ping.setCount(ping.getCount() + 1);
      if (System.currentTimeMillis() - ping.getLastPing() < 2000) {
        ping.setSuspiciousCount(ping.getSuspiciousCount() + 1);
      } else {
        ping.setSuspiciousCount(0);
      }
      ping.setLastPing(System.currentTimeMillis());
    }
    this.pings.put(address, ping);
  }


  public Ping getPing(String address) {
    Ping ping = this.pings.get(address);
    if (ping == null) {
      ping = new Ping(1, System.currentTimeMillis());
    } else {
      ping.setCount(ping.getCount() + 1);
      if (System.currentTimeMillis() - ping.getLastPing() < 1000) {
        ping.setSuspiciousCount(ping.getSuspiciousCount() + 1);
      } else {
        ping.setSuspiciousCount(0);
      }
      ping.setLastPing(System.currentTimeMillis());
    }
    this.pings.put(address, ping);
    return ping;
  }

  public Ping getPing(Connection connection) {
    final String address = connection.getAddress().getAddress().getHostAddress();
    Ping ping = this.pings.get(address);
    if (ping == null) {
      ping = new Ping(1, System.currentTimeMillis());
    } else {
      ping.setCount(ping.getCount() + 1);
      if (System.currentTimeMillis() - ping.getLastPing() < 1000) {
        ping.setSuspiciousCount(ping.getSuspiciousCount() + 1);
      } else {
        ping.setSuspiciousCount(0);
      }
      ping.setLastPing(System.currentTimeMillis());
    }
    this.pings.put(address, ping);
    return ping;
  }

}
