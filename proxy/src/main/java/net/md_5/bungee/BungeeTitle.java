package net.md_5.bungee;

import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;

public class BungeeTitle implements Title {

  private net.md_5.bungee.protocol.packet.Title title;
  private net.md_5.bungee.protocol.packet.Title subtitle;
  private net.md_5.bungee.protocol.packet.Title times;
  private net.md_5.bungee.protocol.packet.Title clear;
  private net.md_5.bungee.protocol.packet.Title reset;

  public BungeeTitle() {
    super();
  }

  private static net.md_5.bungee.protocol.packet.Title createPacket(
      final net.md_5.bungee.protocol.packet.Title.Action action) {
    final net.md_5.bungee.protocol.packet.Title title = new net.md_5.bungee.protocol.packet.Title();
    title.setAction(action);
    if (action == net.md_5.bungee.protocol.packet.Title.Action.TIMES) {
      title.setFadeIn(20);
      title.setStay(60);
      title.setFadeOut(20);
    }
    return title;
  }

  private static void sendPacket(final ProxiedPlayer player, final DefinedPacket packet) {
    if (packet != null) {
      player.unsafe().sendPacket(packet);
    }
  }

  @Override
  public Title title(final BaseComponent text) {
    if (this.title == null) {
      this.title = createPacket(net.md_5.bungee.protocol.packet.Title.Action.TITLE);
    }
    this.title.setText(ComponentSerializer.toString(text));
    return this;
  }

  @Override
  public Title title(final BaseComponent... text) {
    if (this.title == null) {
      this.title = createPacket(net.md_5.bungee.protocol.packet.Title.Action.TITLE);
    }
    this.title.setText(ComponentSerializer.toString(text));
    return this;
  }

  @Override
  public Title subTitle(final BaseComponent text) {
    if (this.subtitle == null) {
      this.subtitle = createPacket(net.md_5.bungee.protocol.packet.Title.Action.SUBTITLE);
    }
    this.subtitle.setText(ComponentSerializer.toString(text));
    return this;
  }

  @Override
  public Title subTitle(final BaseComponent... text) {
    if (this.subtitle == null) {
      this.subtitle = createPacket(net.md_5.bungee.protocol.packet.Title.Action.SUBTITLE);
    }
    this.subtitle.setText(ComponentSerializer.toString(text));
    return this;
  }

  @Override
  public Title fadeIn(final int ticks) {
    if (this.times == null) {
      this.times = createPacket(net.md_5.bungee.protocol.packet.Title.Action.TIMES);
    }
    this.times.setFadeIn(ticks);
    return this;
  }

  @Override
  public Title stay(final int ticks) {
    if (this.times == null) {
      this.times = createPacket(net.md_5.bungee.protocol.packet.Title.Action.TIMES);
    }
    this.times.setStay(ticks);
    return this;
  }

  @Override
  public Title fadeOut(final int ticks) {
    if (this.times == null) {
      this.times = createPacket(net.md_5.bungee.protocol.packet.Title.Action.TIMES);
    }
    this.times.setFadeOut(ticks);
    return this;
  }

  @Override
  public Title clear() {
    if (this.clear == null) {
      this.clear = createPacket(net.md_5.bungee.protocol.packet.Title.Action.CLEAR);
    }
    this.title = null;
    return this;
  }

  @Override
  public Title reset() {
    if (this.reset == null) {
      this.reset = createPacket(net.md_5.bungee.protocol.packet.Title.Action.RESET);
    }
    this.title = null;
    this.subtitle = null;
    this.times = null;
    return this;
  }

  @Override
  public Title send(final ProxiedPlayer player) {
    if (ProtocolConstants.isBeforeOrEq(player.getPendingConnection().getVersion(), 5)) {
      return this;
    }
    sendPacket(player, this.clear);
    sendPacket(player, this.reset);
    sendPacket(player, this.times);
    sendPacket(player, this.subtitle);
    sendPacket(player, this.title);
    return this;
  }
}
