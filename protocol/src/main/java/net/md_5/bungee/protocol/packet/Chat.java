package net.md_5.bungee.protocol.packet;

import java.util.UUID;
import net.md_5.bungee.protocol.DefinedPacket;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.MultiVersionPacketV17;
import net.md_5.bungee.protocol.ProtocolConstants;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Chat extends MultiVersionPacketV17 {

    private static final UUID EMPTY_UUID = new UUID( 0L, 0L );
    private String message;
    private byte position;
    private UUID sender;

    public Chat(final String message) {
        this(message, (byte)0);
    }

    public Chat(final String message, final byte position) {
        this(message, position, Chat.EMPTY_UUID);
    }

    public void v17Read(final ByteBuf buf, final ProtocolConstants.Direction direction, final int protocolVersion) {
        this.message = DefinedPacket.readString(buf);
    }

    @Override
    public void read(final ByteBuf buf, final ProtocolConstants.Direction direction, final int protocolVersion) {
        if (direction == ProtocolConstants.Direction.TO_CLIENT) {
            this.message = DefinedPacket.readString(buf, 262144);
        }
        else {
            this.message = DefinedPacket.readString(buf);
        }
        if (direction == ProtocolConstants.Direction.TO_CLIENT) {
            this.position = buf.readByte();
            if (protocolVersion >= 735) {
                this.sender = DefinedPacket.readUUID(buf);
            }
        }
    }

    public void v17Write(final ByteBuf buf, final ProtocolConstants.Direction direction, final int protocolVersion) {
        DefinedPacket.writeString(this.message, buf);
    }

    @Override
    public void write(final ByteBuf buf, final ProtocolConstants.Direction direction, final int protocolVersion) {
        if (direction == ProtocolConstants.Direction.TO_CLIENT) {
            DefinedPacket.writeString(this.message, 262144, buf);
        }
        else {
            DefinedPacket.writeString(this.message, buf);
        }
        if (direction == ProtocolConstants.Direction.TO_CLIENT) {
            buf.writeByte(this.position);
            if (protocolVersion >= 735) {
                DefinedPacket.writeUUID(this.sender, buf);
            }
        }
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception
    {
        handler.handle( this );
    }
}
