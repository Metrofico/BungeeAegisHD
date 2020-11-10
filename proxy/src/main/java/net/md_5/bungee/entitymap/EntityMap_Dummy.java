package net.md_5.bungee.entitymap;

import io.netty.buffer.ByteBuf;

public class EntityMap_Dummy extends EntityMap
{
  public static final EntityMap_Dummy INSTANCE;

  EntityMap_Dummy() {
    super();
  }

  @Override
  public void rewriteServerbound(final ByteBuf packet, final int oldId, final int newId) {
  }

  @Override
  public void rewriteServerbound(final ByteBuf packet, final int oldId, final int newId, final int protocolVersion) {
  }

  @Override
  public void rewriteClientbound(final ByteBuf packet, final int oldId, final int newId) {
  }

  @Override
  public void rewriteClientbound(final ByteBuf packet, final int oldId, final int newId, final int protocolVersion) {
  }

  static {
    INSTANCE = new EntityMap_Dummy();
  }
}
