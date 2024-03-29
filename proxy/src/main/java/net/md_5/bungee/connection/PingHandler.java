package net.md_5.bungee.connection;

import com.google.gson.Gson;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.BungeeServerInfo;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.protocol.FastException;
import net.md_5.bungee.protocol.MinecraftDecoder;
import net.md_5.bungee.protocol.MinecraftEncoder;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.Handshake;
import net.md_5.bungee.protocol.packet.StatusRequest;
import net.md_5.bungee.protocol.packet.StatusResponse;
import net.md_5.bungee.util.BufUtil;

@RequiredArgsConstructor
public class PingHandler extends PacketHandler {

  private static ServerPing cachedPing;
  private static long cachedPingTime = -1;
  private final ServerInfo target;
  private final Callback<ServerPing> callback;
  private final int protocol;
  private ChannelWrapper channel;

  @Override
  public void connected(ChannelWrapper channel) throws Exception {
    this.channel = channel;
    MinecraftEncoder encoder = new MinecraftEncoder(Protocol.HANDSHAKE, false, protocol);

    channel.getHandle().pipeline()
        .addAfter(PipelineUtils.FRAME_DECODER, PipelineUtils.PACKET_DECODER,
            new MinecraftDecoder(Protocol.STATUS, false,
                ProxyServer.getInstance().getProtocolVersion(), false));
    channel.getHandle().pipeline()
        .addAfter(PipelineUtils.FRAME_PREPENDER, PipelineUtils.PACKET_ENCODER, encoder);

    channel.write(
        new Handshake(protocol, target.getAddress().getHostString(), target.getAddress().getPort(),
            1));

    encoder.setProtocol(Protocol.STATUS);
    channel.write(new StatusRequest());
  }

  @Override
  public void exception(Throwable t) throws Exception {
    callback.done(null, t);
  }

  @Override
  public void handle(PacketWrapper packet) throws Exception {
    if (packet == null || packet.packet == null) {
      channel.close();
      throw new FastException(
          "Unexpected packet received during ping process! " + BufUtil.dump(packet.buf, 16));
    }
  }

  @Override
  @SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
  public void handle(StatusResponse statusResponse) throws Exception {

    final ServerPing ping;
    if (System.currentTimeMillis() - cachedPingTime > 4000 || cachedPing == null) {
      final Gson gson =
          protocol == ProtocolConstants.MINECRAFT_1_7_2 ? BungeeCord.getInstance().gsonLegacy
              : BungeeCord.getInstance().gson; // Travertine

      ping = gson.fromJson(statusResponse.getResponse(), ServerPing.class);
      cachedPing = ping;
      cachedPingTime = System.currentTimeMillis() + 4000L;
      // System.out.println("[Aegis] Using nonCachedPing!");
    } else {
      ping = cachedPing;
      // System.out.println("[Aegis] Using cachedPing!");
    }

    //ServerPing serverPing = gson.fromJson( statusResponse.getResponse(), ServerPing.class );
    ((BungeeServerInfo) target).cachePing(ping);
    callback.done(ping, null);

    //callback.done( gson.fromJson( statusResponse.getResponse(), ServerPing.class ), null );
    channel.close();
  }

  @Override
  public String toString() {
    return "[Ping Handler] -> " + target.getName();
  }
}
