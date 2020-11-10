package xyz.yooniks.aegis.caching;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.Chat;
import net.md_5.bungee.protocol.packet.KeepAlive;
import net.md_5.bungee.protocol.packet.Kick;
import net.md_5.bungee.protocol.packet.PluginMessage;
import xyz.yooniks.aegis.config.Settings;
import xyz.yooniks.aegis.packets.*;
import xyz.yooniks.aegis.shell.BotShell;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author Leymooo
 */
public class PacketUtils {

    public static final CachedCaptcha captchas = new CachedCaptcha();
    private static final CachedPacket[] cachedPackets = new CachedPacket[31];
    private static final Map<KickType, CachedPacket> kickMessagesGame = new HashMap<>(3);
    private static final Map<KickType, CachedPacket> kickMessagesLogin = new HashMap<>(4);
    public static int PROTOCOLS_COUNT = ProtocolConstants.SUPPORTED_VERSION_IDS_BF.size();
    public static int CLIENTID = new Random().nextInt(Integer.MAX_VALUE - 100) + 50;
    public static CachedExpPackets expPackets;

    /**
     * 0 - Checking_fall, 1 - checking_captcha, 2 - sus
     */
    public static CachedTitle[] titles = new CachedTitle[11];

    public static ByteBuf createPacket(DefinedPacket packet, int id, int protocol) {
        final ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        DefinedPacket.writeVarInt(id, buffer);
        packet.write(buffer, ProtocolConstants.Direction.TO_CLIENT, protocol);
        return buffer;
    }

