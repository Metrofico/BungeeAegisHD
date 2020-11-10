package net.md_5.bungee.protocol;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

public class DiscardUtils
{

    public static ChannelFuture injectAndClose(Channel channel)
    {
        if ( channel.pipeline().get( ChannelDiscardHandler.class ) == null )
        {
            channel.pipeline().addBefore( "packet-decoder", ChannelDiscardHandler.DISCARD, ChannelDiscardHandler.INSTANCE );
            channel.config().setAutoRead( false );
        }
        return channel.close();
    }

}
