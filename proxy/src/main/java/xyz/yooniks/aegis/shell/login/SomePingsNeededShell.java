package xyz.yooniks.aegis.shell.login;

import java.util.HashMap;
import java.util.Map;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.packet.Kick;
import xyz.yooniks.aegis.Aegis;
import xyz.yooniks.aegis.config.Settings;
import xyz.yooniks.aegis.connection.AegisStatistics;
import xyz.yooniks.aegis.shell.BotShell;
import xyz.yooniks.aegis.shell.PingableBotCheck;
import xyz.yooniks.aegis.utils.MessageBuilder;

public class SomePingsNeededShell implements BotShell, PingableBotCheck {

  private final Map<String, Integer> pingCount = new HashMap<>();

  private final int neededPings, maxCps;
  private final String kickMessage;

  private final DefinedPacket kick1, kick2, kick3;

  public SomePingsNeededShell(int neededPings, int maxCps, String kickMessage) {
    this.neededPings = neededPings;
    this.maxCps = maxCps;
    this.kickMessage = kickMessage;
    kick1= kick(1);
    kick2 = kick(2);
    kick3 = kick(3);
  }

  @Override
  public String getName() {
    return "some-pings-needed";
  }

  @Override
  public boolean pass(PendingConnection connection) {
    return this.pingCount.getOrDefault(connection.getAddress().getAddress().getHostAddress(), 0) >= this.neededPings;
  }

  @Override
  public void pinged(String name, String address) {
    this.pingCount.put(address, this.pingCount.getOrDefault(address, 0) + 1);
  }

  @Override
  public DefinedPacket getKickPacket(PendingConnection connection) {
    final int current = this.pingCount.getOrDefault(connection.getAddress().getAddress().getHostAddress(), 1);
    switch (current) {
      case 1: {
        return kick1;
      }
      case 2: {
        return kick2;
      }
      case 3: {
        return kick3;
      }
      default:
        return kick(current);
    }
  }

  private DefinedPacket kick(int current) {
    return new Kick(
        ComponentSerializer.toString(new TextComponent(
            MessageBuilder.newBuilder()
                .withMessage(this.kickMessage)
                .withField("{CURRENT}", current)
                .withField("{NEEDED}", this.neededPings)
                .coloured().stripped()
                .build()))
    );
  }

  @Override
  public boolean shouldCheck(AegisStatistics statistics) {
    return statistics.getConnectionsPerSecond() >= this.maxCps;
  }

}