    public static void init() {
        if (expPackets != null) {
            expPackets.release();
        }
        for (CachedPacket packet : cachedPackets) {
            if (packet != null) {
                packet.release();
            }
        }
        for (CachedTitle title : titles) {
            if (title != null) {
                title.release();
            }
        }
        for (CachedPacket packet : kickMessagesGame.values()) {
            packet.release();
        }
        kickMessagesGame.clear();

        expPackets = new CachedExpPackets();

        titles[0] = new CachedTitle(Settings.IMP.MESSAGES.CHECKING_TITLE, 5, 90, 15);
        titles[1] = new CachedTitle(Settings.IMP.MESSAGES.CHECKING_TITLE_CAPTCHA, 5, 15, 10);
        titles[2] = new CachedTitle(Settings.IMP.MESSAGES.CHECKING_TITLE_SUS, 5, 20, 10);
        titles[3] = new CachedTitle(Settings.IMP.AUTH.MESSAGES.REGISTER_MESSAGE_TITLE, 5, 20, 10);

        titles[4] = new CachedTitle(Settings.IMP.AUTH.MESSAGES.LOGIN_MESSAGE_TITLE, 5, 20, 10);
        titles[5] = new CachedTitle(Settings.IMP.AUTH.MESSAGES.PREMIUM_MESSAGE_TITLE, 5, 20, 10);

        titles[6] = new CachedTitle(Settings.IMP.AUTH.MESSAGES.LOADING_USER_TITLE, 5, 20, 10);
        titles[7] = new CachedTitle(Settings.IMP.AUTH.MESSAGES.LOGGED_MESSAGE_TITLE, 5, 20, 10);
        titles[8] = new CachedTitle(Settings.IMP.AUTH.MESSAGES.ASK_USER_WHEN_PREMIUM_TITLE, 5, 20, 10);

        titles[9] = new CachedTitle(Settings.IMP.ANTIVPN.TITLE_CHECKING, 5, 20, 10);
        titles[10] = new CachedTitle(Settings.IMP.ANTIVPN.TITLE_CHECKED, 5, 20, 10);

        final char[] empty = new char[6080];
        Arrays.fill(empty, ' ');

        DefinedPacket[] packets
                =
                {
                        //new Login(CLIENTID, (short) 2, 0, 0, (short) 0, (short) 100, "flat", 3, false, true),
                        // new Login(CLIENTID, (short)0,(short) 0, new HashSet<>(Arrays.asList("world")),null, 1, "world",0, (short)1, (short)100, "default",3, false, true, false, false),
                        new JoinGame(CLIENTID),
                        //0
                        new EmptyChunkPacket(0, 0), //1
                        new TimeUpdate(1, 23700), //2
                        new PlayerAbilities((byte) 6, 0f, 0f), //3
                        new PlayerPositionAndLook(7.00, 450, 7.00, 90f, 38f, 9876, false), //4
                        new SetSlot(0, 36, 358, 1, 0), //5 map 1.8+
                        new SetSlot(0, 36, -1, 0, 0), //6 map reset
                        new KeepAlive(9876), //7
                        createMessagePacket(
                                Settings.IMP.MESSAGES.CHECKING_CAPTCHA_WRONG.replaceFirst("%s", "2")),
//.replaceFirst( "%s", "попытки" ) ), //8
                        createMessagePacket(
                                Settings.IMP.MESSAGES.CHECKING_CAPTCHA_WRONG.replaceFirst("%s", "1")),
//.replaceFirst( "%s", "попытка" ) ), //9
                        createMessagePacket(Settings.IMP.MESSAGES.CHECKING), //10
                        createMessagePacket(Settings.IMP.MESSAGES.CHECKING_CAPTCHA), //11
                        createMessagePacket(Settings.IMP.MESSAGES.SUCCESSFULLY), //12
                        new PlayerPositionAndLook(7.00, 450, 7.00, 90f, 10f, 9876, false), //13
                        new SetExp(0, 0, 0), //14
                        createPluginMessage(), //15
                        createMessagePacket(Settings.IMP.AUTH.MESSAGES.REGISTER_MESSAGE), //16
                        createMessagePacket(Settings.IMP.AUTH.MESSAGES.LOGIN_MESSAGE), //17
                        createMessagePacket(Settings.IMP.AUTH.MESSAGES.PREMIUM_MESSAGE_CHAT), //18
                        createMessagePacket(Settings.IMP.AUTH.MESSAGES.LOADING_USER_CHAT), //19
                        createMessagePacket(Settings.IMP.AUTH.MESSAGES.LOADED_USER), //20
                        createMessagePacket(Settings.IMP.AUTH.MESSAGES.YOUR_DATA_IS_BEING_LOADED), //21
                        createMessagePacket(Settings.IMP.AUTH.MESSAGES.PREMIUM_USER_CANNOT_DO_THAT), //22
                        createMessagePacket(Settings.IMP.AUTH.MESSAGES.PASSWORDS_DO_NOT_MATCH), //23
                        createMessagePacket(Settings.IMP.AUTH.MESSAGES.LOGGED_MESSAGE), //24
                        createMessagePacket(Settings.IMP.AUTH.MESSAGES.REGISTERED_MESSAGE), //25
                        createKickPacket(Settings.IMP.AUTH.MESSAGES.PREMIUM_NEED_RELOGIN), //26
                        createMessagePacket(Settings.IMP.AUTH.MESSAGES.ASK_USER_WHEN_PREMIUM_CHAT), //27
                        createMessagePacket(new String(empty)), //28
                        createMessagePacket(Settings.IMP.ANTIVPN.CHAT_CHECKING), //29
                        createMessagePacket(Settings.IMP.ANTIVPN.CHAT_CHECKED), //30
                };

        for (int i = 0; i < packets.length; i++) {
            PacketUtils.cachedPackets[i] = new CachedPacket(packets[i], Protocol.BotFilter,
                    Protocol.GAME);
        }
        Protocol kickGame = Protocol.GAME;
        Protocol kickLogin = Protocol.LOGIN;

        kickMessagesGame.put(KickType.ANTIVPN_ERROR, new CachedPacket(
                createKickPacket(Settings.IMP.ANTIVPN.KICK_MESSAGE), kickGame));
        kickMessagesGame.put(KickType.TOO_LONG_VERIFICATION, new CachedPacket(
                createKickPacket(Settings.IMP.ANTIVPN.TOO_LONG), kickGame));

        kickMessagesGame.put(KickType.MC_BRAND, new CachedPacket(
                createKickPacket(Settings.IMP.MESSAGES.BOT_BEHAVIOUR), kickGame));

        kickMessagesGame.put(KickType.PING,
                new CachedPacket(createKickPacket(Settings.IMP.MESSAGES.KICK_BIG_PING), kickGame));
        kickMessagesGame.put(KickType.NOTPLAYER,
                new CachedPacket(createKickPacket(Settings.IMP.MESSAGES.KICK_NOT_PLAYER), kickGame));
        kickMessagesGame.put(KickType.COUNTRY,
                new CachedPacket(createKickPacket(Settings.IMP.MESSAGES.KICK_COUNTRY), kickGame));
        kickMessagesGame.put(KickType.TOO_LONG_LOGGING,
                new CachedPacket(createKickPacket(Settings.IMP.AUTH.MESSAGES.KICK_MESSAGE_TOO_LONG),
                        kickGame));

        //kickMessagesLogin.put( KickType.PING, new CachedPacket( createKickPacket( String.join( "", Settings.IMP.SERVER_PING_CHECK.KICK_MESSAGE ) ), kickLogin ) );
        kickMessagesLogin.put(KickType.MANYCHECKS,
                new CachedPacket(createKickPacket(Settings.IMP.MESSAGES.KICK_MANY_CHECKS), kickLogin));
        kickMessagesLogin.put(KickType.COUNTRY,
                new CachedPacket(createKickPacket(Settings.IMP.MESSAGES.KICK_COUNTRY), kickLogin));
        kickMessagesLogin.put(KickType.TOO_LONG_LOGGING,
                new CachedPacket(createKickPacket(Settings.IMP.AUTH.MESSAGES.KICK_MESSAGE_TOO_LONG),
                        kickLogin));

        //BungeeCord bungee = BungeeCord.getInstance();
        //kickMessagesLogin.put( KickType.THROTTLE, new CachedPacket( createKickPacket( bungee.getTranslation( "join_throttle_kick", TimeUnit.MILLISECONDS.toSeconds( bungee.getConfig().getThrottle() ) ) ), kickLogin ) );

    }

