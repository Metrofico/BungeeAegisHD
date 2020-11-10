package xyz.yooniks.aegis.utils;

import com.google.common.cache.Cache;
import java.net.InetAddress;
import xyz.yooniks.aegis.Aegis;

/**
 * @author Leymooo
 */
public class ServerPingUtils {

  private final Aegis aegis;
  private Cache<InetAddress, Boolean> pingList;
  private boolean enabled = false;//Settings.IMP.SERVER_PING_CHECK.MODE != 2;

  public ServerPingUtils(Aegis aegis) {
    this.aegis = aegis;
    pingList = /*CacheBuilder.newBuilder()
                .concurrencyLevel( Runtime.getRuntime().availableProcessors() )
                .initialCapacity( 100 )
                .expireAfterWrite( Settings.IMP.SERVER_PING_CHECK.CACHE_TIME, TimeUnit.SECONDS )
                .build();*/null;
  }

  public boolean needKickOrRemove(InetAddress address) {
    boolean present = pingList.getIfPresent(address) == null;
    if (!present) //Убрираем из мапы если есть уже есть в ней.
    {
      pingList.invalidate(address);
    }
    return present;
  }

  public void add(InetAddress address) {
    if (enabled) {
      pingList.put(address, true);
    }
  }

  public boolean needCheck() {
    return false;//enabled && ( Settings.IMP.SERVER_PING_CHECK.MODE == 0 || aegis.isUnderAttack() );
  }

  public void clear() {
    pingList.invalidateAll();
  }

  public void cleanUP() {
    pingList.cleanUp();
  }
}
