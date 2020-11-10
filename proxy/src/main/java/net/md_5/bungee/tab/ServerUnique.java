package net.md_5.bungee.tab;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.PlayerListItem;

public class ServerUnique extends TabList {

  private final Collection<UUID> uuids;
  private final Collection<String> usernames;

  public ServerUnique(final ProxiedPlayer player) {
    super(player);
    this.uuids = new HashSet<>();
    this.usernames = new HashSet<>();
  }

  @Override
  public void onUpdate(final PlayerListItem playerListItem) {
    for (final PlayerListItem.Item item : playerListItem.getItems()) {
      if (playerListItem.getAction() == PlayerListItem.Action.ADD_PLAYER) {
        if (item.getUuid() != null) {
          this.uuids.add(item.getUuid());
        } else {
          this.usernames.add(item.getUsername());
        }
      } else if (playerListItem.getAction() == PlayerListItem.Action.REMOVE_PLAYER) {
        if (item.getUuid() != null) {
          this.uuids.remove(item.getUuid());
        } else {
          this.usernames.remove(item.getUsername());
        }
      }
    }
    this.player.unsafe().sendPacket(playerListItem);
  }

  @Override
  public void onPingChange(final int ping) {
  }

  @Override
  public void onServerChange() {
    final PlayerListItem packet = new PlayerListItem();
    packet.setAction(PlayerListItem.Action.REMOVE_PLAYER);
    final PlayerListItem.Item[] items = new PlayerListItem.Item[this.uuids.size() + this.usernames
        .size()];
    int i = 0;
    for (final UUID uuid : this.uuids) {
      final int n = i++;
      final PlayerListItem.Item item3 = new PlayerListItem.Item();
      items[n] = item3;
      item3.setUuid(uuid);
    }
    for (final String username : this.usernames) {
      final int n2 = i++;
      final PlayerListItem.Item item4 = new PlayerListItem.Item();
      items[n2] = item4;
      item4.setUsername(username);
      item4.setDisplayName(username);
    }
    packet.setItems(items);
    if (ProtocolConstants.isAfterOrEq(this.player.getPendingConnection().getVersion(), 47)) {
      this.player.unsafe().sendPacket(packet);
    } else {
      for (final PlayerListItem.Item item2 : packet.getItems()) {
        final PlayerListItem p2 = new PlayerListItem();
        p2.setAction(packet.getAction());
        p2.setItems(new PlayerListItem.Item[]{item2});
        this.player.unsafe().sendPacket(p2);
      }
    }
    this.uuids.clear();
    this.usernames.clear();
  }

  @Override
  public void onConnect() {
  }

  @Override
  public void onDisconnect() {
  }

}