    public static DefinedPacket createKickPacket(String message) {
        return new Kick(ComponentSerializer.toString(
                TextComponent.fromLegacyText(
                        ChatColor.translateAlternateColorCodes('&',
                                message.replace("%prefix%", Settings.IMP.MESSAGES.PREFIX).replace("%nl%", "\n")))));
    }

    public static DefinedPacket createMessagePacket(String message) {
        return new Chat(ComponentSerializer.toString(
                TextComponent.fromLegacyText(
                        ChatColor.translateAlternateColorCodes('&',
                                message.replace("%prefix%", Settings.IMP.MESSAGES.PREFIX).replace("%nl%", "\n")))),
                (byte) ChatMessageType.CHAT.ordinal());
    }

    private static DefinedPacket createPluginMessage() {
        BungeeCord bungee = BungeeCord.getInstance();
        ByteBuf brand = ByteBufAllocator.DEFAULT.heapBuffer();
        DefinedPacket.writeString("Aegis 1.7.x-1.16.x", brand);
        DefinedPacket packet = new PluginMessage("MC|Brand", DefinedPacket.toArray(brand), false);
        brand.release();
        return packet;
    }

    public static int getPacketId(DefinedPacket packet, int version, Protocol... protocols) {
        for (Protocol protocol : protocols) {
            try {
                return protocol.TO_CLIENT.getId(packet.getClass(), version);
            } catch (Exception ignore) {
            }
        }

        throw new IllegalStateException(
                "Can not get id for " + packet.getClass().getSimpleName() + "(" + version + ")");
    }

    public static void releaseByteBuf(ByteBuf buf) {
        if (buf != null && buf.refCnt() != 0) {
            while (buf.refCnt() != 0) {
                buf.release();
            }
        }
    }

