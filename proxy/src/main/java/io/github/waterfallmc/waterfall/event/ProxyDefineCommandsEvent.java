package io.github.waterfallmc.waterfall.event;

import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.plugin.Command;
import java.util.Map;
import net.md_5.bungee.api.event.TargetedEvent;

public class ProxyDefineCommandsEvent extends TargetedEvent {

  private final Map<String, Command> commands;

  public ProxyDefineCommandsEvent(final Connection sender, final Connection receiver,
      final Map<String, Command> commands) {
    super(sender, receiver);
    this.commands = commands;
  }

  public Map<String, Command> getCommands() {
    return this.commands;
  }

}
