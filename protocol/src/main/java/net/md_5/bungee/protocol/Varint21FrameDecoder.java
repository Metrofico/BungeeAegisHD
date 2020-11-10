package net.md_5.bungee.protocol;

import io.netty.handler.codec.*;
import io.netty.buffer.*;
import java.util.*;
import java.net.*;
import io.netty.channel.*;

public class Varint21FrameDecoder extends ByteToMessageDecoder
{

    public static int MIN_LENGTH_FIRST_PACKET = 1 + 1 + ( 1 + 1 ) + 2 + 1 - 4; //-4
    public static int MAX_LENGTH_FIRST_PACKET = 1 + 5 + ( 3 + 255 * 4 ) + 2 + 1 + 3; //+3
    public static int MIN_LENGTH_SECOND_PACKET = 1 + 1 + 1;
    public static int MAX_LENGTH_SECOND_PACKET = 1 + ( 1 + 16 * 4 );
    private boolean first;
    private boolean second;
    private boolean stop = false;


    public static boolean ALLOW_EMPTY_PACKETS = false;

    public Varint21FrameDecoder() {
        this.first = true;
        this.second = false;
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) {

        if ( !ctx.channel().isActive() )
        {
            in.skipBytes( in.readableBytes() );
            super.setSingleDecode( true );
            return;
        }

        if (this.stop) {
            //if (in.refCnt() > 0) {
            //    in.release();
            //}
            return;
        }

        in.markReaderIndex();
        final byte[] buf = new byte[3];
        int i = 0;
        while (i < buf.length) {
            if (!in.isReadable()) {
                in.resetReaderIndex();
                return;
            }
            if ((buf[i] = in.readByte()) >= 0) {
                final int length = DefinedPacket.readVarIntLengthSpecial(buf);
                if (length <= 0 && ALLOW_EMPTY_PACKETS) {
                    this.stop = true;
                    super.setSingleDecode(true);
                    //if (in.refCnt() >0)
                    //    in.release();

                    throw new FastException("[Exploit] Too small packet length");
                }
                if (first) {
                    if (length < MIN_LENGTH_FIRST_PACKET || length > MAX_LENGTH_FIRST_PACKET) {
                        final InetAddress address = ((InetSocketAddress) ctx.channel()
                            .remoteAddress()).getAddress();

                        final String hostname = address.getHostAddress();
                        if (!hostname.equals("127.0.0.1") && !hostname.equals("0.0.0.0")) {
                            super.setSingleDecode(true);
                            //if (in.refCnt() >0)
                            //    in.release();

                            this.stop = true;
                            throw new FastException("[Exploit] Received invalid "
                                + "first login packet! Min: "
                                + MIN_LENGTH_FIRST_PACKET + ", max: " + MAX_LENGTH_FIRST_PACKET
                                + ", current: " + length);
                        }
                    }
                } else if (this.second) {
                    this.second = false;
                    if (length != 1 && (length < MIN_LENGTH_SECOND_PACKET
                        || length > MAX_LENGTH_SECOND_PACKET)) {

                        final InetAddress address = ((InetSocketAddress) ctx.channel()
                            .remoteAddress()).getAddress();

                        final String hostname = address.getHostAddress();

                        if (!hostname.equals("127.0.0.1")) {

                            super.setSingleDecode(true);
                            this.stop = true;
                            //if (in.refCnt() >0)
                            //    in.release();

                            throw new FastException("[Exploit] Received invalid "
                                + "second login packet! Min: "
                                + MIN_LENGTH_SECOND_PACKET + ", max: " + MAX_LENGTH_SECOND_PACKET
                                + ", current: " + length);
                        }
                    }
                }
                if (in.readableBytes() < length) {
                    in.resetReaderIndex();
                    return;
                }
                if (this.first) {
                    this.first = false;
                    this.second = true;
                }
                if (in.hasMemoryAddress()) {
                    out.add(in.slice(in.readerIndex(), length).retain());
                    in.skipBytes(length);
                } else {
                    final ByteBuf dst = ctx.alloc().directBuffer(length);
                    in.readBytes(dst);
                    out.add(dst);
                }
                return;
            } else {
                ++i;
            }
        }

        super.setSingleDecode(true);
        this.stop = true;
        //if (in.refCnt() >0)
        //    in.release();

        throw new FastException("[Exploit] 21 bit packet");
    }

