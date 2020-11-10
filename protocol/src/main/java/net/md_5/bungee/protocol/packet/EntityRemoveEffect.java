package net.md_5.bungee.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.MultiVersionPacketV17;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class EntityRemoveEffect extends MultiVersionPacketV17 {

  private int entityId;
  private int effectId;

  @Override
  public void read(final ByteBuf buf) {
    this.entityId = DefinedPacket.readVarInt(buf);
    this.effectId = buf.readUnsignedByte();
  }

  @Override
  protected void v17Read(final ByteBuf buf) {
    this.entityId = buf.readInt();
    this.effectId = buf.readUnsignedByte();
  }

  @Override
  public void write(final ByteBuf buf) {
    DefinedPacket.writeVarInt(this.entityId, buf);
    buf.writeByte(this.effectId);
  }

  @Override
  public void handle(final AbstractPacketHandler handler) throws Exception {
    handler.handle(this);
  }

  @Override
  protected void v17Write(final ByteBuf buf) {
    buf.writeInt(this.entityId);
    buf.writeByte(this.effectId);
  }

}