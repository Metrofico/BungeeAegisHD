package xyz.yooniks.aegis.blacklist;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import net.md_5.bungee.BungeeCord;
import xyz.yooniks.aegis.config.Settings;
import xyz.yooniks.aegis.vpn.StringReplacer;

public class Blacklist {

  private final Executor executor = Executors
      .newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

  private final BlacklistManager blacklistManager;
  private final int blacklistMode;

  public Blacklist(BlacklistManager blacklistManager, int blacklistMode) {
    this.blacklistManager = blacklistManager;
    this.blacklistMode = blacklistMode;
  }

  public void asyncDrop(String address) {
    this.blacklistManager.addBlacklist(address);

    if (blacklistMode == 0) {
      this.asyncDropWithoutBlacklistManager(address);
    }
  }

  public void asyncDropWithoutBlacklistManager(String address) {
    if (blacklistMode == 0) {
      executor.execute(() -> {
        try {
          Runtime.getRuntime().exec(
              StringReplacer.replace(Settings.IMP.AEGIS_SETTINGS.BLACKLIST.COMMANDS.BLOCK_COMMAND,
                  "{ADDRESS}", address));

          if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
            BungeeCord.getInstance().getLogger().log(Level.INFO, "[Aegis Blacklist] "
                + "Blocked ip {0} completely!", address);
          }
        } catch (IOException ex) {
          BungeeCord.getInstance().getLogger().log(Level.WARNING, "[Aegis BlackList] "
              + "Could not block address {0}, error: {1}", new String[]{address, ex.getMessage()});
        }
      });
    }
  }

  public void asyncCommand(List<String> commands) {
    executor.execute(() -> {
      for (String command : commands) {
        try {
          Runtime.getRuntime().exec(command);
        } catch (IOException ex) {
          BungeeCord.getInstance().getLogger().log(Level.WARNING, "[Aegis Blacklist] "
                  + "Could not execute command {0}, error: {1}",
              new String[]{command, ex.getMessage()});
        }
      }
    });
  }

}
