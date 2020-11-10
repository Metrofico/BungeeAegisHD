package xyz.yooniks.aegis.auth.command;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import xyz.yooniks.aegis.auth.AuthSystem;
import xyz.yooniks.aegis.auth.user.AuthUser;
import xyz.yooniks.aegis.config.Settings;

public class LoginSessionCommand extends Command {

  private final AuthSystem authSystem;

  public LoginSessionCommand(AuthSystem authSystem) {
    super("remember", null, "loginsession");
    this.authSystem = authSystem;
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    if (!(sender instanceof ProxiedPlayer)) {
      sender.sendMessage(new TextComponent("Only player!"));
      return;
    }
    if (!sender.hasPermission("aegisauth.changepassword")) {
      sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
          "&cAegis &8> &7No permission! &8(&7aegisauth.changepassword&8))")));
      return;
    }
    final AuthUser user = this.authSystem.getUserManager()
        .getUser(((ProxiedPlayer) sender).getUniqueId());

    if (user.isPremium()) {
      sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
          Settings.IMP.AUTH.MESSAGES.CHANGEPASSWORD_ERROR_PREMIUM)));
      return;
    }
    if (!user.isLogged()) {
      sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
          Settings.IMP.AUTH.MESSAGES.CHANGEPASSWORD_NOT_LOGGED)));
      return;
    }

    if (args.length < 2) {
      sender.sendMessage(new TextComponent(ChatColor
          .translateAlternateColorCodes('&', Settings.IMP.AUTH.MESSAGES.CHANGEPASSWORD_USAGE)));
      return;
    }
    if (!this.authSystem.getEncryption().match(args[0], user.getPassword())) {
      sender.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&',
          Settings.IMP.AUTH.MESSAGES.CHANGEPASSWORD_WRONG_PASS)));
      return;
    }
    if (args[0].equals(args[1])) {
      sender.sendMessage(new TextComponent(ChatColor
          .translateAlternateColorCodes('&', Settings.IMP.AUTH.MESSAGES.CHANGEPASSWORD_SAME_PASS)));
      return;
    }
    user.setPassword(this.authSystem.getEncryption().hash(args[1]));
    sender.sendMessage(new TextComponent(ChatColor
        .translateAlternateColorCodes('&', Settings.IMP.AUTH.MESSAGES.CHANGEPASSWORD_SUCCESS)));
  }

}
