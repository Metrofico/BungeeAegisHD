package xyz.yooniks.aegis.shell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.config.Configuration;
import xyz.yooniks.aegis.Aegis;
import xyz.yooniks.aegis.shell.login.LengthNameShell;
import xyz.yooniks.aegis.shell.login.MaxAccountsShell;
import xyz.yooniks.aegis.shell.login.NameMatchShell;
import xyz.yooniks.aegis.shell.login.NeedReconnectShell;
import xyz.yooniks.aegis.shell.login.PingNeededShell;
import xyz.yooniks.aegis.shell.login.SomePingsNeededShell;
import xyz.yooniks.aegis.shell.login.ThrottleShell;

public class BotShellManager {

  private final List<BotShell> shells = new ArrayList<>();

  public void addShells(Collection<BotShell> shells) {
    this.shells.addAll(shells);
  }

  public List<BotShell> getShells() {
    return shells;
  }

  public void clean() {
    this.shells.clear();
  }

  public static class Loader {

    public static List<BotShell> findShells(Aegis aegis, Configuration configuration) {
      final List<BotShell> shells = new ArrayList<>();
      for (String id : configuration.getSection("shells").getKeys()) {
        id = id.toLowerCase();
        final Configuration data = configuration.getSection("shells." + id);
        if (!data.getBoolean("enabled", true)) {
          continue;
        }
        final String kickMessage = data.getString("kick-message");
        if (id.equals("nicks-length")) {
          shells.add(
              new LengthNameShell(data.getInt("sensibility", 3), kickMessage,
                  data.getInt("max-cps", 3)));
        } else if (id.equals("ping-needed")) {
          BungeeCord.getInstance().getLogger().log(Level.WARNING, "You use old aegis_antibot.yml config with \"ping-needed\"! It was replaced with \"some-pings-needed\" in 8.2.0 Aegis version! Please reset your aegis_antibot.yml config to let it be generated again. (just remove your current aegis_antibot.yml and restart config)");
          //shells.add(new PingNeededShell(aegis.getPingManager(), kickMessage));
        }
        else if (id.equals("some-pings-needed")) {
          shells.add(
              new SomePingsNeededShell(data.getInt("needed-pings"), data.getInt("max-cps", 3), kickMessage));
        }
        else if (id.equals("nicks-match")) {
          shells.add(
              new NameMatchShell(data.getString("pattern"), kickMessage,
                  data.getInt("max-cps", 3)));
        } else if (id.equals("connection-throttle")) {
          shells.add(
              new ThrottleShell(aegis.getConnectionManager(), kickMessage,
                  data.getInt("delay", 10)));
        } else if (id.equalsIgnoreCase("reconnect")) {
          shells.add(new NeedReconnectShell(aegis, kickMessage));
        } else if (id.equalsIgnoreCase("max-accounts-per-ip")) {
          shells.add(new MaxAccountsShell(kickMessage, data.getInt("max-accounts")));
        }
      }
      return shells;
    }
  }

}
