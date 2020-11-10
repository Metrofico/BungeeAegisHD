package net.md_5.bungee.connection;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.md_5.bungee.*;
import net.md_5.bungee.api.AbstractReconnectHandler;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.http.HttpClient;
import net.md_5.bungee.jni.cipher.BungeeCipher;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.netty.cipher.CipherDecoder;
import net.md_5.bungee.netty.cipher.CipherEncoder;
import net.md_5.bungee.protocol.*;
import net.md_5.bungee.protocol.packet.*;
import net.md_5.bungee.util.BoundedArrayList;
import xyz.yooniks.aegis.Aegis;
import xyz.yooniks.aegis.auth.AuthSystem;
import xyz.yooniks.aegis.auth.handler.AuthConnector;
import xyz.yooniks.aegis.auth.premium.PremiumManager;
import xyz.yooniks.aegis.auth.premium.PremiumManager.PremiumUser;
import xyz.yooniks.aegis.auth.user.AuthUser;
import xyz.yooniks.aegis.auth.user.AuthUser.PremiumAnswer;
import xyz.yooniks.aegis.blacklist.Blacklist;
import xyz.yooniks.aegis.config.Settings;
import xyz.yooniks.aegis.filter.Connector;
import xyz.yooniks.aegis.shell.BotShell;
import xyz.yooniks.aegis.shell.PingableBotCheck;
import xyz.yooniks.aegis.utils.PingLimiter;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

@RequiredArgsConstructor
public class InitialHandler extends PacketHandler implements PendingConnection {

    private final BungeeCord bungee;
    @Getter
    private final ListenerInfo listener;
    @Getter
    private final List<PluginMessage> relayMessages = new BoundedArrayList<>(128);
    @Getter //Aegis
    private ChannelWrapper ch;
    private final Unsafe unsafe = new Unsafe() {
        @Override
        public void sendPacket(DefinedPacket packet) {
            ch.write(packet);
        }
    };
    @Getter
    private Handshake handshake;
    @Getter
    private LoginRequest loginRequest;
    private EncryptionRequest request;
    private State thisState = State.HANDSHAKE;
    @Getter
    private InetSocketAddress virtualHost;
    private String name;
    @Getter
    private boolean onlineMode = BungeeCord.getInstance().config.isOnlineMode() || isPremium();
    @Getter
    private UUID uniqueId;
    @Getter
    private UUID offlineId;
    @Getter
    @Setter
    private LoginResult loginProfile;
    @Getter
    private boolean legacy;
    @Getter
    private String extraDataInHandshake = "";
    private boolean sentEncryption = false;

    private static String getFirstLine(String str) {
        int pos = str.indexOf('\n');
        return pos == -1 ? str : str.substring(0, pos);
    }

    private boolean isPremium() {
        if (this.getName() == null) {
            return false;
        }
        final PremiumUser user = PremiumManager.getByName(this.getName());
        final AuthSystem authSystem = bungee.getAegis().getAuthSystem();
        if (user == null || !user.isPremium() || authSystem == null) {
            return false;
        }
        final AuthUser byName = authSystem.getUserManager().getByName(this.getName());
        return byName != null && byName.isPremium() && byName.getPremiumAnswer() != PremiumAnswer.NO;
    }

    @Override
    public boolean shouldHandle(PacketWrapper packet) {
        return !ch.isClosing();
    }

    @Override
    public void connected(ChannelWrapper channel) {
        this.ch = channel;
    }

    @Override
    public void exception(Throwable t) {
        disconnect(ChatColor.RED + Util.exception(t));
    }

    @Override
    public void handle(PacketWrapper packet) throws Exception {
        if (packet.packet == null) {
            bungee.getAegis().getStatistics().addBlockedConnection();
            ch.close();

            final String hostname = getAddress().getAddress().getHostAddress();
            //AddressBlocker.block(hostname);

            /*if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
                this.bungee.getLogger()
                    .log(Level.INFO, "[Aegis] {0} unexpected packet received during login process!",
                        hostname);
            }*/

            bungee.getAegis().getBlacklistManager().addBlacklist(hostname);

            final Blacklist blacklist = bungee.getAegis().getBlacklist();
            if (blacklist != null) {
                blacklist.asyncDropWithoutBlacklistManager(getAddress().getAddress().getHostAddress());
            }

            throw new FastException("[Exploit] Unexpected packet received during login process!");
            //this.ch.getHandle().close(); //Aegis
            //throw new IllegalArgumentException( "Unexpected packet received during login process!\n" + BufUtil.dump( packet.buf, 64 ) );
            //throw new QuietException( "Unexpected packet received during login process! " + BufUtil.dump( packet.buf, 16 ) );
        }
    }

