package net.md_5.bungee.command;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import xyz.yooniks.aegis.config.Settings;

public class CommandBungee extends Command {

  private final String message = ChatColor
      .translateAlternateColorCodes('&',
          "&8> &7This server is running &cAegis &7" + Settings.IMP.AEGIS_VERSION + " (1.7.x-1.16.x)"
              + "\n&8> &7The most advanced &cBungeeCord protection&7 against bots & exploits"
              + "\n&8> &7https://minemen.com/resources/216/");

  public CommandBungee() {
    super("bungee");
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    sender.sendMessage(new TextComponent(this.message));
  }

}
