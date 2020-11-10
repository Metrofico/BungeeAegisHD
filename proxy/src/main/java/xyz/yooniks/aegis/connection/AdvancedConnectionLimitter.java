package xyz.yooniks.aegis.connection;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;

public class AdvancedConnectionLimitter {

  private final Cache<String, Integer> cps = CacheBuilder.newBuilder()
      .expireAfterAccess(5, TimeUnit.SECONDS)
      .build();

  public int increase(String address) {
    Integer current = this.cps.getIfPresent(address);
    if (current != null) {
      this.cps.put(address, ++current);
    } else {
      this.cps.put(address, current = 1);
    }
    return current;
  }

}