    @Override
    public void handle(PluginMessage pluginMessage) throws Exception {
        // TODO: Unregister?
        if (PluginMessage.SHOULD_RELAY.apply(pluginMessage)) {
            relayMessages.add(pluginMessage);
        }
    }

    @Override
    public void handle(LegacyHandshake legacyHandshake) throws Exception {
        this.legacy = true;
        ch.close(bungee.getTranslation("outdated_client", bungee.getGameVersion()));
    }

    @Override
    public void handle(LegacyPing ping) throws Exception {
        this.legacy = true;
        final boolean v1_5 = ping.isV1_5();

        ServerPing legacy = new ServerPing(
                new ServerPing.Protocol(bungee.getName(), bungee.getProtocolVersion()),
                new ServerPing.Players(listener.getMaxPlayers(), bungee.getOnlineCountBF(true), null),
                //Aegis
                new TextComponent(TextComponent.fromLegacyText(listener.getMotd())), null);

        Callback<ProxyPingEvent> callback = new Callback<ProxyPingEvent>() {
            @Override
            public void done(ProxyPingEvent result, Throwable error) {
                if (ch.isClosed()) {
                    return;
                }

                ServerPing legacy = result.getResponse();
                String kickMessage;

                if (v1_5) {
                    kickMessage = ChatColor.DARK_BLUE
                            + "\00" + 127
                            + '\00' + legacy.getVersion().getName()
                            + '\00' + getFirstLine(legacy.getDescription())
                            + '\00' + legacy.getPlayers().getOnline()
                            + '\00' + legacy.getPlayers().getMax();
                } else {
                    // Clients <= 1.3 don't support colored motds because the color char is used as delimiter
                    kickMessage = ChatColor.stripColor(getFirstLine(legacy.getDescription()))
                            + '\u00a7' + legacy.getPlayers().getOnline()
                            + '\u00a7' + legacy.getPlayers().getMax();
                }

                ch.close(kickMessage);
            }
        };

        bungee.getPluginManager().callEvent(new ProxyPingEvent(this, legacy, callback));
    }

    @Override
    public void handle(StatusRequest statusRequest) throws Exception {
        if (thisState != State.STATUS) {
            this.wrongState(thisState, State.STATUS);
            return;
        }

        //final AegisStatistics statistics = bungee.getAegis().getStatistics();
        //final String address = getAddress().getAddress().getHostAddress();
        for (BotShell check : bungee.getAegis().getShellManager().getShells()) {
            if (check instanceof PingableBotCheck) {
                ((PingableBotCheck) check)
                        .pinged(getName(), getAddress().getAddress().getHostAddress());
            }
        }
        //final Ping ping = bungee.getAegis().getPingManager().getPing(address);

        /*if (Settings.IMP.AEGIS_SETTINGS.LIMIT_PINGS_PER_IP && ping != null && statistics.getPingsPerSecond() > 100 && !this.bungee.getAegis().isChecked(address)) {

            if (ping.getSuspiciousCount() > 8) {
                if (!Settings.IMP.AEGIS_SETTINGS.BYPASS_IPS.contains(address)) {
                    this.ch.close();
                    AddressBlocker.block(address);
                    if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
                        bungee.getLogger().info("[Aegis] [Ping Limitter] - " + address
                            + " has been blocked! Too many pings!");
                    }
                    return;
                }
            }
        }*/

      /*  if (Settings.IMP.AEGIS_SETTINGS.ADVANCED_CHECKS.BLOCK_AUTH_SMASHER_AT_ALL && ping != null && !this.bungee.getAegis().isChecked(address)
            && (statistics.getPingsPerSecond() > Settings.IMP.AEGIS_SETTINGS.ADVANCED_CHECKS.MAX_PINGS_PER_SECOND
            || ping.getSuspiciousCount() > Settings.IMP.AEGIS_SETTINGS.ADVANCED_CHECKS.MAX_PINGS_PER_IP )) {


            if (!Settings.IMP.AEGIS_SETTINGS.BYPASS_IPS.contains(address)) {
                this.ch.close();

                if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
                    bungee.getLogger().info(
                        "[Aegis] [Ping Limitter (Advanced auth smasher blocker)] - " + address
                            + " has been aborted! Too many pings!");
                }
                return;
            }
        }*/

        ServerInfo forced = AbstractReconnectHandler.getForcedHost(this);
        final String motd = (forced != null) ? forced.getMotd() : listener.getMotd();

        Callback<ServerPing> pingBack = (result, error) -> {
            if (error != null) {
                result = new ServerPing();
                result.setDescription(bungee.getTranslation("ping_cannot_connect"));
                bungee.getLogger().log(Level.WARNING, "Error pinging remote server", error);
            }

            Callback<ProxyPingEvent> callback = new Callback<ProxyPingEvent>() {
                @Override
                public void done(ProxyPingEvent pingResult, Throwable error) {
                    Gson gson =
                            handshake.getProtocolVersion() == ProtocolConstants.MINECRAFT_1_7_2 ? BungeeCord
                                    .getInstance().gsonLegacy : BungeeCord.getInstance().gson;

                    if (bungee.getConnectionThrottle() != null) {
                        bungee.getConnectionThrottle().unthrottle(getSocketAddress());
                    }
                    if (ProtocolConstants.isBeforeOrEq(handshake.getProtocolVersion(),
                            ProtocolConstants.MINECRAFT_1_8)) {
                        JsonElement element = gson.toJsonTree(pingResult.getResponse());
                        Preconditions.checkArgument(element.isJsonObject(),
                                "Response is not a JSON object");
                        JsonObject object = element.getAsJsonObject();
                        object.addProperty("description",
                                pingResult.getResponse().getDescription());

                        unsafe.sendPacket(new StatusResponse(gson.toJson(element)));
                    } else {
                        unsafe.sendPacket(
                                new StatusResponse(gson.toJson(pingResult.getResponse())));
                    }
                }
            };

            bungee.getPluginManager()
                    .callEvent(new ProxyPingEvent(InitialHandler.this, result, callback));
        };

        if (forced != null && listener.isPingPassthrough()) {
            ((BungeeServerInfo) forced).ping(pingBack, handshake.getProtocolVersion());
        } else {
            int protocol =
                    (ProtocolConstants.SUPPORTED_VERSION_IDS.contains(handshake.getProtocolVersion()))
                            ? handshake.getProtocolVersion() : bungee.getProtocolVersion();
            pingBack.done(new ServerPing(
                            new ServerPing.Protocol(ChatColor.RED
                                    + Settings.IMP.AEGIS_SETTINGS.GAME_VERSION,
                                    protocol),
                            new ServerPing.Players(listener.getMaxPlayers(), bungee.getOnlineCountBF(true),
                                    null), //Aegis
                            motd,
                            PingLimiter.handle() ? null : BungeeCord.getInstance().config.getFaviconObject()),
                    //Aegis PingLimiter.handle() ? null :
                    null);
        }

        thisState = State.PING;
    }

