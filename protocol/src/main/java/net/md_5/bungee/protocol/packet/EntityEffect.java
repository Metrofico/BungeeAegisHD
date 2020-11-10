package net.md_5.bungee.protocol.packet;

import io.netty.buffer.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.md_5.bungee.protocol.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class EntityEffect extends MultiVersionPacketV17 {

  private int entityId;
  private int effectId;
  private int amplifier;
  private int duration;
  private boolean hideParticles;

  @Override
  protected void v17Read(final ByteBuf buf, final ProtocolConstants.Direction direction,
      final int protocolVersion) {
    this.entityId = buf.readInt();
    this.effectId = buf.readUnsignedByte();
    this.amplifier = buf.readUnsignedByte();
    this.duration = buf.readShort();
  }

  @Override
  public void read(final ByteBuf buf) {
    this.entityId = DefinedPacket.readVarInt(buf);
    this.effectId = buf.readUnsignedByte();
    this.amplifier = buf.readUnsignedByte();
    this.duration = DefinedPacket.readVarInt(buf);
    this.hideParticles = buf.readBoolean();
  }

  @Override
  protected void v17Write(final ByteBuf buf) {
    buf.writeInt(this.effectId);
    buf.writeByte(this.effectId);
    buf.writeByte(this.amplifier);
    buf.writeShort(this.duration);
  }

  @Override
  public void write(final ByteBuf buf) {
    DefinedPacket.writeVarInt(this.entityId, buf);
    buf.writeByte(this.effectId);
    buf.writeByte(this.amplifier);
    DefinedPacket.writeVarInt(this.duration, buf);
    buf.writeBoolean(this.hideParticles);
  }

  @Override
  public void handle(final AbstractPacketHandler handler) throws Exception {
    handler.handle(this);
  }

  public int getEntityId() {
    return this.entityId;
  }

  public int getEffectId() {
    return this.effectId;
  }

  public int getAmplifier() {
    return this.amplifier;
  }

  public int getDuration() {
    return this.duration;
  }

  public boolean isHideParticles() {
    return this.hideParticles;
  }

  public void setEntityId(final int entityId) {
    this.entityId = entityId;
  }

  public void setEffectId(final int effectId) {
    this.effectId = effectId;
  }

  public void setAmplifier(final int amplifier) {
    this.amplifier = amplifier;
  }

  public void setDuration(final int duration) {
    this.duration = duration;
  }

  public void setHideParticles(final boolean hideParticles) {
    this.hideParticles = hideParticles;
  }

}