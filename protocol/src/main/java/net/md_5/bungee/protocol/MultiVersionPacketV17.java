package net.md_5.bungee.protocol;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;

public abstract class MultiVersionPacketV17 extends DefinedPacket {

  protected void v17Read(final ByteBuf buf, final ProtocolConstants.Direction direction, final int protocolVersion) {
    this.v17Read(buf);
  }

  @Override
  public void read0(final ByteBuf buf, final ProtocolConstants.Direction direction, final int protocolVersion) {
    switch (protocolVersion) {
      case 4:
      case 5:
        this.v17Read(buf, direction, protocolVersion);
        break;
      default:
        this.read(buf, direction, protocolVersion);
        break;
    }
  }

  protected void v17Write(final ByteBuf buf, final ProtocolConstants.Direction direction, final int protocolVersion) {
    this.v17Write(buf);
  }

  @Override
  public void write0(final ByteBuf buf, final ProtocolConstants.Direction direction, final int protocolVersion) {
    switch (protocolVersion) {
      case 4:
      case 5:
        this.v17Write(buf, direction, protocolVersion);
        break;
      default:
        this.write(buf, direction, protocolVersion);
        break;
    }
  }

  protected void v17Read(final ByteBuf buf) {
    throw new UnsupportedOperationException("Packet must implement read method");
  }

  protected void v17Write(final ByteBuf buf) {
    throw new UnsupportedOperationException("Packet must implement write method");
  }

  public static void v17writeArray(final byte[] b, final ByteBuf buf, final boolean allowExtended) {
    if (allowExtended) {
      Preconditions.checkArgument(b.length <= 2097050, "Cannot send array longer than 2097050 (got %s bytes)", b.length);
    }
    else {
      Preconditions.checkArgument(b.length <= 32767, "Cannot send array longer than Short.MAX_VALUE (got %s bytes)", b.length);
    }
    DefinedPacket.writeVarShort(buf, b.length);
    buf.writeBytes(b);
  }

  public static byte[] v17readArray(final ByteBuf buf) {
    final int len = DefinedPacket.readVarShort(buf);
    Preconditions.checkArgument(len <= 2097050, "Cannot receive array longer than 2097050 (got %s bytes)", len);
    final byte[] ret = new byte[len];
    buf.readBytes(ret);
    return ret;
  }

}
