package net.md_5.bungee.netty;

import com.google.common.base.Preconditions;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.connection.CancelSendSignal;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.PingHandler;
import net.md_5.bungee.log.ColouredWriter;
import net.md_5.bungee.protocol.AddressBlocker;
import net.md_5.bungee.protocol.FastException;
import net.md_5.bungee.protocol.PacketWrapper;
import xyz.yooniks.aegis.Aegis;
import xyz.yooniks.aegis.blacklist.Blacklist;
import xyz.yooniks.aegis.config.Settings;

/**
 * This class is a primitive wrapper for {@link PacketHandler} instances tied to channels to
 * maintain simple states, and only call the required, adapted methods when the channel is
 * connected.
 */
public class HandlerBoss extends ChannelInboundHandlerAdapter {

  private ChannelWrapper channel;
  private PacketHandler handler;

  public void setHandler(PacketHandler handler) {
    Preconditions.checkArgument(handler != null, "handler");
    if (this.handler != null) { //Aegis start
      this.handler.handlerChanged();
    } // END
    this.handler = handler;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    if (handler != null) {
      channel = new ChannelWrapper(ctx);
      handler.connected(channel);

      if (!(handler instanceof InitialHandler || handler instanceof PingHandler)) {
        ProxyServer.getInstance().getLogger().log(Level.INFO, "{0} has connected", handler);
      }
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    if (handler != null) {
      channel.markClosed();
      handler.disconnected(channel);

      if (!(handler instanceof InitialHandler || handler instanceof PingHandler)) {
        ProxyServer.getInstance().getLogger().log(Level.INFO, handler.toString() + " has disconnected");
      }
    }
  }

  @Override
  public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
    if (handler != null) {
      handler.writabilityChanged(channel);
    }
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof HAProxyMessage) {
      HAProxyMessage proxy = (HAProxyMessage) msg;
      InetSocketAddress newAddress = new InetSocketAddress(proxy.sourceAddress(),
          proxy.sourcePort());

      ProxyServer.getInstance().getLogger()
          .log(Level.FINE, "Set remote address via PROXY {0} -> {1}", new Object[]
              {
                  channel.getRemoteAddress(), newAddress
              });

      channel.setRemoteAddress(newAddress);
      return;
    }

    if (handler != null) {
      PacketWrapper packet = (PacketWrapper) msg;
      boolean sendPacket = handler.shouldHandle(packet);
      try {
        if (sendPacket && packet.packet != null) {
          try {
            packet.packet.handle(handler);
          } catch (CancelSendSignal ex) {
            sendPacket = false;
          }
        }
        if (sendPacket) {
          handler.handle(packet);
        }
      } finally {
        packet.trySingleRelease();
      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {

    ctx.close();

    final BungeeCord bungee = BungeeCord.getInstance();
        /*if (cause instanceof FastException || (cause.getMessage() != null && (cause.getMessage().contains("Could not read")
        || cause.getMessage().contains("Did not read")))) {
            bungee.getLogger()
                .log(Level.WARNING, "[Aegis] {0} - exploit detected? {1}",
                    new Object[]{handler, cause.getMessage()});

            final Aegis aegis = bungee.getAegis();
            final Blacklist blacklist = aegis.getBlacklist();
            if (blacklist != null) {
                blacklist
                    .asyncDrop(((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress());
            }
        }*/

    if (cause instanceof FastException || (cause.getMessage() != null && cause.getMessage()
        .contains("[Exploit]"))) {

      final InetSocketAddress address = (InetSocketAddress) ctx.channel()
          .remoteAddress();
      final String hostAddress = address.getAddress().getHostAddress();
      final Aegis aegis = bungee.getAegis();
      final Blacklist blacklist = aegis.getBlacklist();
      if (blacklist != null) {
        blacklist
            .asyncDrop(hostAddress);
      }

      //AddressBlocker.block(hostAddress);

      if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
        bungee.getLogger()
            .log(Level.WARNING, ColouredWriter.ANSI_RED +
                "[Aegis] " + ColouredWriter.ANSI_RESET + cause.getMessage() + " (blocked: "
                + hostAddress + ")");
      }

      return;
    } else if (Settings.IMP.AEGIS_SETTINGS.PRINT_EXCEPTIONS) {
      if (cause.getMessage() != null && !(cause instanceof IOException)) {
        bungee.getLogger()
            .log(Level.WARNING,
                "[Aegis] {0} - Exception caught! {1} (To see details enable print-stacktraces)",
                new Object[]{handler, cause.getMessage()});
        if (Settings.IMP.AEGIS_SETTINGS.PRINT_STACKTRACES_FROM_EXCEPTIONS) {
          cause.printStackTrace();
        }
      }
    }

        /*if ( ctx.channel().isActive() )
        {
            boolean logExceptions = !( handler instanceof PingHandler );

            if ( logExceptions )
            {
                if (cause instanceof FastException) {
                    ProxyServer.getInstance().getLogger().log( Level.WARNING, "[Aegis] {0} - probably tried to use crash packet", handler );
                }
                else if ( cause instanceof ReadTimeoutException )
                {
                    ProxyServer.getInstance().getLogger().log( Level.WARNING, "{0} - read timed out", handler );
                } else if ( cause instanceof DecoderException && cause.getCause() instanceof BadPacketException )
                {
                    ProxyServer.getInstance().getLogger().log( Level.WARNING, "{0} - bad packet ID, are mods in use!? {1}", new Object[]
                    {
                        handler, cause.getCause().getMessage()
                    } );
                } else if ( cause instanceof DecoderException && cause.getCause() instanceof OverflowPacketException )
                {
                    ProxyServer.getInstance().getLogger().log( Level.WARNING, "{0} - overflow in packet detected! {1}", new Object[]
                    {
                        handler, cause.getCause().getMessage()
                    } );
                } else if ( cause instanceof IOException || ( cause instanceof IllegalStateException && handler instanceof InitialHandler ) )
                {
                    ProxyServer.getInstance().getLogger().log( Level.WARNING, "{0} - {1}: {2}", new Object[]
                    {
                        handler, cause.getClass().getSimpleName(), cause.getMessage()
                    } );
                } else if ( cause instanceof QuietException )
                {
                    ProxyServer.getInstance().getLogger().log( Level.SEVERE, "{0} - encountered exception: {1}", new Object[]
                    {
                        handler, cause
                    } );
                } else
                {
                    ProxyServer.getInstance().getLogger().log( Level.SEVERE, handler + " - encountered exception", cause );
                }
            }

            if ( handler != null )
            {
                try
                {
                    handler.exception( cause );
                } catch ( Exception ex )
                {
                    ProxyServer.getInstance().getLogger().log( Level.SEVERE, handler + " - exception processing exception", ex );
                }
            }

            ctx.close();
        }*/
  }
}
