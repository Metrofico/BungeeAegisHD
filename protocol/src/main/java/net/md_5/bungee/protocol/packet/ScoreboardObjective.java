package net.md_5.bungee.protocol.packet;

import net.md_5.bungee.protocol.DefinedPacket;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
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
public class ScoreboardObjective extends MultiVersionPacketV17
{

    private String name;
    private String value;
    private HealthDisplay type;
    /**
     * 0 to create, 1 to remove, 2 to update display text.
     */
    private byte action;

    //BotFilter start
    @Deprecated
    public ScoreboardObjective(String name, String value, String type, byte action)
    {
        this.name = name;
        this.value = value;
        this.type = HealthDisplay.fromString( type );
        this.action = action;
    }

    public void v17Read(final ByteBuf buf, final ProtocolConstants.Direction direction, final int protocolVersion) {
        this.name = DefinedPacket.readString(buf);
        this.value = DefinedPacket.readString(buf);
        this.action = buf.readByte();
    }

    @Override
    public void read(final ByteBuf buf, final ProtocolConstants.Direction direction, final int protocolVersion) {
        name = readString( buf );
        if ( protocolVersion <= ProtocolConstants.MINECRAFT_1_7_6 )
        {
            value = readString( buf );
        }
        action = buf.readByte();
        if ( protocolVersion >= ProtocolConstants.MINECRAFT_1_8 && ( action == 0 || action == 2 ) )
        {
            value = readString( buf );
            if ( protocolVersion >= ProtocolConstants.MINECRAFT_1_13 )
            {
                type = HealthDisplay.values()[readVarInt( buf )];
            } else
            {
                type = HealthDisplay.fromString( readString( buf ) );
            }
        }
    }

    public void v17Write(final ByteBuf buf, final ProtocolConstants.Direction direction, final int protocolVersion) {
        DefinedPacket.writeString(this.name, buf);
        DefinedPacket.writeString(this.value, buf);
        buf.writeByte(this.action);
    }

    @Override
    public void write(final ByteBuf buf, final ProtocolConstants.Direction direction, final int protocolVersion) {
        writeString( name, buf );
        if ( protocolVersion <= ProtocolConstants.MINECRAFT_1_7_6 )
        {
            writeString( value, buf );
        }
        buf.writeByte( action );
        if ( protocolVersion >= ProtocolConstants.MINECRAFT_1_8 && ( action == 0 || action == 2 ) )
        {
            writeString( value, buf );
            if ( protocolVersion >= ProtocolConstants.MINECRAFT_1_13 )
            {
                writeVarInt( type.ordinal(), buf );
            } else
            {
                writeString( type.toString(), buf );
            }
        }
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception
    {
        handler.handle( this );
    }

    public enum HealthDisplay
    {

        INTEGER, HEARTS;

        @Override
        public String toString()
        {
            return super.toString().toLowerCase( Locale.ROOT );
        }

        public static HealthDisplay fromString(String s)
        {
            return valueOf( s.toUpperCase( Locale.ROOT ) );
        }
    }
}
