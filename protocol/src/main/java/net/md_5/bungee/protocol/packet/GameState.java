package net.md_5.bungee.protocol.packet;

import java.util.Objects;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import io.netty.buffer.ByteBuf;
import net.md_5.bungee.protocol.DefinedPacket;

public class GameState extends DefinedPacket {

  public static final short IMMEDIATE_RESPAWN = 11;
  private short state;
  private float value;

  @Override
  public void read(final ByteBuf buf) {
    this.state = buf.readUnsignedByte();
    this.value = buf.readFloat();
  }

  @Override
  public void write(final ByteBuf buf) {
    buf.writeByte(this.state);
    buf.writeFloat(this.value);
  }

  @Override
  public void handle(final AbstractPacketHandler handler) throws Exception {
    handler.handle(this);
  }

  public short getState() {
    return this.state;
  }

  public float getValue() {
    return this.value;
  }

  public void setState(final short state) {
    this.state = state;
  }

  public void setValue(final float value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "GameState(state=" + this.getState() + ", value=" + this.getValue() + ")";
  }

  public GameState() {
    super();
  }

  public GameState(final short state, final float value) {
    super();
    this.state = state;
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GameState gameState = (GameState) o;
    return state == gameState.state &&
        Float.compare(gameState.value, value) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(state, value);
  }
}