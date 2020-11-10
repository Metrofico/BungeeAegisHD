package net.md_5.bungee.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.net.InetSocketAddress;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Setter;
import net.md_5.bungee.protocol.packet.EntityEffect;
import net.md_5.bungee.protocol.packet.EntityRemoveEffect;
import net.md_5.bungee.protocol.packet.GameState;
import net.md_5.bungee.protocol.packet.Handshake;
import net.md_5.bungee.protocol.packet.LoginRequest;
import net.md_5.bungee.protocol.packet.PingPacket;
import net.md_5.bungee.protocol.packet.StatusRequest;

@AllArgsConstructor
public class MinecraftDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Setter
    private Protocol protocol;
    private final boolean server;
    @Setter
    private int protocolVersion;

    private boolean stop;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!ctx.channel().isActive()) {
            return;
        }

        if (this.stop) {
            // if (in.refCnt() > 0) {
            //   in.release();
            // }
            return;
        }

        if (!server && in.readableBytes() == 0) //Fix empty packet from server
        {
            this.stop = true;
            return;
        }

        Protocol.DirectionData prot = (server) ? protocol.TO_SERVER : protocol.TO_CLIENT;
        int originalReaderIndex = in.readerIndex();
        int originalReadableBytes = in.readableBytes();

        int packetId;
        try {
            packetId = DefinedPacket.readVarInt(in);
        } catch (FastException exception) {
            this.stop = true;
                /*if (in.refCnt() > 0)
                    in.release();
                if (slice.refCnt() > 0)
                    slice.release();*/
            throw exception;
        }

        DefinedPacket packet;
        try {
            packet = prot.createPacket(packetId, protocolVersion);
        } catch (FastException ex) {
            this.stop = true;
                /*if (in.refCnt() > 0)
                    in.release();
                if (slice.refCnt() > 0)
                    slice.release();*/

            throw ex;
        }
        if (packet != null) {
            try {
                packet.read0(in, prot.getDirection(), protocolVersion);
            } catch (Exception ex) {
                if (!(packet instanceof LoginRequest)) {
                    this.stop = true;
                        /*if (in.refCnt() > 0)
                            in.release();
                        if (slice.refCnt() > 0)
                            slice.release();*/
                    throw new FastException("[Exploit] Packet read error");
                }
            }

            if (in.isReadable() && this.server) {
                in.skipBytes(in.readableBytes());
                this.stop = true;
                //if (in.refCnt() > 0)
                // in.release();
                //if (slice.refCnt() > 0)
                //  slice.release();
                throw new FastException("[Exploit] Packet still readable, too large");
            } else if (in.isReadable() && !server) {
                in.skipBytes(in.readableBytes());

                if (packet instanceof GameState || packet instanceof EntityEffect
                    || packet instanceof EntityRemoveEffect) {
                    System.out.println("[IMPORTANT - REPORT TO DEVELOPER, YOONIKS#0289] "
                        + "Did not read all bytes from packet " + packet.getClass() +
                        +packetId
                        + " Protocol " + protocol + " Direction " + prot.getDirection());
                } else {
                    throw new BadPacketException(
                        "Did not read all bytes from packet " + packet.getClass() + " "
                            + packetId
                            + " Protocol " + protocol + " Direction " + prot.getDirection());
                }
            }
        } else {
            in.skipBytes(in.readableBytes());
        }

        ByteBuf slice = in.copy( originalReaderIndex, originalReadableBytes );
        out.add(new PacketWrapper(packet, slice));
    }

    /*@Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
    {
        if (this.stop) {
            return;
        }
        int originalReaderIndex = in.readerIndex();
        int originalReadableBytes = in.readableBytes();
        int packetId = DefinedPacket.readVarInt( in );
        if ( packetId < 0 || packetId > Protocol.MAX_PACKET_ID )
        {
            this.stop = true;
            DiscardUtils.injectAndClose( ctx.channel() ).addListener( (ChannelFutureListener) future ->
            {
                ErrorStream.error( "[" + ctx.channel().remoteAddress() + "] Received invalid packet id " + packetId + ", blocked" );
            } );
            AddressBlocker.block(((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress());
            return;
        }
        Protocol.DirectionData prot = ( server ) ? protocol.TO_SERVER : protocol.TO_CLIENT;
        int protocolVersion = this.protocolVersion;
        DefinedPacket packet = prot.createPacket( packetId, protocolVersion );
        if ( packet != null )
        {
            if ( packet instanceof Handshake || packet instanceof PingPacket || packet instanceof StatusRequest)
            {
                try
                {
                    packet.read0( in, prot.getDirection(), protocolVersion );
                } catch ( Exception e )
                {
                    this.stop = true;
                    DiscardUtils.injectAndClose( ctx.channel() ).addListener( (ChannelFutureListener) future ->
                    {
                        ErrorStream.error( "[" + ctx.channel().remoteAddress() + "] Wrong handshake received and blocked" );
                    } );
                    AddressBlocker.block(((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress());
                    return;
                }
            } else
            {
                packet.read0( in, prot.getDirection(), protocolVersion );
            }
            if ( in.isReadable() )
            {
                in.skipBytes( in.readableBytes() );
                if ( server )
                {
                    this.stop = true;
                    DiscardUtils.injectAndClose( ctx.channel() ).addListener( (ChannelFutureListener) future ->
                    {
                        ErrorStream.error( "[" + ctx.channel().remoteAddress() + "] Received longer packet than we expected " + packet.getClass().getSimpleName() );
                    } );
                    AddressBlocker.block(((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress());
                    return;
                }
                throw new BadPacketException( "Did not read all bytes from packet " + packet.getClass() + " " + packetId + " Protocol " + protocol + " Direction " + prot.getDirection() );
            }
        } else
        {
            in.skipBytes( in.readableBytes() );
        }
        ByteBuf copy = in.copy( originalReaderIndex, originalReadableBytes );
        out.add( new PacketWrapper( packet, copy ) );
    }*/

    /*@Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws Exception {
        if (this.stop) {
            ctx.close();
            return;
        }

        final Protocol.DirectionData prot =
            this.server ? this.protocol.TO_SERVER : this.protocol.TO_CLIENT;
        ByteBuf slice = in.copy();
        try {

            if (this.server && in.readableBytes() < 1) {
                this.stop = true;

                final InetAddress address = ((InetSocketAddress) ctx.channel().remoteAddress())
                    .getAddress();
                ctx.pipeline().addFirst("I_DISCARD_FIRST", InboundDiscardHandler.INSTANCE)
                    .addAfter(ctx.name(), "I_DISCARD", InboundDiscardHandler.INSTANCE);
                AddressBlocker.block(address.getHostAddress());
                ctx.close();

                throw new FastException("Wrong bytes");
            }

            if (in.readableBytes() == 0) {
                return;
            }
            final int packetId;
            try {
                packetId = DefinedPacket.readVarIntPacketIdSpecial(in);
            } catch (Exception ex) {
                this.stop = true;

                final InetAddress address = ((InetSocketAddress) ctx.channel().remoteAddress())
                    .getAddress();
                AddressBlocker.block(address.getHostAddress());

                ctx.close();
                throw new FastException("Wrong packet id (1)");
            }
            if (packetId < 0 || packetId > 255) {
                this.stop = true;

                ctx.pipeline().addFirst("I_DISCARD_FIRST", InboundDiscardHandler.INSTANCE)
                    .addAfter(ctx.name(), "I_DISCARD", InboundDiscardHandler.INSTANCE);
                ctx.close();

                final InetAddress address = ((InetSocketAddress) ctx.channel().remoteAddress())
                    .getAddress();
                AddressBlocker.block(address.getHostAddress());
                throw new FastException("Wrong packet id (2) (Got: " + packetId + ")");
            }

            DefinedPacket packet;
            try {
                packet = prot.createPacket(packetId, protocolVersion);
            } catch (Exception ex) {
                this.stop = true;

                ctx.close();
                final InetAddress address = ((InetSocketAddress) ctx.channel().remoteAddress())
                    .getAddress();
                AddressBlocker.block(address.getHostAddress());

                throw new FastException("Create packet error packetId: " + packetId);
            }
            if (packet != null) {
                try {
                    packet.read0(in, prot.getDirection(), this.protocolVersion);
                }
                catch (Exception ex) {
                    if (!(packet instanceof LoginRequest)) {
                        this.stop = true;

                        ctx.close();

                        ctx.pipeline().addFirst("I_DISCARD_FIRST", InboundDiscardHandler.INSTANCE)
                            .addAfter(ctx.name(), "I_DISCARD", InboundDiscardHandler.INSTANCE);

                        final InetAddress address = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();
                        AddressBlocker.block(address.getHostAddress());

                        if (slice.refCnt() > 0)
                            slice.release();
                        throw new FastException("Could not read the packet! (" + packet.getClass().getSimpleName() + " - " + ex.getMessage() + ")");
                    }
                    return;
                }
                if (in.isReadable()) {
                    this.stop = true;
                    ctx.close();

                    if (slice.refCnt() > 0)
                        slice.release();

                    final InetAddress address = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();
                    AddressBlocker.block(address.getHostAddress());

                    if (System.currentTimeMillis() > broadcastAt) {
                        System.out.println("[Aegis] Blocked: " + address.getHostAddress()
                            + " (did not read all bytes)");
                        broadcastAt = System.currentTimeMillis() + 1000L * 5;
                    }

                    throw new FastException("Did not read all bytes!"); }
            }
            else {
                in.skipBytes(in.readableBytes());
            }
            out.add(new PacketWrapper(packet, slice));
            slice = null;
        }
        finally {
            if (slice != null) {
                if (slice.refCnt() > 0)
                    slice.release();
            }
        }
    }*/

    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception
    {
        return msg instanceof ByteBuf;
    }

}