    @Override
    public void handle(PingPacket ping) throws Exception {
        if (thisState != State.PING) {
            this.wrongState(thisState, State.PING);
            return;
        }
        unsafe.sendPacket(ping);
        disconnect("");
    }

    @Override
    public void handle(Handshake handshake) throws Exception {

        if (thisState != State.HANDSHAKE) {
            this.wrongState(thisState, State.HANDSHAKE);
            return;
        }
        this.handshake = handshake;

        final String address = getAddress().getAddress().getHostAddress();
        /*if (handshake.getRequestedProtocol() != 1 && handshake.getRequestedProtocol() != 2) {
            ch.close();
            AddressBlocker.block(address);

            if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE)
                bungee.getLogger().log(Level.INFO, "[Aegis] {0} Invalid protocol requested!",
                    address);

            bungee.getAegis().getBlacklistManager().addBlacklist(address);

            final Blacklist blacklist = bungee.getAegis().getBlacklist();
            if (blacklist != null) {
                blacklist.asyncDropWithoutBlacklistManager(address);
            }
            return;
        }*/

        ch.setVersion(handshake.getProtocolVersion());

        // Starting with FML 1.8, a "\0FML\0" token is appended to the handshake. This interferes
        // with Bungee's IP forwarding, so we detect it, and remove it from the host string, for now.
        // We know FML appends \00FML\00. However, we need to also consider that other systems might
        // add their own data to the end of the string. So, we just take everything from the \0 character
        // and save it for later.
        if (handshake.getHost().contains("\0")) {
            String[] split = handshake.getHost().split("\0", 2);
            handshake.setHost(split[0]);
            extraDataInHandshake = "\0" + split[1];
        }

        // SRV records can end with a . depending on DNS / client.
        if (handshake.getHost().endsWith(".")) {
            handshake.setHost(handshake.getHost().substring(0, handshake.getHost().length() - 1));
        }

        this.virtualHost = InetSocketAddress
                .createUnresolved(handshake.getHost(), handshake.getPort());

        bungee.getPluginManager()
                .callEvent(new PlayerHandshakeEvent(InitialHandler.this, handshake));
        switch (handshake.getRequestedProtocol()) {
            case 1:
                // Ping
                thisState = State.STATUS;
                ch.setProtocol(Protocol.STATUS);
                //bungee.getAegis().getPingManager().ping(this);
                bungee.getAegis().getStatistics().addPingPerSecond();

                if (bungee.getConfig().isLogPings() && !Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
                    bungee.getLogger().log(Level.INFO, "{0} has pinged", this);
                }
                break;
            case 2:
                // Login
                if (!bungee.getConfig().isLogPings()
                        && !Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
                    bungee.getLogger().log(Level.INFO, "{0} has connected", this);
                }
                thisState = State.USERNAME;
                ch.setProtocol(Protocol.LOGIN);

                if (bungee.getAegis().getAuthSystem() != null
                        && handshake.getProtocolVersion() < ProtocolConstants.MINECRAFT_1_8
                    /*&& !this.bungee.getConfig().isAllowV1_7_Support()*/) {
                    this.bungee.getLogger().info("[Aegis Auth] " + getName()
                            + " tried to login with 1.7 minecraft but auth-system is enabled so 1.7 is blocked automatically!");
                    disconnect(bungee.getTranslation("outdated_client", bungee.getGameVersion()));
                    return;
                } else {
                    if (handshake.getProtocolVersion() < ProtocolConstants.MINECRAFT_1_8
                            && !Settings.IMP.AEGIS_SETTINGS.ALLOW_V1_7_SUPPORT) {
                        disconnect(
                                bungee.getTranslation("outdated_client", bungee.getGameVersion()));
                        return;
                    }
                }

                if (!ProtocolConstants.SUPPORTED_VERSION_IDS
                        .contains(handshake.getProtocolVersion())) {
                    if (handshake.getProtocolVersion() > bungee.getProtocolVersion()) {
                        disconnect(
                                bungee.getTranslation("outdated_server", bungee.getGameVersion()));
                    } else {
                        disconnect(
                                bungee.getTranslation("outdated_client", bungee.getGameVersion()));
                    }
                    return;
                }

                if (Settings.IMP.AEGIS_SETTINGS.BLOCKED_PROTOCOLS
                        .contains(handshake.getProtocolVersion())) {
                    disconnect(ChatColor.translateAlternateColorCodes('&',
                            Settings.IMP.AEGIS_SETTINGS.PROTOCOL_BLOCKED));
                    return;
                }

                break;
            default:
                ch.close();
                //AddressBlocker.block(address);

                //if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE)
                //    bungee.getLogger().log(Level.INFO, "[Aegis] {0} Invalid protocol requested!",
                //       address);

                //AddressBlocker.block(address);

                bungee.getAegis().getBlacklistManager().addBlacklist(address);

                final Blacklist blacklist = bungee.getAegis().getBlacklist();
                if (blacklist != null) {
                    blacklist.asyncDropWithoutBlacklistManager(address);
                }

                throw new FastException("[Exploit] Invalid protocol requested");
                // return;
                //throw new FastException(
                //    "Cannot request protocol " + handshake.getRequestedProtocol());
        }
    }

