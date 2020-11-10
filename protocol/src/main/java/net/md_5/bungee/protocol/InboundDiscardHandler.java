package net.md_5.bungee.protocol;

import io.netty.channel.*;
import io.netty.buffer.*;

@ChannelHandler.Sharable
public class InboundDiscardHandler extends ChannelInboundHandlerAdapter {

  public static final InboundDiscardHandler INSTANCE =  new InboundDiscardHandler();

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
    if (msg instanceof ByteBuf) {
      ((ByteBuf)msg).release();

      Channel ch = ctx.channel();
      if ( ch.isActive() )
      {
        ch.close();
      }
      //ctx.close();
    }
  }

}