    /*@Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) {
        if (this.stop) {
            ctx.close();
            return;
        }

        in.markReaderIndex();
        final byte[] buf = new byte[3];
        int i = 0;
        while (i < buf.length) {
            if (!in.isReadable()) {
                in.resetReaderIndex();
                return;
            }
            if ((buf[i] = in.readByte()) >= 0) {
                final int length = DefinedPacket.readVarIntLengthSpecial(buf);
                if (length <= 0) {
                    this.stop = true;

                    final InetAddress address = ((InetSocketAddress) ctx.channel().remoteAddress())
                        .getAddress();

                    super.setSingleDecode(true);
                    AddressBlocker.block(address.getHostAddress());
                    ctx.close();
                    return;
                }
                if ( first )
                {
                    if ( length < MIN_LENGTH_FIRST_PACKET || length > MAX_LENGTH_FIRST_PACKET ) {
                        final InetAddress address = ((InetSocketAddress) ctx.channel()
                            .remoteAddress()).getAddress();

                        final String hostname = address.getHostAddress();
                        if (!hostname.equals("127.0.0.1") && !hostname.equals("0.0.0.0")) {
                            super.setSingleDecode(true);
                            this.stop = true;
                            DiscardUtils.injectAndClose( ctx.channel() ).addListener( (ChannelFutureListener) future ->
                            {
                                ErrorStream.error( "[" + ctx.channel().remoteAddress() + "] Received invalid "
                                    + "first login packet! Min: "
                                    + MIN_LENGTH_FIRST_PACKET + ", max: " + MAX_LENGTH_FIRST_PACKET
                                    + ", current: " + length );
                            } );
                            AddressBlocker.block(hostname);

                            return;
                        }
                    }
                }
                else if (this.second) {
                    this.second = false;
                    if (length != 1 && (length < MIN_LENGTH_SECOND_PACKET
                        || length > MAX_LENGTH_SECOND_PACKET)) {

                        final InetAddress address = ((InetSocketAddress) ctx.channel()
                            .remoteAddress()).getAddress();

                        final String hostname = address.getHostAddress();

                        if (!hostname.equals("127.0.0.1")) {

                            super.setSingleDecode(true);
                            this.stop = true;

                            DiscardUtils.injectAndClose( ctx.channel() ).addListener( (ChannelFutureListener) future ->
                            {
                                ErrorStream.error( "[" + ctx.channel().remoteAddress() + "] Received invalid "
                                    + "second login packet! Min: "
                                + MIN_LENGTH_SECOND_PACKET + ", max: " + MAX_LENGTH_SECOND_PACKET
                                + ", current: " + length );
                            } );
                            AddressBlocker.block(hostname);
                            return;
                        }
                    }
                }
                if (in.readableBytes() < length) {
                    in.resetReaderIndex();
                    return;
                }
                if (this.first) {
                    this.first = false;
                    this.second = true;
                }
                if (in.hasMemoryAddress()) {
                    out.add(in.slice(in.readerIndex(), length).retain());
                    in.skipBytes(length);
                } else {
                    final ByteBuf dst = ctx.alloc().directBuffer(length);
                    in.readBytes(dst);
                    out.add(dst);
                }
                return;
            } else {
                ++i;
            }
        }

        super.setSingleDecode(true);
        this.stop = true;
        DiscardUtils.injectAndClose( ctx.channel() ).addListener( (ChannelFutureListener) future ->
        {
            ErrorStream.error( "[" + ctx.channel().remoteAddress() + "] Received larger packet than expected (21bit +)" );
        } );
        AddressBlocker.block(((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress());
    }*/

}