    @Override
    public void handle(LoginRequest loginRequest) throws Exception {
        if (thisState != State.USERNAME) {
            this.wrongState(thisState, State.USERNAME);
            return;
        }
        this.loginRequest = loginRequest;

        bungee.getAegis().checkAsyncIfNeeded(this);
        //Aegis moved code to delayedHandleOfLoginRequset();
    }

    public void delayedHandleOfLoginRequset() {
        if (getName().contains(".") || (
                Settings.IMP.AEGIS_SETTINGS.ADVANCED_CHECKS.NAME_PATTERN_CHECK
                        && !getName().matches(Settings.IMP.AEGIS_SETTINGS.ADVANCED_CHECKS.ALLOWED_PATTERN))) {
            disconnect(bungee.getTranslation("name_invalid"));
            return;
        }

        final int length = getName().length();
        if (length > Settings.IMP.AEGIS_SETTINGS.MAX_LENGTH_NAME) {
            disconnect(bungee.getTranslation("name_too_long"));
            return;
        }

        if (length < Settings.IMP.AEGIS_SETTINGS.MIN_LENGTH_NAME) {
            disconnect("Minimum length of nickname is: " + Settings.IMP.AEGIS_SETTINGS.MIN_LENGTH_NAME);
            return;
        }

        int limit = BungeeCord.getInstance().config.getPlayerLimit();
        if (limit > 0 && bungee.getOnlineCountBF(false) >= limit) {
            disconnect(bungee.getTranslation("proxy_full"));
            return;
        }

        // If offline mode and they are already on, don't allow connect
        // We can just check by UUID here as names are based on UUID
        if (!isOnlineMode() && bungee.getPlayer(getUniqueId()) != null) {
            disconnect(bungee.getTranslation("already_connected_proxy"));
            return;
        }

        if (!isOnlineMode() && bungee.getPlayerByOfflineUUID(getUniqueId()) != null) {
            disconnect(bungee.getTranslation("already_connected_proxy"));
            return;
        }

        if (!isOnlineMode() && bungee.getPlayer(getName()) != null) {
            disconnect(bungee.getTranslation("already_connected_proxy"));
            return;
        }

        Callback<PreLoginEvent> callback = (result, error) -> {
            if (result.isCancelled()) {
                disconnect(result.getCancelReasonComponents());
                return;
            }
            if (ch.isClosed()) {
                return;
            }
            if (onlineMode || isPremium()) {
                unsafe().sendPacket(request = EncryptionUtil.encryptRequest());
            } else if (isInEventLoop()) {
                finish();
            } else {
                ch.getHandle().eventLoop().execute(() ->
                {
                    if (!ch.isClosing()) {
                        finish();
                    }
                });
            }
            thisState = State.ENCRYPT;
        };
        //TODO: dodane
        //callback.done(new PreLoginEvent(this, (smth, smth2) -> {
        //}), null);

        // fire pre login event
        bungee.getPluginManager().callEvent(new PreLoginEvent(InitialHandler.this, callback));

    }

