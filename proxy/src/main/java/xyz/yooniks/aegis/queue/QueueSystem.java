package xyz.yooniks.aegis.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import xyz.yooniks.aegis.config.Settings;
import xyz.yooniks.aegis.vpn.StringReplacer;

public class QueueSystem implements Runnable {

  private final java.util.Queue<Queue> queues = new ConcurrentLinkedQueue<>();

  public int getQueueSize() {
    return this.queues.size() + 1;
  }

  @Override
  public void run() {
    final Queue poll = this.queues.poll();
    if (poll == null) {
      return;
    }

    int place = 0;
    final int places = this.queues.size() + 1;

    final List<String> priority = new ArrayList<>();

    final List<Queue> newQueue = new ArrayList<>(queues);
    for (Queue queue : newQueue) {
      if (queue.getPlayer() != null) {
        if (queue.getPlayer().hasPermission(Settings.IMP.QUEUE.BYPASS_PERMISSION)) {
          priority.add(queue.getPlayer().getName());
        }
      }
    }

    newQueue.sort((o1, o2) -> {
      if (priority.contains(o1.getPlayer().getName())) {
        if (priority.contains(o2.getPlayer().getName())) {
          return Integer.compare(10, 10);
        }
        return Integer.compare(10, 0);
      }
      return 0;
    });

    for (Queue queue : newQueue) {

      final ProxiedPlayer player = queue.getPlayer();
      if (player == null || !player.isConnected()) {
        this.queues.remove(queue);
        continue;
      }

      queue.setPlace(++place);
      String message = Settings.IMP.QUEUE.ACTIONBAR_MESSAGE;
      message = StringReplacer.replace(message, "{PLACE}", String.valueOf(place));
      message = StringReplacer
          .replace(message, "{TARGET}", String.valueOf(queue.getTargetServer()));

      player.sendMessage(ChatMessageType.ACTION_BAR,
          new TextComponent(ChatColor.translateAlternateColorCodes('&', message)));
    }

    final ProxiedPlayer player = poll.getPlayer();
    if (player != null) {
      String message = Settings.IMP.QUEUE.ACTIONBAR_MESSAGE;
      message = StringReplacer.replace(message, "{PLACE}", String.valueOf(poll.getPlace()));
      message = StringReplacer.replace(message, "{TARGET}", String.valueOf(poll.getTargetServer()));

      player.sendMessage(ChatMessageType.ACTION_BAR,
          new TextComponent(ChatColor.translateAlternateColorCodes('&', message)));
      poll.getConnection()
          .connectNoQueue(poll.getTargetServer(), null, true, ServerConnectEvent.Reason.JOIN_PROXY);
    }

  }

  public void addQueue(Queue queue) {
    this.queues.offer(queue);
  }

}
