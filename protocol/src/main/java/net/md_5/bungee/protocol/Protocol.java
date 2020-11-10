package net.md_5.bungee.protocol;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import gnu.trove.impl.Constants;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import lombok.Data;
import lombok.Getter;
import net.md_5.bungee.protocol.packet.BossBar;
import net.md_5.bungee.protocol.packet.Chat;
import net.md_5.bungee.protocol.packet.ClientSettings;
import net.md_5.bungee.protocol.packet.Commands;
import net.md_5.bungee.protocol.packet.EncryptionRequest;
import net.md_5.bungee.protocol.packet.EncryptionResponse;
import net.md_5.bungee.protocol.packet.EntityEffect;
import net.md_5.bungee.protocol.packet.EntityRemoveEffect;
import net.md_5.bungee.protocol.packet.EntityStatus;
import net.md_5.bungee.protocol.packet.GameState;
import net.md_5.bungee.protocol.packet.Handshake;
import net.md_5.bungee.protocol.packet.KeepAlive;
import net.md_5.bungee.protocol.packet.Kick;
import net.md_5.bungee.protocol.packet.Login;
import net.md_5.bungee.protocol.packet.LoginPayloadRequest;
import net.md_5.bungee.protocol.packet.LoginPayloadResponse;
import net.md_5.bungee.protocol.packet.LoginRequest;
import net.md_5.bungee.protocol.packet.LoginSuccess;
import net.md_5.bungee.protocol.packet.PingPacket;
import net.md_5.bungee.protocol.packet.PlayerListHeaderFooter;
import net.md_5.bungee.protocol.packet.PlayerListItem;
import net.md_5.bungee.protocol.packet.PluginMessage;
import net.md_5.bungee.protocol.packet.Respawn;
import net.md_5.bungee.protocol.packet.ScoreboardDisplay;
import net.md_5.bungee.protocol.packet.ScoreboardObjective;
import net.md_5.bungee.protocol.packet.ScoreboardScore;
import net.md_5.bungee.protocol.packet.SetCompression;
import net.md_5.bungee.protocol.packet.StatusRequest;
import net.md_5.bungee.protocol.packet.StatusResponse;
import net.md_5.bungee.protocol.packet.TabCompleteRequest;
import net.md_5.bungee.protocol.packet.TabCompleteResponse;
import net.md_5.bungee.protocol.packet.Team;
import net.md_5.bungee.protocol.packet.Title;
import net.md_5.bungee.protocol.packet.ViewDistance;
import xyz.yooniks.aegis.packets.EmptyChunkPacket;
import xyz.yooniks.aegis.packets.JoinGame;
import xyz.yooniks.aegis.packets.Player;
import xyz.yooniks.aegis.packets.PlayerAbilities;
import xyz.yooniks.aegis.packets.PlayerPosition;
import xyz.yooniks.aegis.packets.PlayerPositionAndLook;
import xyz.yooniks.aegis.packets.SetExp;
import xyz.yooniks.aegis.packets.SetSlot;
import xyz.yooniks.aegis.packets.TeleportConfirm;
import xyz.yooniks.aegis.packets.TimeUpdate;

