package xyz.yooniks.aegis.shell;

import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.protocol.DefinedPacket;
import xyz.yooniks.aegis.connection.AegisStatistics;

public interface BotShell {

  String getName();

  boolean pass(PendingConnection connection);

  DefinedPacket getKickPacket(PendingConnection connection);

  boolean shouldCheck(AegisStatistics statistics);

}
