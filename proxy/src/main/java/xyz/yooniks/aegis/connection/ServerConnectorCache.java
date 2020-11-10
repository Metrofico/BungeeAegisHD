package xyz.yooniks.aegis.connection;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import xyz.yooniks.aegis.config.Settings;

public class ServerConnectorCache {

  private final Cache<String, Long> lastServerJoin = CacheBuilder.newBuilder()
      .expireAfterWrite(2, TimeUnit.MINUTES)
      .build();

  public static ServerConnectorCache getCache() {
    return ServerConnectorCacheHelper.CACHE;
  }

  public boolean canConnect(String address) {
    final Long lastJoin = this.lastServerJoin.getIfPresent(address);
    if (lastJoin == null) {
      return true;
    }
    return lastJoin < System.currentTimeMillis();
  }

  public void join(String address) {
    this.lastServerJoin.put(address,
        System.currentTimeMillis() + Settings.IMP.AEGIS_SETTINGS.LIMIT_SERVERCONNECTOR_TIME);
  }

  public static class ServerConnectorCacheHelper {

    private static final ServerConnectorCache CACHE = new ServerConnectorCache();
  }

}
