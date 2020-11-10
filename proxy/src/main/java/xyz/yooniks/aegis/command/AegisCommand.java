package xyz.yooniks.aegis.command;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.protocol.AddressBlocker;
import xyz.yooniks.aegis.Aegis;
import xyz.yooniks.aegis.auth.AuthSystem;
import xyz.yooniks.aegis.auth.premium.PremiumManager;
import xyz.yooniks.aegis.auth.premium.PremiumManager.PremiumUser;
import xyz.yooniks.aegis.auth.user.AuthUser;
import xyz.yooniks.aegis.auth.user.AuthUser.PremiumAnswer;
import xyz.yooniks.aegis.auth.user.AuthUserManager;
import xyz.yooniks.aegis.blackhole.Blackhole;
import xyz.yooniks.aegis.config.Settings;
import xyz.yooniks.aegis.connection.AegisStatistics;

public class AegisCommand extends Command {

  private final List<String> notifications = new ArrayList<>();

  public AegisCommand() {
    super("aegis", null, "bf", "antibot", "bot", "antybot", "botfilter", "flamecord", "waterfall",
        "travertine");

    new Timer().scheduleAtFixedRate(new TimerTask() {
      boolean firstFancyChar = true;

      @Override
      public void run() {
        try {
            if (BungeeCord.getInstance() == null) {
                return;
            }

          final List<ProxiedPlayer> players = notifications.stream()
              .map(BungeeCord.getInstance()::getPlayer)
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
          final String fancyChar;
          if (firstFancyChar) {
            fancyChar = "↰";
            firstFancyChar = false;
          } else {
            fancyChar = "↱";
            firstFancyChar = true;
          }

          final Aegis instance = Aegis.getInstance();
          final AegisStatistics statistics = instance.getStatistics();

          String message = Settings.IMP.MESSAGES.STATISTICS
              .replace("{TOTAL-BLOCKED}",
                  String.valueOf(statistics.getBlockedConnections()))
              .replace("{CPS}", String.valueOf(statistics.getConnectionsPerSecond()))
              .replace("{PPS}", String.valueOf(statistics.getPingsPerSecond()))
              .replace("{TOTAL-CPS}",
                  String.valueOf(statistics.getTotalConnectionsPerSecond()))
              .replace("{CHECKING}", String.valueOf(instance.getOnlineOnFilter()))

              .replace("{FANCY-CHAR}", fancyChar);

          for (ProxiedPlayer player : players) {
            player.sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(
                    ChatColor.translateAlternateColorCodes('&', message)));
          }
        } catch (Exception ignored) {
        }
      }
    }, 120, 120);
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    if (args.length == 0) {
      if (!sender.hasPermission("aegis.command")) {
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
            "&7This server is protected with &cAegis " + Settings.IMP.AEGIS_VERSION + "&7, "
                + "read more about aegis here: &chttps://minemen.com/resources/216/")));

        sender.sendMessage(new TextComponent(ChatColor
            .translateAlternateColorCodes('&',
                "&7You have no permission to see usage of aegis commands &8(&caegis.command&8)")));
        return;
      }
      sender.sendMessage(
          new TextComponent(ChatColor.translateAlternateColorCodes('&', getDescription())));
    } else if (args[0].equalsIgnoreCase("reload")) {
      if (!sender.hasPermission("aegis.reload")) {
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "&cAegis &8> "
            + "&7No permission! &8(&7aegis.reload&8)")));
        return;
      }
      BungeeCord.getInstance().getAegis().disable();
      BungeeCord.getInstance().setAegis(new Aegis(false));
      sender.sendMessage(new TextComponent(
          ChatColor.translateAlternateColorCodes('&', "&cAegis &8> &7Reloaded successfully!")));
    } else if (args[0].equals("blacklist")) {
      if (!sender.hasPermission("aegis.blacklist")) {
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "&cAegis &8> "
            + "&7No permission! &8(&7aegis.blacklist&8)")));
        return;
      }
      if (args.length < 2) {
        sender.sendMessage(new TextComponent(ChatColor.RED
            + "Invalid amount of arguments! Please provide second argument which is ip address. /aegis blacklist some-ip"));
        return;
      }
      final String address = args[1];
      //AddressBlocker.block(address);
      Aegis.getInstance().getBlacklistManager().addBlacklist(address);
      sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
          "&cAegis &8> &7You have &cblocked&7 ip " + address)));
    } else if (args[0].equals("unblacklist")) {
      if (!sender.hasPermission("aegis.blacklist")) {
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "&cAegis &8> "
            + "&7No permission! &8(&7aegis.blacklist&8)")));
        return;
      }
      if (args.length < 2) {
        sender.sendMessage(new TextComponent(ChatColor.RED
            + "Invalid amount of arguments! Please provide second argument which is ip address. /aegis unblacklist some-ip"));
        return;
      }
      final String address = args[1];
      //AddressBlocker.unblock(address);
      Aegis.getInstance().getBlacklistManager().removeBlacklist(address);
      sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
          "&cAegis &8> &7You have &aunblocked&7 ip " + address)));
    } else if (args[0].equals("whitelist")) {
      if (!sender.hasPermission("aegis.whitelist")) {
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "&cAegis &8> "
            + "&7No permission! &8(&7aegis.whitelist&8)")));
        return;
      }
      if (args.length < 2) {
        sender.sendMessage(new TextComponent(ChatColor.RED + "Invalid amount of arguments!"
            + " Please provide second argument which is ip address. /aegis whitelist [ipAddress/orName]"));
        return;
      }
      final String address = args[1];
      Settings.IMP.AEGIS_SETTINGS.BYPASS_IPS.add(address);
      Aegis.getInstance().getWhitelistManager().addWhitelist(address);
      sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
          "&cAegis &8> &7You have &6whitelisted&7 ip " + address)));
    } else if (args[0].equals("unwhitelist")) {
      if (!sender.hasPermission("aegis.whitelist")) {
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "&cAegis &8> "
            + "&7No permission! &8(&7aegis.whitelist&8)")));
        return;
      }
      if (args.length < 2) {
        sender.sendMessage(new TextComponent(ChatColor.RED + "Invalid amount of arguments!"
            + " Please provide second argument which is ip address. /aegis unwhitelist some-ip"));
        return;
      }
      final String address = args[1];
      sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
          "&cAegis &8> &7You have &cremoved&7 ip " + address + " from whitelist")));
      Settings.IMP.AEGIS_SETTINGS.BYPASS_IPS.remove(address);
    }

    else if (args[0].equals("blackhole")) {
      if (!sender.hasPermission("aegis.blackhole")) {
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "&cAegis &8> "
            + "&7No permission! &8(&7aegis.blackhole&8)")));
        return;
      }
      if (Blackhole.INSTANCE.isEnabled()) {
        Blackhole.INSTANCE.setEnabled(false);
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
            "&cAegis &8> &7You have &cdisabled&7 &6BLACK-HOLE &7mode, every NEW player will be blacklisted, it is very good for blacklisting bots proxies/ips, please use it only temorary, like for a minute max!!")));
      }
      else {
        Blackhole.INSTANCE.setEnabled(true);
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
            "&cAegis &8> &7You have &aenabled&7 &6BLACK-HOLE &7mode, every NEW player will be blacklisted, it is very good for blacklisting bots proxies/ips, please use it only temorary, like for a minute max!!")));
      }
    }
    else if (args[0].equals("blackholeinfo")) {
      if (!sender.hasPermission("aegis.blackhole")) {
        sender.sendMessage(
            new TextComponent(ChatColor.translateAlternateColorCodes('&', "&cAegis &8> "
                + "&7No permission! &8(&7aegis.blackhole&8)")));
        return;
      }
      sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
          "&cAegis &8> &7What is &6BLACK-HOLE &7mode?")));
      sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
          "&cIt blacklists EVERY new player that joins the server."
              + "\n&cIt can be enabled even just for a few seconds to reduce powerful bot attacks\n&cIt lets aegis block bots with ipset & iptables"
              + "\n&cIt works only if blacklist-mode is 0!!!\n&cWould it work now? " + (Settings.IMP.AEGIS_SETTINGS.BLACKLIST.BLACKLIST_MODE == 0 ? "&ayes" : "&6no!! Please set blacklist-mode to 0 and execute linux/ubuntu/debian commands from aegis.zip/very important/iptables rules.txt in server root"))));
    }
    else if (args[0].equalsIgnoreCase("auth")) {
      if (!sender.hasPermission("aegis.auth")) {
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "&cAegis &8> "
            + "&7No permission! &8(&7aegis.auth&8)")));
        return;
      }
      final AuthSystem authSystem = Aegis.getInstance().getAuthSystem();
      if (authSystem == null) {
        sender.sendMessage(
            new TextComponent(ChatColor.RED + "Auth-System is disabled on this server!"));
        return;
      }
      if (args.length < 3) {
        sender.sendMessage(new TextComponent(
            ChatColor.RED + "Aegis: Correct usage: /aegis auth [premium/nonpremium] <playerName>"));
        return;
      }

      final AuthUserManager userManager = authSystem.getUserManager();
      if (args[1].equalsIgnoreCase("premium")) {
        final String playerName = args[2];
        final AuthUser user = userManager.getByName(playerName);
        if (user == null) {
          sender.sendMessage(
              new TextComponent(ChatColor.RED + "Aegis: This player doesn't exist in our system!"));
          return;
        }
        user.setPremiumAnswer(PremiumAnswer.YES);
        user.setPremium(true);
        if (user.getOnlineId() != null) {
          PremiumManager.putUser(playerName, new PremiumUser(true, user.getOnlineId()));
        }
        sender.sendMessage(new TextComponent(
            ChatColor.RED + "Aegis: You have forcily added player " + playerName
                + " as premium user!"));
      } else if (args[1].equalsIgnoreCase("nonpremium")
          || args[1].equalsIgnoreCase("cracked") || args[1].equalsIgnoreCase("unpremium")) {
        final String playerName = args[2];
        final AuthUser user = userManager.getByName(playerName);
        if (user == null) {
          sender.sendMessage(
              new TextComponent(ChatColor.RED + "Aegis: This player doesn't exist in our system!"));
          return;
        }
        user.setPremiumAnswer(PremiumAnswer.NO);
        user.setPremium(false);
        user.setCheckedIfPremium(true);

        //if (PremiumManager.getByName(playerName) != null) {
        PremiumManager.putUser(playerName, new PremiumUser(false, user.getId()));
        // }
        sender.sendMessage(new TextComponent(
            ChatColor.RED + "Aegis: You have forcily added player " + playerName
                + " as non-premium user!"));
      }
      else if (args[1].equalsIgnoreCase("unregister")) {
        if (!sender.hasPermission("aegis.unregister")) {
          sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "&cAegis &8> "
              + "&7No permission! &8(&7aegis.unregister&8)")));
          return;
        }
        final String playerName = args[2];
        final AuthUser user = userManager.getByName(playerName);
        if (user == null) {
          try {
            Aegis.getInstance().getAuthSystem().getDatabase().removeObjectByName(playerName);
          } catch (SQLException throwables) {
            sender.sendMessage(
                new TextComponent(ChatColor.RED + "Aegis: Could not unregister this player! " + throwables.getMessage()));
            throwables.printStackTrace();
          }
          sender.sendMessage(
              new TextComponent(ChatColor.RED + "Aegis: Unregistered player " + playerName + " with mysql only (because he wasn't on server)!"));
          //sender.sendMessage(
           //   new TextComponent(ChatColor.RED + "Aegis: This player doesn't exist in our system!"));
          return;
        }
        user.setRegistered(false);
       // userManager.removeUser(user.getId());
        userManager.removeByName(playerName);
        sender.sendMessage(
            new TextComponent(ChatColor.RED + "Aegis: Unregistered player " + playerName + "!"));
      }
      else {
        sender.sendMessage(new TextComponent(
            ChatColor.RED + "Aegis: Correct usage: /aegis auth [premium/nonpremium/unregister] <playerName>"));
      }
    } else if (args[0].equals("stat") || args[0].equals("stats") || args[0].equals("notify")
        || args[0].equals("notifications")) {

        if (!(sender instanceof ProxiedPlayer)) {
            return;
        }

      final ProxiedPlayer player = (ProxiedPlayer) sender;

      if (!sender.hasPermission("aegis.stats")) {
        sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', "&cAegis &8> "
            + "&7No permission! &8(&7aegis.stats&8)")));
        return;
      }

      player.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
          "&cAegis > &7You have " + (this.notifications.contains(player.getName()) ? "&cdisabled"
              : "&aenabled") + "&7 stats!")));

        if (this.notifications.contains(player.getName())) {
            notifications.remove(player.getName());
        } else {
            notifications.add(player.getName());
        }
    }
  }

  private String getDescription() {
    final Aegis aegis = Aegis.getInstance();
    return "\n&8&m=-=-=-=-=-=&r &aAegis &8&m=-=-=-=-=-=\n"
        + "&cTotal &7blocked connections: &7" + aegis.getStatistics().getBlockedConnections() + "\n"
        + "&cAttack detected: " + (aegis.isUnderAttack() ? "&cyes" : "&ano") + "\n"
        + "&cChecking players(s)&7: &7" + aegis.getOnlineOnFilter() + "\n"
        + "&cNon-Bot players(s)&7: &7" + aegis.getUsersCount() + "\n\n"
        + "&c/aegis stats&7 - show statistics on actionbar\n"
        + "&c/aegis reload&7 - reloads whole configuration\n"
        + "&c/aegis blackhole [ip]&7 - enables/disables blackhole mode\n"
        + "&c/aegis blackholeinfo [ip]&7 - info about blackhole mode\n"
        + "&c/aegis whitelist [ip]&7 - whitelists ip from bot & crash checks\n"
        + "&c/aegis unwhitelist [ip]&7 - unwhitelists ip\n"
        + "&c/aegis blacklist [ip]&7 - blacklists ip (0 packets from this ip)\n"
        + "&c/aegis unblacklist [ip]&7 - unblacklists ip\n"
        + (aegis.getAuthSystem() == null ? "" : ""
        + "\n&c/aegis auth premium [nick]&7 - sets player account to premium\n"
        + "&c/aegis auth unpremium [nick] &7- sets player account to nonpremium\n"
        + "&c/aegis auth unregister [nick] &7- unregisters player\n")
        + "&7Developer's discord: &cyooniks#0289\n"
        + "&7You are running &c" + Settings.IMP.AEGIS_VERSION + " (1.7.x-1.16.3) version\n"
        + "&8&m=-=-=-=-=-=&r &aAegis &8&m=-=-=-=-=-=\n";
  }

}
