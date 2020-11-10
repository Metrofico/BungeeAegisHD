package xyz.yooniks.aegis.utils;

import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.protocol.DefinedPacket;

public final class WritePacketUtil {

  private WritePacketUtil() {
  }

  public static void writePacket(DefinedPacket packet, ChannelWrapper wrapper) {
    if (wrapper.isClosed() || wrapper.isClosing()) {
      return;
    }
    wrapper.write(packet);
  }

}
