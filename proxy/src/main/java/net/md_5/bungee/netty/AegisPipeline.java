package net.md_5.bungee.netty;

import io.netty.channel.Channel;
import net.md_5.bungee.BungeeCord;
import xyz.yooniks.aegis.Aegis;
import xyz.yooniks.aegis.config.Settings;
import xyz.yooniks.aegis.config.Settings.AEGIS_SETTINGS;
import xyz.yooniks.aegis.config.Settings.AEGIS_SETTINGS.ADVANCED_CHECKS;
import xyz.yooniks.aegis.connection.AegisStatistics;

public final class AegisPipeline {

  public static boolean preConnectCheck(String hostname, Aegis aegis, AegisStatistics statistics, BungeeCord bungeeCord, Channel ch) {
    final Settings settings = Settings.IMP;
    final AEGIS_SETTINGS aegisSettings = settings.AEGIS_SETTINGS;

    if (!aegisSettings.BYPASS_IPS.contains(hostname)) {
      if (aegis.getBlacklistManager().isBlacklisted(hostname)) {
        return true;
      }
    }

    final ADVANCED_CHECKS advancedChecks = aegisSettings.ADVANCED_CHECKS;
    final int blockNewConnectionsWhenCpsIsHigherThan = advancedChecks.BLOCK_NEW_CONNECTIONS_WHEN_CPS_IS_HIGHER_THAN;
    if (blockNewConnectionsWhenCpsIsHigherThan != -1) {
      if (statistics.getTotalConnectionsPerSecond()
          > blockNewConnectionsWhenCpsIsHigherThan && aegis.needCheck(hostname)) {
        ch.close();
        statistics.addBlockedConnection();
        if (!aegisSettings.CLEAN_CONSOLE) {
          bungeeCord.getLogger()
              .info("[Aegis CPS limiter] Blocked untrusted connection " + hostname);
        }

        if (advancedChecks.LIMIT_CONNECTIONS_PER_IP_WHEN_ATTACK) {
          if (aegis.getAdvancedConnectionLimitter().increase(hostname)
              > advancedChecks.LIMIT_CONNECTIONS_PER_IP_WHEN_ATTACK_LIMIT) {
            if (!aegisSettings.CLEAN_CONSOLE) {
              bungeeCord.getLogger()
                  .info("[Aegis CPS limiter] Blacklisted untrusted connection " + hostname
                      + " for 30min, limit of cps exceed");
            }
            aegis.getBlacklistManager().addBlacklist(hostname);
            //AddressBlocker.block(hostname);
          }
        }
        return true;
      }
    }

    if (aegis.getBlacklistManager().isBlacklisted(hostname) && !aegis.getWhitelistManager()
        .isWhitelisted(hostname)) {
      ch.close();

      statistics.addBlockedConnection();
      if (!aegisSettings.CLEAN_CONSOLE) {
        bungeeCord.getLogger()
            .info("[Aegis blacklist file] Blocked connection " + hostname);
      }
      return true;
    }
    return false;
  }

  private AegisPipeline() {
  }

}