    @Override
    public void handle(final EncryptionResponse encryptResponse) throws Exception {
        if (this.sentEncryption && Settings.IMP.AEGIS_SETTINGS.ADVANCED_CHECKS.ENCRYPTION_LIMITTER) {
            this.ch.close();
            final String address = getAddress().getAddress().getHostAddress();
            bungee.getLogger().info("[Aegis] AdvancedCheck (Encryption)"
                    + " Sent more than one time <- " + address);
            //AddressBlocker.block(address);
            bungee.getAegis().getBlacklistManager().addBlacklist(address);
            return;
        }

        if (Settings.IMP.AEGIS_SETTINGS.ADVANCED_CHECKS.REMOVE_ENCRYPTION) {
            this.ch.close();
            final String address = getAddress().getAddress().getHostAddress();

            if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
                bungee.getLogger().info(
                        "[Aegis] Got encryption packet from " + address + " while encryption is blocked!");
            }
            bungee.getAegis().getBlacklistManager().addBlacklist(address);
            //AddressBlocker.block(address);
            return;
        }

        if (thisState != State.ENCRYPT) {
            this.wrongState(thisState, State.ENCRYPT);
            return;
        }
        this.sentEncryption = true;

        SecretKey sharedKey = EncryptionUtil.getSecret(encryptResponse, request);
        BungeeCipher decrypt = EncryptionUtil.getCipher(false, sharedKey);
        ch.addBefore(PipelineUtils.FRAME_DECODER, PipelineUtils.DECRYPT_HANDLER,
                new CipherDecoder(decrypt));
        BungeeCipher encrypt = EncryptionUtil.getCipher(true, sharedKey);
        ch.addBefore(PipelineUtils.FRAME_PREPENDER, PipelineUtils.ENCRYPT_HANDLER,
                new CipherEncoder(encrypt));

        String encName = URLEncoder.encode(InitialHandler.this.getName(), "UTF-8");

        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        for (byte[] bit : new byte[][]
                {
                        request.getServerId().getBytes("ISO_8859_1"), sharedKey.getEncoded(),
                        EncryptionUtil.keys.getPublic().getEncoded()
                }) {
            sha.update(bit);
        }
        String encodedHash = URLEncoder.encode(new BigInteger(sha.digest()).toString(16), "UTF-8");

        String preventProxy = (BungeeCord.getInstance().config.isPreventProxyConnections()
                && getSocketAddress() instanceof InetSocketAddress) ? "&ip=" + URLEncoder
                .encode(getAddress().getAddress().getHostAddress(), "UTF-8") : "";


        String authURL =
                Settings.IMP.AEGIS_SETTINGS.CHANGE_MOJANG_SESSION_URL
                        ? Settings.IMP.AEGIS_SETTINGS.CHANGED_MOJANG_SESSION_URL.replace("{ENCNAME}", encName).replace("{SERVERID}", encodedHash + preventProxy)
                        : "https://sessionserver.mojang.com/session/minecraft/hasJoined?username=" + encName
                        + "&serverId=" + encodedHash + preventProxy;

