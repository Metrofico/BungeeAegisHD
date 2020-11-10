package xyz.yooniks.aegis.shell.login;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.protocol.DefinedPacket;
import xyz.yooniks.aegis.Aegis;
import xyz.yooniks.aegis.caching.PacketUtils;
import xyz.yooniks.aegis.connection.AegisStatistics;
import xyz.yooniks.aegis.shell.BotShell;

public class NeedReconnectShell implements BotShell {

  private final DefinedPacket kickPacket;
  private final Aegis aegis;

  private final Cache<String, Byte> reconnectCache = CacheBuilder.newBuilder()
      .expireAfterWrite(10, TimeUnit.MINUTES)
      .build();

  public NeedReconnectShell(Aegis aegis, String kickMessage) {
    this.aegis = aegis;
    this.kickPacket = PacketUtils.createKickPacket(kickMessage);
  }

  @Override
  public String getName() {
    return "reconnect";
  }

  @Override
  public boolean pass(PendingConnection connection) {
    final InetAddress address = connection.getAddress().getAddress();
    if (!aegis.needCheck(connection.getName(), address)) {
      return true;
    }
    final String hostAddress = address.getHostAddress();
    if (this.reconnectCache.getIfPresent(hostAddress) == null) {
      this.reconnectCache.put(hostAddress, (byte) 0);
      return false;
    }
    return true;
  }

  @Override
  public DefinedPacket getKickPacket(PendingConnection connection) {
    return kickPacket;
  }

  @Override
  public boolean shouldCheck(AegisStatistics statistics) {
    return true;
  }

}
