package xyz.yooniks.aegis.auth.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthUserManager {

  private final Map<UUID, AuthUser> userMap = new ConcurrentHashMap<>();

  public AuthUser createUser(UUID uuid, String name) {
    if (this.userMap.containsKey(uuid)) {
      return this.userMap.get(uuid);
    }
    AuthUser user;
    this.userMap.put(uuid, user = new AuthUser(uuid, name));
    return user;
  }

  public AuthUser getByName(String name) {
    return this.getUsers().stream()
        .filter(user -> user.getName().equals(name))
        .findFirst()
        .orElse(null);
  }

  public AuthUser getUser(UUID uuid) {
    return getUsers().stream().filter(user -> user.getOnlineId().equals(uuid))
        .findFirst()
        .orElse(this.userMap.get(uuid));
  }

  public void removeUser(UUID id) {
    this.userMap.remove(id);
  }

  public void removeByName(String name) {
    this.getUsers().stream()
        .filter(user -> user.getName().equals(name))
        .findFirst()
        .ifPresent((user) -> {
          this.userMap.remove(user.getOnlineId());
          this.userMap.remove(user.getId());
        });
  }

  public void putUser(AuthUser user) {
    this.userMap.put(user.getId(), user);
  }

  public List<AuthUser> getUsers() {
    return new ArrayList<>(userMap.values());
  }

}
