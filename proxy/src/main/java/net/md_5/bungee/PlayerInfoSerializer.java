package net.md_5.bungee;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.UUID;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.protocol.ProtocolConstants;

public class PlayerInfoSerializer implements JsonSerializer<ServerPing.PlayerInfo>,
    JsonDeserializer<ServerPing.PlayerInfo> {

  private final int protocol;

  public PlayerInfoSerializer() {
    this.protocol = 5;
  }

  public PlayerInfoSerializer(final int protocol) {
    this.protocol = protocol;
  }

  @Override
  public ServerPing.PlayerInfo deserialize(final JsonElement json, final Type typeOfT,
      final JsonDeserializationContext context) throws JsonParseException {
    final JsonObject js = json.getAsJsonObject();
    final ServerPing.PlayerInfo info = new ServerPing.PlayerInfo(js.get("name").getAsString(),
        (UUID) null);
    final String id = js.get("id").getAsString();
    if (ProtocolConstants.isBeforeOrEq(this.protocol, 4) || !id.contains("-")) {
      info.setId(id);
    } else {
      info.setUniqueId(UUID.fromString(id));
    }
    return info;
  }

  @Override
  public JsonElement serialize(final ServerPing.PlayerInfo src, final Type typeOfSrc,
      final JsonSerializationContext context) {
    final JsonObject out = new JsonObject();
    out.addProperty("name", src.getName());
    if (ProtocolConstants.isBeforeOrEq(this.protocol, 4)) {
      out.addProperty("id", src.getId());
    } else {
      out.addProperty("id", src.getUniqueId().toString());
    }
    return out;
  }
}
