package xyz.yooniks.aegis.caching;

import io.netty.buffer.ByteBuf;
import net.md_5.bungee.protocol.Protocol;
import xyz.yooniks.aegis.filter.Connector;
import xyz.yooniks.aegis.packets.SetExp;

/**
 * @author Leymooo
 */
public class CachedExpPackets {

  private final ByteBuf[/*tick*/][/*mc version*/] byteBufAuth = new ByteBuf[31][PacketUtils.PROTOCOLS_COUNT];
  private ByteBuf[/*tick*/][/*mc version*/] byteBuf = new ByteBuf[Connector.TOTAL_TICKS][PacketUtils.PROTOCOLS_COUNT];

  public CachedExpPackets() {
    create();
    createAuth();
  }

  private void create() {
    int ticks = Connector.TOTAL_TICKS;
    int interval = 2;
    float expinterval = 1f / ((float) ticks / (float) interval);
    SetExp setExp = new SetExp(0, 0, 0);
    for (int i = 0; i < /*ticks*/ticks; i = i + interval) {
      setExp.setExpBar(setExp.getExpBar() + expinterval);
      setExp.setLevel(setExp.getLevel() + 1);
      PacketUtils.fillArray(byteBuf[i], setExp, Protocol.BotFilter);
    }
  }

  private void createAuth() {
    int ticks = 31;

    int interval = 1;
    float expinterval = 1f / ((float) ticks / (float) interval);

    SetExp setExp = new SetExp(0, 0, 0);
    for (int i = 0; i < /*ticks*/ticks; i = i + interval) {
      setExp.setExpBar(setExp.getExpBar() + expinterval);
      setExp.setLevel(setExp.getLevel() + 1);
      PacketUtils.fillArray(byteBufAuth[i], setExp, Protocol.BotFilter);
    }
  }

  public ByteBuf getAuth(int tick, int version) {
    //if (version < ProtocolConstants.MINECRAFT_1_8) return null;

    ByteBuf buf = byteBufAuth[tick][PacketUtils.rewriteVersion(version)];
    return buf == null ? null : buf.retainedDuplicate();
  }

  public ByteBuf get(int tick, int version) {
    //if (version < ProtocolConstants.MINECRAFT_1_8) return null;

    ByteBuf buf = byteBuf[tick][PacketUtils.rewriteVersion(version)];
    return buf == null ? null : buf.retainedDuplicate();
  }

  public void release() {
    for (int i = 0; i < byteBuf.length; i++) {
      if (byteBuf[i] != null) {
        for (ByteBuf buf : byteBuf[i]) {
          PacketUtils.releaseByteBuf(buf);
        }
        byteBuf[i] = null;
      }

    }
    byteBuf = null;
  }

}