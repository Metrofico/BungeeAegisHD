package xyz.yooniks.aegis.shell.login;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.protocol.AddressBlocker;
import net.md_5.bungee.protocol.DefinedPacket;
import xyz.yooniks.aegis.blacklist.Blacklist;
import xyz.yooniks.aegis.caching.PacketUtils;
import xyz.yooniks.aegis.config.Settings;
import xyz.yooniks.aegis.connection.AegisStatistics;
import xyz.yooniks.aegis.shell.BotShell;
import xyz.yooniks.aegis.shell.BotShellRunnable;

public class NameMatchShell implements BotShell, BotShellRunnable {

  private final String pattern;
  private final DefinedPacket kickPacket;
  private final int maxCps;
  private final Cache<String, Integer> abortedConnections = CacheBuilder.newBuilder()
      .expireAfterWrite(3, TimeUnit.MINUTES)
      .build();

  public NameMatchShell(String pattern, String kickMessage, int maxCps) {
    this.pattern = pattern;
    this.kickPacket = PacketUtils.createKickPacket(kickMessage);
    this.maxCps = maxCps;
  }

  @Override
  public void advancedAction(String address, AegisStatistics statistics) {
    if (statistics.getPingsPerSecond() > 50 || statistics.getConnectionsPerSecond() > 5) {
      final Integer abortedConnections = this.abortedConnections.getIfPresent(address);
      if (abortedConnections != null) {
        if (abortedConnections > Settings.IMP.AEGIS_SETTINGS.BLACKLIST.MAX_NAME_MATCH_FAILURES) {

          final BungeeCord bungee = BungeeCord.getInstance();

          final Blacklist blacklist = bungee.getAegis().getBlacklist();
          if (blacklist != null && Settings.IMP.AEGIS_SETTINGS.BLACKLIST.BLOCK_WHEN_SURE) {
            blacklist.asyncDrop(address);

            //AddressBlocker.block(address);

            if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
              bungee.getLogger().log(Level.INFO, "[Aegis AB] AntiBot blacklist blocked ip "
                  + "{0} completely", address);
            }
          }
        } else {
          this.abortedConnections.put(address, abortedConnections + 1);
        }
      } else {
        this.abortedConnections.put(address, 1);
      }
    }
  }

  @Override
  public String getName() {
    return "nicks-match";
  }

  @Override
  public DefinedPacket getKickPacket(PendingConnection connection) {
    return kickPacket;
  }

  @Override
  public boolean pass(PendingConnection connection) {
    return !connection.getName().matches(this.pattern);
  }

  @Override
  public boolean shouldCheck(AegisStatistics statistics) {
    return statistics.getConnectionsPerSecond() > this.maxCps;
  }

}
