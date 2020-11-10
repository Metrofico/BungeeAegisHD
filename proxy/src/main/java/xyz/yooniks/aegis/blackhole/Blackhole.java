package xyz.yooniks.aegis.blackhole;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.packet.Kick;
import xyz.yooniks.aegis.config.Settings;
import xyz.yooniks.aegis.utils.MessageBuilder;

public enum Blackhole {

  INSTANCE;

  private boolean enabled = false;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  private final DefinedPacket kickPacket = new Kick(

      ComponentSerializer.toString(new TextComponent(
          MessageBuilder.newBuilder()
              .withMessage(Settings.IMP.MESSAGES.BLACKHOLE_KICK)
              .coloured().stripped()
              .build()))
  );

  public DefinedPacket getKickPacket() {
    return kickPacket;
  }

}
