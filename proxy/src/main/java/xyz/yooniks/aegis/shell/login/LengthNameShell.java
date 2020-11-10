package xyz.yooniks.aegis.shell.login;

import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.protocol.DefinedPacket;
import xyz.yooniks.aegis.caching.PacketUtils;
import xyz.yooniks.aegis.connection.AegisStatistics;
import xyz.yooniks.aegis.shell.BotShell;

public class LengthNameShell implements BotShell {

  private final int sensibility;
  private final DefinedPacket kickPacket;
  private final int maxCps;
  private String lastName = "";
  private int count;
  private long lastSimilarNameLoginTime;

  public LengthNameShell(int sensibility, String kickMessage, int maxCps) {
    this.sensibility = sensibility;
    this.kickPacket = PacketUtils.createKickPacket(kickMessage);
    this.maxCps = maxCps;
  }

  @Override
  public String getName() {
    return "nicks-lenght";
  }

  @Override
  public DefinedPacket getKickPacket(PendingConnection connection) {
    return kickPacket;
  }

  @Override
  public boolean pass(PendingConnection connection) {
    final String name = connection.getName();
    if (!lastName.equals(name) && this.lastName.length() == name.length()) {
      if (this.lastSimilarNameLoginTime > System.currentTimeMillis()) {
        return false;
      }
      this.count++;
      if (this.count >= this.sensibility) {
        this.lastSimilarNameLoginTime = System.currentTimeMillis() + 10000L;
        this.count = 0;
        return false;
      }
      return true;
    }
    this.count = 0;
    this.lastName = name;
    return true;
  }

  @Override
  public boolean shouldCheck(AegisStatistics statistics) {
    return statistics.getConnectionsPerSecond() > this.maxCps;
  }

}
