package xyz.yooniks.aegis.shell;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.protocol.AddressBlocker;
import xyz.yooniks.aegis.Aegis;
import xyz.yooniks.aegis.blacklist.Blacklist;
import xyz.yooniks.aegis.config.Settings;
import xyz.yooniks.aegis.connection.AegisStatistics;

public class CaptchaFailed {

  private final Cache<String, Integer> connections = CacheBuilder.newBuilder()
      .expireAfterWrite(4, TimeUnit.MINUTES)
      .build();

  public void failed(String address) {
    final AegisStatistics statistics = Aegis.getInstance().getStatistics();

    if (statistics.getPingsPerSecond() > 10
        || statistics.getConnectionsPerSecond() > 2) {
      final BungeeCord bungee = BungeeCord.getInstance();

      final Integer abortedConnections = this.connections.getIfPresent(address);
      if (abortedConnections != null) {
        if (abortedConnections > Settings.IMP.AEGIS_SETTINGS.BLACKLIST.MAX_CAPTCHA_FAILURES) {
          final Blacklist blacklist = bungee.getAegis().getBlacklist();
          if (blacklist != null && Settings.IMP.AEGIS_SETTINGS.BLACKLIST.BLOCK_WHEN_SURE) {

            blacklist.asyncDrop(address);
            //AddressBlocker.block(address);

            if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
              bungee.getLogger().log(Level.INFO, "[Aegis AB] AntiBot blacklist blocked ip "
                  + "{0} completely (captcha failed)", address);
            }
          }
        } else {
          this.connections.put(address, abortedConnections + 1);
        }
      } else {
        this.connections.put(address, 1);
      }
    }
  }

}
