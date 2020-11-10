package xyz.yooniks.aegis.shell.login;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.protocol.DefinedPacket;
import xyz.yooniks.aegis.caching.PacketUtils;
import xyz.yooniks.aegis.connection.AegisStatistics;
import xyz.yooniks.aegis.shell.BotShell;

public class MaxAccountsShell implements BotShell {

  private final DefinedPacket kickPacket;
  private final int maxAccounts;

  public MaxAccountsShell(String kickMessage, int maxAccounts) {
    this.kickPacket = PacketUtils.createKickPacket(kickMessage);
    this.maxAccounts = maxAccounts;
  }

  @Override
  public String getName() {
    return "max-accounts-per-ip";
  }

  @Override
  public DefinedPacket getKickPacket(PendingConnection connection) {
    return kickPacket;
  }

  @Override
  public boolean pass(PendingConnection connection) {
    final String hostAddress = connection.getAddress().getAddress().getHostAddress();

    return BungeeCord.getInstance()
        .getPlayers()
        .stream()
        .filter(player -> player.getAddress().getAddress().getHostAddress()
            .equalsIgnoreCase(hostAddress))
        .count() < this.maxAccounts;
  }

  @Override
  public boolean shouldCheck(AegisStatistics statistics) {
    return true;
  }

}
