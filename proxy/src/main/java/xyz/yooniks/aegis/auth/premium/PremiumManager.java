package xyz.yooniks.aegis.auth.premium;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PremiumManager {

  private static final Map<String, PremiumUser> cachedUsers = new ConcurrentHashMap<>();

  public static void putUser(String nickName, PremiumUser user) {
    cachedUsers.put(nickName.toLowerCase(), user);
  }

  public static void removeUser(String nickName) {
    cachedUsers.remove(nickName);
  }

  public static PremiumUser getByName(String name) {
    final String nick = name.toLowerCase();
    return cachedUsers.getOrDefault(nick, null);
  }

  public static class PremiumUser {

    private boolean premium;
    private UUID uuid;

    public PremiumUser(boolean premium, UUID uuid) {
      this.premium = premium;
      this.uuid = uuid;
    }

    public boolean isPremium() {
      return premium;
    }

    public void setPremium(boolean premium) {
      this.premium = premium;
    }

    public UUID getUuid() {
      return uuid;
    }

    public void setUuid(UUID uuid) {
      this.uuid = uuid;
    }
  }

}
