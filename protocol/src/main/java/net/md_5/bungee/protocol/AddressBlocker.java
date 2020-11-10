package net.md_5.bungee.protocol;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;

public class AddressBlocker {

  /*private static final Cache<String, Byte> blockedTemporary = CacheBuilder.newBuilder()
      .expireAfterWrite(30, TimeUnit.MINUTES)
      .build();

  public static void unblock(String hostname) {
    blockedTemporary.invalidate(hostname);
  }

  public static void block(String hostname) {
    blockedTemporary.put(hostname, (byte) 0);
  }

  public static boolean isBlocked(String address) {
    return blockedTemporary.getIfPresent(address) != null;
  }*/

}