        Callback<String> handler = new Callback<String>() {
            @Override
            public void done(String result, Throwable error) {
                if (error == null) {
                    LoginResult obj = BungeeCord.getInstance().gson
                            .fromJson(result, LoginResult.class);
                    if (obj != null && obj.getId() != null) {
                        loginProfile = obj;
                        name = obj.getName();
                        uniqueId = Util.getUUID(obj.getId());
                        finish();
                        return;
                    }
                    disconnect(bungee.getTranslation("offline_mode_player"));
                } else {
                    disconnect(bungee.getTranslation("mojang_fail"));
                    bungee.getLogger().log(Level.SEVERE,
                            "Error authenticating " + getName() + " with minecraft.net", error);
                }
            }
        };

        HttpClient.get(authURL, ch.getHandle().eventLoop(), handler);
    }

    private void finish() {
        if (this.bungee.getAegis().getAuthSystem() == null
                && Settings.IMP.AEGIS_SETTINGS.ONLINE_UUIDS_SUPPORT) {
            if (isOnlineMode()/* || premium*/) {
                /*if (premium)
                    uniqueId = PremiumManager.getByName(this.getName()).getUuid();*/

                // Check for multiple connections
                // We have to check for the old name first
                ProxiedPlayer oldName = bungee.getPlayer(getName());
                if (oldName != null) {
                    // TODO See #1218
                    oldName.disconnect(bungee.getTranslation("already_connected_proxy"));
                }
                // And then also for their old UUID
                ProxiedPlayer oldID = bungee.getPlayer(getUniqueId());
                if (oldID != null) {
                    // TODO See #1218
                    oldID.disconnect(bungee.getTranslation("already_connected_proxy"));
                }
            } else {
                // In offline mode the existing user stays and we kick the new one
                ProxiedPlayer oldName = bungee.getPlayer(getName());
                if (oldName != null) {
                    // TODO See #1218
                    disconnect(bungee.getTranslation("already_connected_proxy"));
                    return;
                }

            }

            offlineId = UUID
                    .nameUUIDFromBytes(("OfflinePlayer:" + getName()).getBytes(Charsets.UTF_8));
            if (uniqueId == null) {
                uniqueId = offlineId;
            }

            Callback<LoginEvent> complete = new Callback<LoginEvent>() {
                @Override
                public void done(LoginEvent result, Throwable error) {
                    if (result.isCancelled()) {
                        disconnect(result.getCancelReasonComponents());
                        return;
                    }
                    if (ch.isClosed()) {
                        return;
                    }

                    ch.getHandle().eventLoop().execute(() -> {
                        if (!ch.isClosing()) {
                            UserConnection userCon = new UserConnection(bungee, ch, getName(),
                                    InitialHandler.this);
                            userCon.setCompressionThreshold(
                                    BungeeCord.getInstance().config.getCompressionThreshold());
                            userCon.init();

                            sendLoginSuccess(true);
                            //unsafe.sendPacket( new LoginSuccess( getUniqueId().toString(), getName() ) ); // With dashes in between

                            ch.setProtocol(Protocol.GAME);

                            ch.getHandle().pipeline().get(HandlerBoss.class)
                                    .setHandler(new UpstreamBridge(bungee, userCon));
                            bungee.getPluginManager().callEvent(new PostLoginEvent(userCon));
                            ServerInfo server;
                            if (bungee.getReconnectHandler() != null) {
                                server = bungee.getReconnectHandler().getServer(userCon);
                            } else {
                                server = AbstractReconnectHandler
                                        .getForcedHost(InitialHandler.this);
                            }
                            if (server == null) {
                                server = bungee.getServerInfo(listener.getDefaultServer());
                            }

                            userCon
                                    .connect(server, null, true, ServerConnectEvent.Reason.JOIN_PROXY);

                            thisState = State.FINISHED;
                        }
                    });
                }
            };

            // fire login event
            bungee.getPluginManager().callEvent(new LoginEvent(InitialHandler.this, complete));
            return;
        }

        if (isOnlineMode()) {
            ProxiedPlayer oldName = bungee.getPlayer(getName());
            if (oldName != null) {
                oldName.disconnect(bungee.getTranslation(
                        "already_connected_proxy")); // TODO: Cache this disconnect packet
            }
            ProxiedPlayer oldID = bungee.getPlayer(getUniqueId());
            if (oldID != null) {
                oldID.disconnect(bungee.getTranslation(
                        "already_connected_proxy")); // TODO: Cache this disconnect packet
            }
        } else {
            ProxiedPlayer oldName = bungee.getPlayer(getName());
            if (oldName != null) {
                disconnect(bungee.getTranslation(
                        "already_connected_proxy")); // TODO: Cache this disconnect packet
                return;
            }

        }

        //BotFilter start
        if (bungee.getAegis().isOnChecking(getName())) {
            disconnect(bungee
                    .getTranslation("already_connected_proxy")); // TODO: Cache this disconnect packet
            return;
        }

        offlineId = UUID.nameUUIDFromBytes(("OfflinePlayer:" + getName()).getBytes(Charsets.UTF_8));

        PlayerSetUUIDEvent uuidEvent = new PlayerSetUUIDEvent(this, offlineId);
        //Because botfilter delayed a LoginEvent when player needs a check for a bot,
        //plugins can not change a uniqueId field via reflection in event, because BotFilter needs send
        //a LoginSuccess packet before a LoginEvent will be fired.

        //TODO
        /*if (this.isPremium()) {
            uuidEvent.setUniqueId(PremiumManager.getByName(getName()).getUuid());
        }*/

        bungee.getPluginManager().callEvent(uuidEvent);

        if (uuidEvent.getUniqueId() != null) {
            uniqueId = uuidEvent.getUniqueId();
        }

        boolean sendLoginSuccess = uuidEvent.getUniqueId() != null;

        if (uniqueId == null) {
            uniqueId = offlineId;
        }

        UserConnection userCon = new UserConnection(bungee, ch, getName(), InitialHandler.this);
        userCon.setCompressionThreshold(BungeeCord.getInstance().config.getCompressionThreshold());
        //userCon.init();

        sendLoginSuccess(sendLoginSuccess);

        if (getVersion() >= ProtocolConstants.MINECRAFT_1_8/* && getVersion() < ProtocolConstants.MINECRAFT_1_16*/) {
            if (!bungee.getAegis().needCheck(getName(), getAddress().getAddress())) {
                if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
                    bungee.getLogger().log(Level.INFO, "{0} has connected", InitialHandler.this);
                }

                if (this.bungee.getAegis().getAuthSystem() != null) {
                    sendLoginSuccess(
                            !sendLoginSuccess); //Send a loginSuccess if sendLoginSuccess is false
                    ch.setEncoderProtocol(Protocol.GAME);
                    ch.setDecoderProtocol(Protocol.BotFilter);

                    //final AuthUser authUser = this.bungee.getAegis().getAuthSystem().getUserManager().getUser(getUniqueId());

                    ch.getHandle().pipeline().get(HandlerBoss.class)
                            .setHandler(new AuthConnector(userCon, bungee.getAegis(), true));
                } else {
                    finishLogin(userCon, sendLoginSuccess); //if true, dont send again login success
                }
            } else {
                sendLoginSuccess(
                        !sendLoginSuccess); //Send a loginSuccess if sendLoginSuccess is false
                ch.setEncoderProtocol(Protocol.GAME);
                ch.setDecoderProtocol(Protocol.BotFilter);

                final Connector connector = new Connector(userCon, bungee.getAegis());
                // Aegis.getInstance().addConnection(connector);

                if (!Aegis.getInstance().addConnection(connector)) {
                    disconnect(BungeeCord.getInstance().getTranslation("already_connected_proxy")); // TODO: Cache this disconnect packet
                    return;
                }

                ch.getHandle().pipeline().get(HandlerBoss.class)
                        .setHandler(connector);
            }
        } else {
            if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
                bungee.getLogger().log(Level.INFO, "{0} has connected", InitialHandler.this);
            }
            finishLogin(userCon, sendLoginSuccess); //if true, dont send again login success
        }
    }

    public void finishLogin(UserConnection userCon, boolean ignoreLoginSuccess) {

        Callback<LoginEvent> complete = (result, error) -> {
            if (result.isCancelled()) {
                disconnect(result.getCancelReasonComponents());
                return;
            }
            if (ch.isClosed()) {
                return;
            }
            if (isInEventLoop()) {
                finnalyFinishLogin(userCon, ignoreLoginSuccess);
            } else {
                ch.getHandle().eventLoop().execute(() ->
                {
                    if (!ch.isClosing()) {
                        finnalyFinishLogin(userCon, ignoreLoginSuccess);
                    }
                });
            }
        };
        // fire login event
        bungee.getPluginManager().callEvent(new LoginEvent(InitialHandler.this, complete));
    }

    private void finnalyFinishLogin(UserConnection userCon, boolean ignoreLoginSuccess) {
        userCon.init();
        sendLoginSuccess(!ignoreLoginSuccess);
        ch.setProtocol(Protocol.GAME);

        ch.getHandle().pipeline().get(HandlerBoss.class)
                .setHandler(new UpstreamBridge(bungee, userCon)); //BotFilter

        bungee.getPluginManager().callEvent(new PostLoginEvent(userCon)); //BotFilter

        ServerInfo server;
        if (bungee.getReconnectHandler() != null) {
            server = bungee.getReconnectHandler().getServer(userCon);
        } else {
            server = AbstractReconnectHandler.getForcedHost(InitialHandler.this);
        }
        if (server == null) {
            server = bungee.getServerInfo(listener.getDefaultServer());
        }
        userCon.connect(server, null, true, ServerConnectEvent.Reason.JOIN_PROXY);
        thisState = State.FINISHED;
    }

    private void sendLoginSuccess(boolean send) {
        if (send) {
            //unsafe.sendPacket( new LoginSuccess( getUniqueId().toString(), getName() ) );
            if (ProtocolConstants.isAfterOrEq(getVersion(), ProtocolConstants.MINECRAFT_1_7_6)) {

                final UUID id;
                final AuthSystem authSystem = this.bungee.getAegis().getAuthSystem();
                if (authSystem != null) {
                    final PremiumUser user = PremiumManager.getByName(this.getName());
                    final AuthUser byName = authSystem.getUserManager().getByName(this.getName());
                    if (user != null && user.isPremium() && byName != null && byName
                            .isPremium() && byName.getPremiumAnswer() != PremiumAnswer.NO) {
                        //final AuthUser user = this.bungee.getAegis().getAuthSystem().getUserManager().getUser(getUniqueId());
                        //if (user != null && user.isPremium() && user.getPremiumAnswer() == PremiumAnswer.YES) {;
                        id = user.getUuid();
                    } else {
                        id = getUniqueId();
                    }
                } else {
                    id = getUniqueId();
                }

                unsafe.sendPacket(new LoginSuccess(id, getName())); // With dashes in between

            } else {
                unsafe.sendPacket(
                        new LoginSuccess(getUniqueId(), getName())); // Without dashes, for older clients.
            }
        }
    }

    private boolean isInEventLoop() {
        return ch.getHandle().eventLoop().inEventLoop();
    }

    private void wrongState(State current, State expected) {
        ch.close();
        final String address = getAddress().getAddress().getHostAddress();
        if (!Settings.IMP.AEGIS_SETTINGS.CLEAN_CONSOLE) {
            this.bungee.getLogger()
                    .log(Level.INFO, "[Aegis] {0} has sent invalid state during handshaking!"
                            + " Current state: " + current.name() + ", expected: " + expected.name(), address);
        }
    }

    @Override
    public void handle(LoginPayloadResponse response) throws Exception {
        disconnect("Unexpected custom LoginPayloadResponse");
    }

    //Aegis end

    @Override
    public void disconnect(String reason) {
        disconnect(TextComponent.fromLegacyText(reason));
    }

    @Override
    public void disconnect(final BaseComponent... reason) {
        if (thisState != State.STATUS && thisState != State.PING) {
            ch.delayedClose(new Kick(ComponentSerializer.toString(reason)));
        } else {
            ch.close();
        }
    }

    @Override
    public void disconnect(BaseComponent reason) {
        disconnect(new BaseComponent[]
                {
                        reason
                });
    }

    @Override
    public String getName() {
        return (name != null) ? name : (loginRequest == null) ? null : loginRequest.getData();
    }

    @Override
    public int getVersion() {
        return (handshake == null) ? -1 : handshake.getProtocolVersion();
    }

    @Override
    public InetSocketAddress getAddress() {
        return (InetSocketAddress) getSocketAddress();
    }

    @Override
    public SocketAddress getSocketAddress() {
        return ch.getRemoteAddress();
    }

    @Override
    public Unsafe unsafe() {
        return unsafe;
    }

    @Override
    public void setOnlineMode(boolean onlineMode) {
        Preconditions.checkState(thisState == State.USERNAME,
                "Can only set online mode status whilst state is username");
        this.onlineMode = onlineMode;
    }

    @Override
    public void setUniqueId(UUID uuid) {
        //Preconditions.checkState( thisState == State.USERNAME, "Can only set uuid while state is username" );
        //Preconditions.checkState( !onlineMode, "Can only set uuid when online mode is false" );
        this.uniqueId = uuid;
    }

    @Override
    public String getUUID() {
        return uniqueId.toString().replace("-", "");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');

        String currentName = getName();
        if (currentName != null) {
            sb.append(currentName);
            sb.append(',');
        }

        sb.append(getSocketAddress());
        sb.append("] <-> InitialHandler");

        return sb.toString();
    }

    @Override
    public boolean isConnected() {
        return !ch.isClosed();
    }

    private enum State {

        HANDSHAKE, STATUS, PING, USERNAME, ENCRYPT, FINISHED
    }
}