public enum Protocol
{
  HANDSHAKE {
    {
      this.TO_SERVER.<DefinedPacket>registerPacket(Handshake.class, (Supplier<DefinedPacket>)(Supplier)Handshake::new, map(4, 0));
    }
  },
  GAME {
    {
      this.TO_CLIENT.<DefinedPacket>registerPacket(KeepAlive.class, (Supplier<DefinedPacket>)(Supplier)KeepAlive::new,
          map(4, 0), map(107, 31), map(393, 33), map(477, 32), map(573, 33), map(735, 32),
          map( ProtocolConstants.MINECRAFT_1_16_2, 0x1F ));
      this.TO_CLIENT.<DefinedPacket>registerPacket(Login.class, (Supplier<DefinedPacket>)(Supplier)Login::new, map(4, 1), map(107, 35),
          map(393, 37), map(573, 38), map(735, 37),
          map( ProtocolConstants.MINECRAFT_1_16_2, 0x24 ));
      this.TO_CLIENT.<DefinedPacket>registerPacket(Chat.class, (Supplier<DefinedPacket>)(Supplier)Chat::new, map(4, 2), map(107, 15),
          map(393, 14), map(573, 15), map(735, 14));
      this.TO_CLIENT.<DefinedPacket>registerPacket(Respawn.class, (Supplier<DefinedPacket>)(Supplier)Respawn::new, map(4, 7), map(107, 51),
          map(335, 52), map(338, 53), map(393, 56), map(477, 58), map(573, 59), map(735, 58)
          , map( ProtocolConstants.MINECRAFT_1_16_2, 0x39 ));
      this.TO_CLIENT.<DefinedPacket>registerPacket(BossBar.class, (Supplier<DefinedPacket>)(Supplier)BossBar::new, map(107, 12), map(573, 13), map(735, 12));
      this.TO_CLIENT.<DefinedPacket>registerPacket(EntityEffect.class, (Supplier<DefinedPacket>)(Supplier)EntityEffect::new, map(4, 29), map(107, 76), map(110, 75), map(335, 78), map(338, 79), map(393, 83), map(477, 89), map(573, 90), map(735, 89));

      this.TO_CLIENT.<DefinedPacket>registerPacket(EntityRemoveEffect.class, (Supplier<DefinedPacket>)(Supplier)EntityRemoveEffect::new, map(4, 30),
          map(107, 49), map(335, 50), map(338, 51), map(393, 54), map(477, 56), map(573, 57), map(735, 56), map(751, 55));

      this.TO_CLIENT.<DefinedPacket>registerPacket((Class<DefinedPacket>)(Class)PlayerListItem.class, map(4, 56), map(107, 45), map(338, 46), map(393, 48), map(477, 51), map(573, 52), map(735, 51), map(751, 50));

      this.TO_CLIENT.<DefinedPacket>registerPacket(TabCompleteResponse.class, (Supplier<DefinedPacket>)(Supplier)TabCompleteResponse::new,
          map(4, 58), map(107, 14), map(393, 16), map(573, 17), map(735, 16),
          map( ProtocolConstants.MINECRAFT_1_16_2, 0x0F ));
      this.TO_CLIENT.<DefinedPacket>registerPacket(ScoreboardObjective.class, (Supplier<DefinedPacket>)(Supplier)ScoreboardObjective::new, map(4, 59), map(107, 63), map(335, 65), map(338, 66), map(393, 69), map(477, 73), map(573, 74));
      this.TO_CLIENT.<DefinedPacket>registerPacket(ScoreboardScore.class, (Supplier<DefinedPacket>)(Supplier)ScoreboardScore::new, map(4, 60), map(107, 66), map(335, 68), map(338, 69), map(393, 72), map(477, 76), map(573, 77));
      this.TO_CLIENT.<DefinedPacket>registerPacket(ScoreboardDisplay.class, (Supplier<DefinedPacket>)(Supplier)ScoreboardDisplay::new, map(4, 61), map(107, 56), map(335, 58), map(338, 59), map(393, 62), map(477, 66), map(573, 67));
      this.TO_CLIENT.<DefinedPacket>registerPacket(Team.class, (Supplier<DefinedPacket>)(Supplier)Team::new, map(4, 62), map(107, 65), map(335, 67), map(338, 68), map(393, 71), map(477, 75), map(573, 76));
      this.TO_CLIENT.<DefinedPacket>registerPacket(PluginMessage.class, (Supplier<DefinedPacket>)(Supplier)PluginMessage::new,
          map(4, 63), map(107, 24), map(393, 25),
          map(477, 24), map(573, 25), map(735, 24),
          map( ProtocolConstants.MINECRAFT_1_16_2, 0x17 ));
      this.TO_CLIENT.<DefinedPacket>registerPacket(Kick.class, (Supplier<DefinedPacket>)(Supplier)Kick::new,
          map(4, 64), map(107, 26), map(393, 27), map(477, 26), map(573, 27), map(735, 26),                    map( ProtocolConstants.MINECRAFT_1_16_2, 0x19 ));
      this.TO_CLIENT.<DefinedPacket>registerPacket(Title.class, (Supplier<DefinedPacket>)(Supplier)Title::new, map(4, 69), map(335, 71), map(338, 72), map(393, 75), map(477, 79), map(573, 80), map(735, 79));
      this.TO_CLIENT.<DefinedPacket>registerPacket(PlayerListHeaderFooter.class, (Supplier<DefinedPacket>)(Supplier)PlayerListHeaderFooter::new, map(4, 71), map(107, 72), map(110, 71), map(335, 73), map(338, 74), map(393, 78), map(477, 83), map(573, 84), map(735, 83));
      this.TO_CLIENT.<DefinedPacket>registerPacket(EntityStatus.class, (Supplier<DefinedPacket>)(Supplier)EntityStatus::new, map(4, 26), map(107, 27), map(393, 28),
          map(477, 27), map(573, 28), map(735, 27),                    map( ProtocolConstants.MINECRAFT_1_16_2, 0x1A ));
      this.TO_CLIENT.<DefinedPacket>registerPacket(Commands.class, (Supplier<DefinedPacket>)(Supplier)Commands::new,
          map(393, 17), map(573, 18), map(735, 17),                    map( ProtocolConstants.MINECRAFT_1_16_2, 0x10 ));
      this.TO_CLIENT.<DefinedPacket>registerPacket(GameState.class, (Supplier<DefinedPacket>)(Supplier)GameState::new, map(573, 31), map(735, 30), map(751, 29));
      this.TO_CLIENT.<DefinedPacket>registerPacket(ViewDistance.class, (Supplier<DefinedPacket>)(Supplier)ViewDistance::new, map(477, 65), map(573, 66), map(735, 65));
      this.TO_SERVER.<DefinedPacket>registerPacket(KeepAlive.class, (Supplier<DefinedPacket>)(Supplier)KeepAlive::new, map(4, 0), map(107, 11), map(335, 12), map(338, 11), map(393, 14), map(477, 15), map(735, 16));
      this.TO_SERVER.<DefinedPacket>registerPacket(Chat.class, (Supplier<DefinedPacket>)(Supplier)Chat::new, map(4, 1), map(107, 2), map(335, 3), map(338, 2), map(477, 3));
      this.TO_SERVER.<DefinedPacket>registerPacket(TabCompleteRequest.class, (Supplier<DefinedPacket>)(Supplier)TabCompleteRequest::new, map(4, 20), map(107, 1), map(335, 2), map(338, 1), map(393, 5), map(477, 6));
      this.TO_SERVER.<DefinedPacket>registerPacket(ClientSettings.class, (Supplier<DefinedPacket>)(Supplier)ClientSettings::new, map(4, 21), map(107, 4), map(335, 5), map(338, 4), map(477, 5));
      this.TO_SERVER.<DefinedPacket>registerPacket(PluginMessage.class, (Supplier<DefinedPacket>)(Supplier)PluginMessage::new, map(4, 23), map(107, 9), map(335, 10), map(338, 9), map(393, 10), map(477, 11));
    }
  },
  STATUS {
    {
      this.TO_CLIENT.<DefinedPacket>registerPacket(StatusResponse.class, (Supplier<DefinedPacket>)(Supplier)StatusResponse::new, map(4, 0));
      this.TO_CLIENT.<DefinedPacket>registerPacket(PingPacket.class, (Supplier<DefinedPacket>)(Supplier)PingPacket::new, map(4, 1));
      this.TO_SERVER.<DefinedPacket>registerPacket(StatusRequest.class, (Supplier<DefinedPacket>)(Supplier)StatusRequest::new, map(4, 0));
      this.TO_SERVER.<DefinedPacket>registerPacket(PingPacket.class, (Supplier<DefinedPacket>)(Supplier)PingPacket::new, map(4, 1));
    }
  },
  LOGIN {
    {
      this.TO_CLIENT.<DefinedPacket>registerPacket(Kick.class, (Supplier<DefinedPacket>)(Supplier)Kick::new, map(4, 0));
      this.TO_CLIENT.<DefinedPacket>registerPacket(EncryptionRequest.class, (Supplier<DefinedPacket>)(Supplier)EncryptionRequest::new, map(4, 1));
      this.TO_CLIENT.<DefinedPacket>registerPacket(LoginSuccess.class, (Supplier<DefinedPacket>)(Supplier)LoginSuccess::new, map(4, 2));
      this.TO_CLIENT.<DefinedPacket>registerPacket(SetCompression.class, (Supplier<DefinedPacket>)(Supplier)SetCompression::new, map(4, 3));
      this.TO_CLIENT.<DefinedPacket>registerPacket(LoginPayloadRequest.class, (Supplier<DefinedPacket>)(Supplier)LoginPayloadRequest::new, map(393, 4));
      this.TO_SERVER.<DefinedPacket>registerPacket(LoginRequest.class, (Supplier<DefinedPacket>)(Supplier)LoginRequest::new, map(4, 0));
      this.TO_SERVER.<DefinedPacket>registerPacket(EncryptionResponse.class, (Supplier<DefinedPacket>)(Supplier)EncryptionResponse::new, map(4, 1));
      this.TO_SERVER.<DefinedPacket>registerPacket(LoginPayloadResponse.class, (Supplier<DefinedPacket>)(Supplier)LoginPayloadResponse::new, map(393, 2));
    }
  },
  //Custom
  BotFilter
      {

        {
          TO_CLIENT.registerPacket(
              JoinGame.class, JoinGame::new, //Renamed Login
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x01 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x23 ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x25 ),
              map( ProtocolConstants.MINECRAFT_1_15, 0x26 ),
              map( ProtocolConstants.MINECRAFT_1_16, 0x25 ),
              map( ProtocolConstants.MINECRAFT_1_16_2, 0x24 )
          );
          TO_CLIENT.registerPacket(
              TimeUpdate.class, TimeUpdate::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x03 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x44 ),
              map( ProtocolConstants.MINECRAFT_1_12, 0x46 ),
              map( ProtocolConstants.MINECRAFT_1_12_1, 0x47 ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x4A ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x4E ),
              map( ProtocolConstants.MINECRAFT_1_15, 0x4F ),
              map( ProtocolConstants.MINECRAFT_1_16, 0x4E )
          );
          TO_CLIENT.registerPacket(
              PlayerPositionAndLook.class, PlayerPositionAndLook::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x08 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x2E ),
              map( ProtocolConstants.MINECRAFT_1_12_1, 0x2F ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x32 ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x35 ),
              map( ProtocolConstants.MINECRAFT_1_15, 0x36 ),
              map( ProtocolConstants.MINECRAFT_1_16, 0x35 ),
              map( ProtocolConstants.MINECRAFT_1_16_2, 0x34 )
          );
          TO_CLIENT.registerPacket(
              EmptyChunkPacket.class, EmptyChunkPacket::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x21 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x20 ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x22 ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x21 ),
              map( ProtocolConstants.MINECRAFT_1_15, 0x22 ),
              map( ProtocolConstants.MINECRAFT_1_16, 0x21 ),
              map( ProtocolConstants.MINECRAFT_1_16_2, 0x20 )
          );
          TO_CLIENT.registerPacket(
              SetSlot.class, SetSlot::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x2F ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x16 ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x17 ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x16 ),
              map( ProtocolConstants.MINECRAFT_1_15, 0x17 ),
              map( ProtocolConstants.MINECRAFT_1_16, 0x16 ),
              map( ProtocolConstants.MINECRAFT_1_16_2, 0x15 )
          );
          TO_CLIENT.registerPacket(
              PlayerAbilities.class, PlayerAbilities::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x39 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x2B ),
              map( ProtocolConstants.MINECRAFT_1_12_1, 0x2C ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x2E ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x31 ),
              map( ProtocolConstants.MINECRAFT_1_15, 0x32 ),
              map( ProtocolConstants.MINECRAFT_1_16, 0x31 ),
              map( ProtocolConstants.MINECRAFT_1_16_2, 0x30 )
          );
          TO_CLIENT.registerPacket(
              SetExp.class, SetExp::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x1F ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x3D ),
              map( ProtocolConstants.MINECRAFT_1_12, 0x3F ),
              map( ProtocolConstants.MINECRAFT_1_12_1, 0x40 ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x43 ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x47 ),
              map( ProtocolConstants.MINECRAFT_1_15, 0x48 )
          );
          TO_SERVER.registerPacket(
              ClientSettings.class, ClientSettings::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x15 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x04 ),
              map( ProtocolConstants.MINECRAFT_1_12, 0x05 ),
              map( ProtocolConstants.MINECRAFT_1_12_1, 0x04 ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x05 )
          );
          TO_SERVER.registerPacket(
              TeleportConfirm.class, TeleportConfirm::new,
              map( ProtocolConstants.MINECRAFT_1_9, 0x00 )
          );
          TO_SERVER.registerPacket(
              PlayerPositionAndLook.class, PlayerPositionAndLook::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x06 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x0D ),
              map( ProtocolConstants.MINECRAFT_1_12, 0x0F ),
              map( ProtocolConstants.MINECRAFT_1_12_1, 0x0E ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x11 ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x12 ),
              map( ProtocolConstants.MINECRAFT_1_16, 0x13 )
          );
          TO_SERVER.registerPacket(
              PlayerPosition.class, PlayerPosition::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x04 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x0C ),
              map( ProtocolConstants.MINECRAFT_1_12, 0x0E ),
              map( ProtocolConstants.MINECRAFT_1_12_1, 0x0D ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x10 ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x11 ),
              map( ProtocolConstants.MINECRAFT_1_16, 0x12 )
          );
          TO_SERVER.registerPacket(
              Player.class, Player::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x03 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x0F ),
              map( ProtocolConstants.MINECRAFT_1_12, 0x0D ),
              map( ProtocolConstants.MINECRAFT_1_12_1, 0x0C ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x0F ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x14 ),
              map( ProtocolConstants.MINECRAFT_1_16, 0x15 )
          );
          TO_SERVER.registerPacket(
              KeepAlive.class, KeepAlive::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x00 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x0B ),
              map( ProtocolConstants.MINECRAFT_1_12, 0x0C ),
              map( ProtocolConstants.MINECRAFT_1_12_1, 0x0B ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x0E ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x0F ),
              map( ProtocolConstants.MINECRAFT_1_16, 0x10 )
          );
          TO_SERVER.registerPacket(
              Chat.class, Chat::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x01 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x02 ),
              map( ProtocolConstants.MINECRAFT_1_12, 0x03 ),
              map( ProtocolConstants.MINECRAFT_1_12_1, 0x02 ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x03 )
          );
          TO_SERVER.registerPacket(
              PluginMessage.class, PluginMessage::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x17 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x09 ),
              map( ProtocolConstants.MINECRAFT_1_12, 0x0A ),
              map( ProtocolConstants.MINECRAFT_1_12_1, 0x09 ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x0A ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x0B )
          );
          /*TO_CLIENT.registerPacket(
              JoinGame.class, JoinGame::new, //Renamed Login
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x01 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x23 ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x25 ),
              map( ProtocolConstants.MINECRAFT_1_15, 0x26 ),
              map( ProtocolConstants.MINECRAFT_1_16, 0x25 )
          );

          TO_CLIENT.registerPacket(
              TimeUpdate.class, TimeUpdate::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x03 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x44 ),
              map( ProtocolConstants.MINECRAFT_1_12, 0x46 ),
              map( ProtocolConstants.MINECRAFT_1_12_1, 0x47 ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x4A ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x4E ),
              map( ProtocolConstants.MINECRAFT_1_15, 0x4F )
          );
          TO_CLIENT.registerPacket(
              PlayerPositionAndLook.class, PlayerPositionAndLook::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x08 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x2E ),
              map( ProtocolConstants.MINECRAFT_1_12_1, 0x2F ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x32 ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x35 ),
              map( ProtocolConstants.MINECRAFT_1_15, 0x36 )
          );
          TO_CLIENT.registerPacket(
              EmptyChunkPacket.class, EmptyChunkPacket::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x21 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x20 ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x22 ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x21 ),
              map( ProtocolConstants.MINECRAFT_1_15, 0x22 )
          );
          TO_CLIENT.registerPacket(
              SetSlot.class, SetSlot::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x2F ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x16 ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x17 ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x16 ),
              map( ProtocolConstants.MINECRAFT_1_15, 0x17 )
          );
          TO_CLIENT.registerPacket(
              PlayerAbilities.class, PlayerAbilities::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x39 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x2B ),
              map( ProtocolConstants.MINECRAFT_1_12_1, 0x2C ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x2E ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x31 ),
              map( ProtocolConstants.MINECRAFT_1_15, 0x32 )
          );
          TO_CLIENT.registerPacket(
              SetExp.class, SetExp::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x1F ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x3D ),
              map( ProtocolConstants.MINECRAFT_1_12, 0x3F ),
              map( ProtocolConstants.MINECRAFT_1_12_1, 0x40 ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x43 ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x47 ),
              map( ProtocolConstants.MINECRAFT_1_15, 0x48 )
          );
          TO_SERVER.registerPacket(
              ClientSettings.class, ClientSettings::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x15 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x04 ),
              map( ProtocolConstants.MINECRAFT_1_12, 0x05 ),
              map( ProtocolConstants.MINECRAFT_1_12_1, 0x04 ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x05 )
          );
          TO_SERVER.registerPacket(
              TeleportConfirm.class, TeleportConfirm::new,
              map( ProtocolConstants.MINECRAFT_1_9, 0x00 )
          );
          TO_SERVER.registerPacket(
              PlayerPositionAndLook.class, PlayerPositionAndLook::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x06 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x0D ),
              map( ProtocolConstants.MINECRAFT_1_12, 0x0F ),
              map( ProtocolConstants.MINECRAFT_1_12_1, 0x0E ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x11 ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x12 )
          );
          TO_SERVER.registerPacket(
              PlayerPosition.class, PlayerPosition::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x04 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x0C ),
              map( ProtocolConstants.MINECRAFT_1_12, 0x0E ),
              map( ProtocolConstants.MINECRAFT_1_12_1, 0x0D ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x10 ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x11 )
          );
          TO_SERVER.registerPacket(
              Player.class, Player::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x03 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x0F ),
              map( ProtocolConstants.MINECRAFT_1_12, 0x0D ),
              map( ProtocolConstants.MINECRAFT_1_12_1, 0x0C ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x0F ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x14 )
          );
          TO_SERVER.registerPacket(
              KeepAlive.class, KeepAlive::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x00 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x0B ),
              map( ProtocolConstants.MINECRAFT_1_12, 0x0C ),
              map( ProtocolConstants.MINECRAFT_1_12_1, 0x0B ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x0E ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x0F )
          );
          TO_SERVER.registerPacket(
              Chat.class, Chat::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x01 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x02 ),
              map( ProtocolConstants.MINECRAFT_1_12, 0x03 ),
              map( ProtocolConstants.MINECRAFT_1_12_1, 0x02 ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x03 )
          );
          TO_SERVER.registerPacket(
              PluginMessage.class, PluginMessage::new,
              map( ProtocolConstants.MINECRAFT_1_7_2, 0x17 ),
              map( ProtocolConstants.MINECRAFT_1_9, 0x09 ),
              map( ProtocolConstants.MINECRAFT_1_12, 0x0A ),
              map( ProtocolConstants.MINECRAFT_1_12_1, 0x09 ),
              map( ProtocolConstants.MINECRAFT_1_13, 0x0A ),
              map( ProtocolConstants.MINECRAFT_1_14, 0x0B )
          );*/
        }
      };

    /*========================================================================*/
    public static final int MAX_PACKET_ID = 0xFF;
    /*========================================================================*/
    public final DirectionData TO_SERVER = new DirectionData( this, ProtocolConstants.Direction.TO_SERVER );
    public final DirectionData TO_CLIENT = new DirectionData( this, ProtocolConstants.Direction.TO_CLIENT );

    public static void main(String[] args)
    {
        for ( int version : ProtocolConstants.SUPPORTED_VERSION_IDS )
        {
            dump( version );
        }
    }

    private static void dump(int version)
    {
        for ( Protocol protocol : Protocol.values() )
        {
            dump( version, protocol );
        }
    }

    private static void dump(int version, Protocol protocol)
    {
        dump( version, protocol.TO_CLIENT );
        dump( version, protocol.TO_SERVER );
    }

    private static void dump(int version, DirectionData data)
    {
        for ( int id = 0; id < MAX_PACKET_ID; id++ )
        {
            DefinedPacket packet = data.createPacket( id, version );
            if ( packet != null )
            {
                System.out.println( version + " " + data.protocolPhase + " " + data.direction + " " + id + " " + packet.getClass().getSimpleName() );
            }
        }
    }

  @Data
  private static class ProtocolData
  {

    private final int protocolVersion;
    private final TObjectIntMap<Class<? extends DefinedPacket>> packetMap = new TObjectIntHashMap<>( MAX_PACKET_ID, Constants.DEFAULT_LOAD_FACTOR, -1 );
    private final Supplier<? extends DefinedPacket>[] packetConstructors = new Supplier[ MAX_PACKET_ID ]; //BotFilter
  }

  @Data
  private static class ProtocolMapping
  {

    private final int protocolVersion;
    private final int packetID;
  }

  // Helper method
  private static ProtocolMapping map(int protocol, int id)
  {
    return new ProtocolMapping( protocol, id );
  }

  public static final class DirectionData //BotFilter -> public
  {

    private final TIntObjectMap<ProtocolData> protocols = new TIntObjectHashMap<>();
    //
    private final Protocol protocolPhase;
    @Getter
    private final ProtocolConstants.Direction direction;

    public DirectionData(Protocol protocolPhase, ProtocolConstants.Direction direction)
    {
      this.protocolPhase = protocolPhase;
      this.direction = direction;

      for ( int protocol : ProtocolConstants.SUPPORTED_VERSION_IDS )
      {
        protocols.put( protocol, new ProtocolData( protocol ) );
      }
    }

    private ProtocolData getProtocolData(int version)
    {
      ProtocolData protocol = protocols.get( version );
      if ( protocol == null && ( protocolPhase != Protocol.GAME ) )
      {
        protocol = Iterables.getFirst( protocols.valueCollection(), null );
      }
      return protocol;
    }

    public final DefinedPacket createPacket(int id, int version)
    {
      ProtocolData protocolData = getProtocolData( version );
      if ( protocolData == null )
      {
        throw new FastException( "Unsupported protocol version " + version );
      }
      if ( id > MAX_PACKET_ID )
      {
        throw new FastException( "Packet with id " + id + " outside of range " );
      }

      Supplier<? extends DefinedPacket> constructor = protocolData.packetConstructors[id]; //BotFilter
      return ( constructor == null ) ? null : constructor.get(); //Aegis
    }

    private void registerPacket(Class<? extends DefinedPacket> packetClass, ProtocolMapping... mappings) //BotFilter
    {
      registerPacket( packetClass, MetaFactoryUtils.createNoArgsConstructorUnchecked( packetClass ), mappings ); //BotFilter
    }

    private <P extends DefinedPacket> void registerPacket(Class<? extends DefinedPacket> packetClass, Supplier<P> packetSupplier, ProtocolMapping... mappings) //BotFilter
    {
      int mappingIndex = 0;
      ProtocolMapping mapping = mappings[mappingIndex];
      for ( int protocol : ProtocolConstants.SUPPORTED_VERSION_IDS )
      {
        if ( protocol < mapping.protocolVersion )
        {
          // This is a new packet, skip it till we reach the next protocol
          continue;
        }

        if ( mapping.protocolVersion < protocol && mappingIndex + 1 < mappings.length )
        {
          // Mapping is non current, but the next one may be ok
          ProtocolMapping nextMapping = mappings[mappingIndex + 1];
          if ( nextMapping.protocolVersion == protocol )
          {
            Preconditions.checkState( nextMapping.packetID != mapping.packetID, "Duplicate packet mapping (%s, %s)", mapping.protocolVersion, nextMapping.protocolVersion );

            mapping = nextMapping;
            mappingIndex++;
          }
        }

        ProtocolData data = protocols.get( protocol );
        data.packetMap.put( packetClass, mapping.packetID );
        data.packetConstructors[mapping.packetID] = packetSupplier; //BotFilter
      }
    }

    public final int getId(Class<? extends DefinedPacket> packet, int version)
    {

      ProtocolData protocolData = getProtocolData( version );
      if ( protocolData == null )
      {
        throw new FastException( "Unsupported protocol version" );
      }
      Preconditions.checkArgument( protocolData.packetMap.containsKey( packet ), "Cannot get ID for packet %s in phase %s with direction %s", packet, protocolPhase, direction );

      return protocolData.packetMap.get( packet );
    }
  }
}