    public static void fillArray(ByteBuf[] buffer, DefinedPacket packet, Protocol... protocols) {
        if (packet == null) {
            return;
        }
        int oldPacketId = -1;
        ByteBuf oldBuf = null;
        for (int version : ProtocolConstants.SUPPORTED_VERSION_IDS_BF) {
            //if (version < ProtocolConstants.MINECRAFT_1_8) continue;
            int versionRewrited = rewriteVersion(version);
            int newPacketId = PacketUtils.getPacketId(packet, version, protocols);
            if (newPacketId != oldPacketId) {
                oldPacketId = newPacketId;
                oldBuf = PacketUtils.createPacket(packet, oldPacketId, version);
                buffer[versionRewrited] = oldBuf;
            } else {
                ByteBuf newBuf = PacketUtils.createPacket(packet, oldPacketId, version);
                if (newBuf.equals(oldBuf)) {
                    buffer[versionRewrited] = oldBuf;
                    newBuf.release();
                } else {
                    oldBuf = newBuf;
                    buffer[versionRewrited] = oldBuf;
                }
            }
        }
    }

    public static int rewriteVersion(int version) {
        switch (version) {

            /*case ProtocolConstants.MINECRAFT_1_7_2:
                return 0;
            case ProtocolConstants.MINECRAFT_1_7_6:
                return 1;

            case ProtocolConstants.MINECRAFT_1_8:
                return 2;
            case ProtocolConstants.MINECRAFT_1_9:
                return 3;
            case ProtocolConstants.MINECRAFT_1_9_1:
                return 4;
            case ProtocolConstants.MINECRAFT_1_9_2:
                return 5;
            case ProtocolConstants.MINECRAFT_1_9_4:
                return 6;
            case ProtocolConstants.MINECRAFT_1_10:
                return 7;
            case ProtocolConstants.MINECRAFT_1_11:
                return 8;
            case ProtocolConstants.MINECRAFT_1_11_1:
                return 9;
            case ProtocolConstants.MINECRAFT_1_12:
                return 10;
            case ProtocolConstants.MINECRAFT_1_12_1:
                return 11;
            case ProtocolConstants.MINECRAFT_1_12_2:
                return 12;
            case ProtocolConstants.MINECRAFT_1_13:
                return 13;
            case ProtocolConstants.MINECRAFT_1_13_1:
                return 14;
            case ProtocolConstants.MINECRAFT_1_13_2:
                return 15;
            case ProtocolConstants.MINECRAFT_1_14:
                return 16;
            case ProtocolConstants.MINECRAFT_1_14_1:
                return 17;
            case ProtocolConstants.MINECRAFT_1_14_2:
                return 18;
            case ProtocolConstants.MINECRAFT_1_14_3:
                return 19;
            case ProtocolConstants.MINECRAFT_1_14_4:
                return 20;
            case ProtocolConstants.MINECRAFT_1_15:
                return 21;
            case ProtocolConstants.MINECRAFT_1_15_1:
                return 22;
            case ProtocolConstants.MINECRAFT_1_15_2:
                return 23;*/
            case ProtocolConstants.MINECRAFT_1_8:
                return 0;
            case ProtocolConstants.MINECRAFT_1_9:
                return 1;
            case ProtocolConstants.MINECRAFT_1_9_1:
                return 2;
            case ProtocolConstants.MINECRAFT_1_9_2:
                return 3;
            case ProtocolConstants.MINECRAFT_1_9_4:
                return 4;
            case ProtocolConstants.MINECRAFT_1_10:
                return 5;
            case ProtocolConstants.MINECRAFT_1_11:
                return 6;
            case ProtocolConstants.MINECRAFT_1_11_1:
                return 7;
            case ProtocolConstants.MINECRAFT_1_12:
                return 8;
            case ProtocolConstants.MINECRAFT_1_12_1:
                return 9;
            case ProtocolConstants.MINECRAFT_1_12_2:
                return 10;
            case ProtocolConstants.MINECRAFT_1_13:
                return 11;
            case ProtocolConstants.MINECRAFT_1_13_1:
                return 12;
            case ProtocolConstants.MINECRAFT_1_13_2:
                return 13;
            case ProtocolConstants.MINECRAFT_1_14:
                return 14;
            case ProtocolConstants.MINECRAFT_1_14_1:
                return 15;
            case ProtocolConstants.MINECRAFT_1_14_2:
                return 16;
            case ProtocolConstants.MINECRAFT_1_14_3:
                return 17;
            case ProtocolConstants.MINECRAFT_1_14_4:
                return 18;
            case ProtocolConstants.MINECRAFT_1_15:
                return 19;
            case ProtocolConstants.MINECRAFT_1_15_1:
                return 20;
            case ProtocolConstants.MINECRAFT_1_15_2:
                return 21;
            case ProtocolConstants.MINECRAFT_1_16:
                return 22;
            case ProtocolConstants.MINECRAFT_1_16_1:
                return 23;
            case ProtocolConstants.MINECRAFT_1_16_2:
                return 24;
            case ProtocolConstants.MINECRAFT_1_16_3:
                return 24;
            default:
                throw new IllegalArgumentException("Version is not supported " + version);
        }
    }

