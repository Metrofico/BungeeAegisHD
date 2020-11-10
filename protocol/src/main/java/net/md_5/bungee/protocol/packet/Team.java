package net.md_5.bungee.protocol.packet;

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
public class Team extends MultiVersionPacketV17 {

    private String name;
    /**
     * 0 - create, 1 remove, 2 info update, 3 player add, 4 player remove.
     */
    private byte mode;
    private String displayName;
    private String prefix;
    private String suffix;
    private String nameTagVisibility;
    private String collisionRule;
    private int color;
    private byte friendlyFire;
    private String[] players;

    /**
     * Packet to destroy a team.
     */
    public Team(String name) {
        this.name = name;
        this.mode = 1;
    }


    @Override
    public void v17Read(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        this.name = DefinedPacket.readString(buf);
        this.mode = buf.readByte();
        if (this.mode == 0 || this.mode == 2) {
            this.displayName = DefinedPacket.readString(buf);
            this.prefix = DefinedPacket.readString(buf);
            this.suffix = DefinedPacket.readString(buf);
            this.friendlyFire = buf.readByte();
        }
        if (this.mode == 0 || this.mode == 3 || this.mode == 4) {
            final int len = buf.readShort();
            this.players = new String[len];
            for (int i = 0; i < len; ++i) {
                this.players[i] = DefinedPacket.readString(buf);
            }
        }
    }

        @Override
    public void v17Write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
    {
        DefinedPacket.writeString(this.name, buf);
        buf.writeByte(this.mode);
        if (this.mode == 0 || this.mode == 2) {
            DefinedPacket.writeString(this.displayName, buf);
            DefinedPacket.writeString(this.prefix, buf);
            DefinedPacket.writeString(this.suffix, buf);
            buf.writeByte(this.friendlyFire);
        }
        if (this.mode == 0 || this.mode == 3 || this.mode == 4) {
            buf.writeShort(this.players.length);
            for (final String player : this.players) {
                DefinedPacket.writeString(player, buf);
            }
        }
    }
        
    
    @Override
    public void read(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
    {
        this.name = DefinedPacket.readString(buf);
        this.mode = buf.readByte();
        if (this.mode == 0 || this.mode == 2) {
            this.displayName = DefinedPacket.readString(buf);
            if (protocolVersion < 393) {
                this.prefix = DefinedPacket.readString(buf);
                this.suffix = DefinedPacket.readString(buf);
            }
            this.friendlyFire = buf.readByte();
            this.nameTagVisibility = DefinedPacket.readString(buf);
            if (protocolVersion >= 107) {
                this.collisionRule = DefinedPacket.readString(buf);
            }
            this.color = ((protocolVersion >= 393) ? DefinedPacket.readVarInt(buf) : buf.readByte());
            if (protocolVersion >= 393) {
                this.prefix = DefinedPacket.readString(buf);
                this.suffix = DefinedPacket.readString(buf);
            }
        }
        if (this.mode == 0 || this.mode == 3 || this.mode == 4) {
            final int len = DefinedPacket.readVarInt(buf);
            this.players = new String[len];
            for (int i = 0; i < len; ++i) {
                this.players[i] = DefinedPacket.readString(buf);
            }
        }
    }

    @Override
    public void write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
    {
        DefinedPacket.writeString(this.name, buf);
        buf.writeByte(this.mode);
        if (this.mode == 0 || this.mode == 2) {
            DefinedPacket.writeString(this.displayName, buf);
            if (protocolVersion < 393) {
                DefinedPacket.writeString(this.prefix, buf);
                DefinedPacket.writeString(this.suffix, buf);
            }
            buf.writeByte(this.friendlyFire);
            DefinedPacket.writeString(this.nameTagVisibility, buf);
            if (protocolVersion >= 107) {
                DefinedPacket.writeString(this.collisionRule, buf);
            }
            if (protocolVersion >= 393) {
                DefinedPacket.writeVarInt(this.color, buf);
                DefinedPacket.writeString(this.prefix, buf);
                DefinedPacket.writeString(this.suffix, buf);
            }
            else {
                buf.writeByte(this.color);
            }
        }
        if (this.mode == 0 || this.mode == 3 || this.mode == 4) {
            DefinedPacket.writeVarInt(this.players.length, buf);
            for (final String player : this.players) {
                DefinedPacket.writeString(player, buf);
            }
        }
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception
    {
        handler.handle( this );
    }
}
