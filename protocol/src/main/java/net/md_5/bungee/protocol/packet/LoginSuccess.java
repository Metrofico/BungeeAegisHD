package net.md_5.bungee.protocol.packet;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.ProtocolConstants;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class LoginSuccess extends DefinedPacket {

    private UUID uuid;
    private String username;

    @Override
    public void read(final ByteBuf buf, final ProtocolConstants.Direction direction,
        final int protocolVersion) {
        if (protocolVersion <= 4) {
            this.uuid = readUndashedUUID(buf);
        } else if (protocolVersion >= 735) {
            this.uuid = DefinedPacket.readUUID(buf);
        } else {
            this.uuid = UUID.fromString(DefinedPacket.readString(buf));
        }
        this.username = DefinedPacket.readString(buf);
    }

    @Override
    public void write(final ByteBuf buf, final ProtocolConstants.Direction direction,
        final int protocolVersion) {
        if (protocolVersion <= 4) {
            writeUndashedUUID(this.uuid.toString(), buf);
        } else if (protocolVersion >= 735) {
            DefinedPacket.writeUUID(this.uuid, buf);
        } else {
            DefinedPacket.writeString(this.uuid.toString(), buf);
        }
        DefinedPacket.writeString(this.username, buf);
    }

    private static UUID readUndashedUUID(final ByteBuf buf) {
        return UUID.fromString(
            new StringBuilder(DefinedPacket.readString(buf)).insert(20, '-').insert(16, '-')
                .insert(12, '-').insert(8, '-').toString());
    }

    private static void writeUndashedUUID(final String uuid, final ByteBuf buf) {
        DefinedPacket.writeString(
            new StringBuilder(32).append(uuid, 0, 8).append(uuid, 9, 13).append(uuid, 14, 18)
                .append(uuid, 19, 23).append(uuid, 24, 36).toString(), buf);
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {
        handler.handle(this);
    }

}
