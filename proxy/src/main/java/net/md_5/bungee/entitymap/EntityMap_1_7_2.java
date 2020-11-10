package net.md_5.bungee.entitymap;

import io.netty.buffer.ByteBuf;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;

class EntityMap_1_7_2 extends EntityMap {

  static final EntityMap INSTANCE = new EntityMap_1_7_2();

  EntityMap_1_7_2() {
    this.addRewrite(4, ProtocolConstants.Direction.TO_CLIENT, false);
    this.addRewrite(10, ProtocolConstants.Direction.TO_CLIENT, false);
    this.addRewrite(11, ProtocolConstants.Direction.TO_CLIENT, true);
    this.addRewrite(12, ProtocolConstants.Direction.TO_CLIENT, true);
    this.addRewrite(13, ProtocolConstants.Direction.TO_CLIENT, false);
    this.addRewrite(14, ProtocolConstants.Direction.TO_CLIENT, true);
    this.addRewrite(15, ProtocolConstants.Direction.TO_CLIENT, true);
    this.addRewrite(16, ProtocolConstants.Direction.TO_CLIENT, true);
    this.addRewrite(17, ProtocolConstants.Direction.TO_CLIENT, true);
    this.addRewrite(18, ProtocolConstants.Direction.TO_CLIENT, false);
    this.addRewrite(20, ProtocolConstants.Direction.TO_CLIENT, false);
    this.addRewrite(21, ProtocolConstants.Direction.TO_CLIENT, false);
    this.addRewrite(22, ProtocolConstants.Direction.TO_CLIENT, false);
    this.addRewrite(23, ProtocolConstants.Direction.TO_CLIENT, false);
    this.addRewrite(24, ProtocolConstants.Direction.TO_CLIENT, false);
    this.addRewrite(25, ProtocolConstants.Direction.TO_CLIENT, false);
    this.addRewrite(26, ProtocolConstants.Direction.TO_CLIENT, false);
    this.addRewrite(27, ProtocolConstants.Direction.TO_CLIENT, false);
    this.addRewrite(28, ProtocolConstants.Direction.TO_CLIENT, false);
    this.addRewrite(29, ProtocolConstants.Direction.TO_CLIENT, false);
    this.addRewrite(30, ProtocolConstants.Direction.TO_CLIENT, false);
    this.addRewrite(32, ProtocolConstants.Direction.TO_CLIENT, false);
    this.addRewrite(37, ProtocolConstants.Direction.TO_CLIENT, true);
    this.addRewrite(44, ProtocolConstants.Direction.TO_CLIENT, true);
    this.addRewrite(2, ProtocolConstants.Direction.TO_SERVER, false);
    this.addRewrite(10, ProtocolConstants.Direction.TO_SERVER, false);
    this.addRewrite(11, ProtocolConstants.Direction.TO_SERVER, false);
  }

  @Override
  public void rewriteClientbound(ByteBuf packet, int oldId, int newId) {
    super.rewriteClientbound(packet, oldId, newId);

    //Special cases
    int readerIndex = packet.readerIndex();
    int packetId = DefinedPacket.readVarInt(packet);
    int packetIdLength = packet.readerIndex() - readerIndex;
    if (packetId == 0x0D /* Collect Item */ || packetId == 0x1B /* Attach Entity */) {
      rewriteInt(packet, oldId, newId, readerIndex + packetIdLength + 4);
    } else if (packetId == 0x13 /* Destroy Entities */) {
      int count = packet.getByte(packetIdLength);
      for (int i = 0; i < count; i++) {
        rewriteInt(packet, oldId, newId, packetIdLength + 1 + i * 4);
      }
    } else if (packetId == 0x0E /* Spawn Object */) {
      DefinedPacket.readVarInt(packet);
      int type = packet.readUnsignedByte();

      if (type == 60 || type == 90) {
        packet.skipBytes(14);
        int position = packet.readerIndex();
        int readId = packet.readInt();
        int changedId = -1;
        if (readId == oldId) {
          packet.setInt(position, newId);
          changedId = newId;
        } else if (readId == newId) {
          packet.setInt(position, oldId);
          changedId = oldId;
        }
        if (changedId != -1) {
          if (changedId == 0 && readId != 0) { // Trim off the extra data
            packet.readerIndex(readerIndex);
            packet.writerIndex(packet.readableBytes() - 6);
          } else if (changedId != 0 && readId == 0) { // Add on the extra data
            packet.readerIndex(readerIndex);
            packet.capacity(packet.readableBytes() + 6);
            packet.writerIndex(packet.readableBytes() + 6);
          }
        }
      }
    }
    packet.readerIndex(readerIndex);
  }
}