    public static void spawnPlayer(Channel channel, int version, boolean disableFall,
                                   boolean captcha) {

        /*if (version < 47) {
            channel.write( getCachedPacket( PacketsPosition.LOGIN ).get( version ), channel.voidPromise() );
            channel.write( getCachedPacket( PacketsPosition.PLAYERABILITIES ).get( version ), channel.voidPromise() );
            channel.write( getCachedPacket( PacketsPosition.PLAYERPOSANDLOOK ).get( version ), channel.voidPromise() );
            channel.write( getCachedPacket( PacketsPosition.TIME ).get( version ), channel.voidPromise() );
            return;
        }*/

        try {
            channel
                    .write(getCachedPacket(PacketsPosition.LOGIN).get(version), channel.voidPromise());
            channel.write(getCachedPacket(PacketsPosition.PLUGIN_MESSAGE).get(version),
                    channel.voidPromise());
            channel
                    .write(getCachedPacket(PacketsPosition.CHUNK).get(version), channel.voidPromise());

            if (disableFall) {
                channel.write(getCachedPacket(PacketsPosition.PLAYERABILITIES).get(version),
                        channel.voidPromise());
            }
            if (captcha) {
                channel
                        .write(getCachedPacket(PacketsPosition.PLAYERPOSANDLOOK_CAPTCHA).get(version),
                                channel.voidPromise());

            } else {

                channel.write(getCachedPacket(PacketsPosition.PLAYERPOSANDLOOK).get(version),
                        channel.voidPromise());

            }

            channel
                    .write(getCachedPacket(PacketsPosition.TIME).get(version), channel.voidPromise());

        } catch (Exception ignored) {
        }
        //channel.flush(); Не очищяем поскольку это будет в другом месте
    }

    public static void kickPlayer(DefinedPacket message, ChannelWrapper wrapper) {
        if (wrapper.isClosed() || wrapper.isClosing()) {
            return;
        }
        wrapper.write(message);
        wrapper.close();
    }

    public static void kickPlayer(PendingConnection connection, BotShell shell, ChannelWrapper wrapper) {
        if (wrapper.isClosed() || wrapper.isClosing()) {
            return;
        }
        wrapper.write(shell.getKickPacket(connection));
        wrapper.close();
    }

    public static void kickPlayer(KickType kick, Protocol protocol, ChannelWrapper wrapper,
                                  int version) {
        if (wrapper.isClosed() || wrapper.isClosing()) {
            return;
        }
        if (protocol == Protocol.GAME) {
            wrapper.write(kickMessagesGame.get(kick).get(version));
        } else {
            wrapper.write(kickMessagesLogin.get(kick).get(version));
        }
        wrapper.close();
    }

    public static CachedPacket getCachedPacket(int pos) {
        return cachedPackets[pos];
    }

    public static enum KickType {
        MANYCHECKS,
        NOTPLAYER,
        COUNTRY,
        TOO_LONG_LOGGING,
        ANTIVPN_ERROR,
        TOO_LONG_VERIFICATION,
        // THROTTLE,
        PING,
        MC_BRAND;
    }

}