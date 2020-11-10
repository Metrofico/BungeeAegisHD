package xyz.yooniks.aegis;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.score.Scoreboard;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.Varint21FrameDecoder;
import xyz.yooniks.aegis.auth.AuthSystem;
import xyz.yooniks.aegis.auth.handler.AuthConnector;
import xyz.yooniks.aegis.auth.thread.AegisAuthThread;
import xyz.yooniks.aegis.blackhole.Blackhole;
import xyz.yooniks.aegis.blacklist.Blacklist;
import xyz.yooniks.aegis.blacklist.BlacklistManager;
import xyz.yooniks.aegis.blacklist.Proxies;
import xyz.yooniks.aegis.blacklist.WhitelistManager;
import xyz.yooniks.aegis.caching.CachedCaptcha;
import xyz.yooniks.aegis.caching.PacketUtils;
import xyz.yooniks.aegis.caching.PacketUtils.KickType;
import xyz.yooniks.aegis.captcha.CaptchaGeneration;
import xyz.yooniks.aegis.config.Settings;
import xyz.yooniks.aegis.config.Settings.AEGIS_SETTINGS.BLACKLIST;
import xyz.yooniks.aegis.config.Settings.AEGIS_SETTINGS.LOGIN_PACKETS;
import xyz.yooniks.aegis.connection.AdvancedConnectionLimitter;
import xyz.yooniks.aegis.connection.AegisStatistics;
import xyz.yooniks.aegis.connection.login.ConnectionManager;
import xyz.yooniks.aegis.filter.AegisThread;
import xyz.yooniks.aegis.filter.Connector;
import xyz.yooniks.aegis.shell.*;
import xyz.yooniks.aegis.utils.GeoIp;
import xyz.yooniks.aegis.utils.ManyChecksUtils;
import xyz.yooniks.aegis.utils.Sql;
import xyz.yooniks.aegis.utils.WritePacketUtil;
import xyz.yooniks.aegis.vpn.VPNConnector;
import xyz.yooniks.aegis.vpn.VPNSystem;

import java.io.File;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class Aegis {

    public static final long ONE_MIN = 60000;

    private static Aegis instance;

    @Getter
    private final Map<String, Connector> connectedUsersSet = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, VPNConnector> connectedVPNUsersSet = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, AuthConnector> connectedAuthUsersSet = new ConcurrentHashMap<>();

    //UserName, Ip
    private final Map<String, String> userCache = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors
            .newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    @Getter
    private final Sql sql;
    @Getter
    private final GeoIp geoIp;

    private final CheckState normalState;
    private final CheckState attackState;

    private int botCounter = 0;
    private long lastAttack = 0;
    @Setter
    @Getter
    private long lastCheck = System.currentTimeMillis();

    @Getter
    private BotShellManager shellManager;
    @Getter
    private AegisStatistics statistics;
    @Getter
    private ConnectionManager connectionManager;
    //@Getter
    // private PingManager pingManager;

    @Getter
    private AuthSystem authSystem;

    @Getter
    private VPNSystem vpnSystem;

    @Getter
    private Proxies proxies;

    @Getter
    private Blacklist blacklist;

    @Getter
    private CaptchaFailed captchaFailed;

    @Getter
    private BlacklistManager blacklistManager;

    @Getter
    private AdvancedConnectionLimitter advancedConnectionLimitter;

    @Getter
    private WhitelistManager whitelistManager;


    public Aegis(boolean startup) {
        instance = this;

        Settings.IMP.reload(new File("Aegis", "config.yml"));

        DefinedPacket.fix_scoreboards = Settings.IMP.FIX_SCOREBOARDS;
        Scoreboard.DISABLE_DUBLICATE = Settings.IMP.FIX_SCOREBOARD_TEAMS;
        if (!CachedCaptcha.generated) {
            new Thread(() -> new CaptchaGeneration().generateImages()).start();
        }
        normalState = getCheckState(Settings.IMP.PROTECTION.NORMAL);
        attackState = getCheckState(Settings.IMP.PROTECTION.ON_ATTACK);
        PacketUtils.init();
        sql = new Sql(this);
        geoIp = new GeoIp(startup);
        shellManager = new BotShellManager();
        // pingManager = new PingManager();
        connectionManager = new ConnectionManager();

        final BotShellConfiguration shellConfiguration = new BotShellConfiguration();
        shellConfiguration.saveConfig();

        shellManager.addShells(
                BotShellManager.Loader.findShells(this, shellConfiguration.getConfig()));

        statistics = new AegisStatistics();
        statistics.startUpdating();
        AegisThread.start();

        if (Settings.IMP.AUTH.ENABLED) {
            AegisAuthThread.start();

            this.authSystem = new AuthSystem();
            this.authSystem.init();
        }
        if (Settings.IMP.ANTIVPN.ENABLED) {
            this.vpnSystem = new VPNSystem();
            this.vpnSystem.init(shellConfiguration);
        }

        this.blacklistManager = new BlacklistManager();
        this.whitelistManager = new WhitelistManager();
        new Thread(() -> {
            this.blacklistManager.init();
            this.whitelistManager.init();
        }).start();

        final BLACKLIST blacklist = Settings.IMP.AEGIS_SETTINGS.BLACKLIST;
        if (blacklist.ENABLED) {
            this.blacklist = new Blacklist(this.blacklistManager, blacklist.BLACKLIST_MODE);
            this.captchaFailed = new CaptchaFailed();

            if (blacklist.BLACKLIST_MODE == 0) {
                this.blacklist.asyncCommand(
                        Arrays.asList(blacklist.COMMANDS.INSTALL_IPSET_COMMAND,
                                blacklist.COMMANDS.CONFIGURE_IPSET_COMMAND));
            }
        }

        if (Settings.IMP.AEGIS_SETTINGS.ADVANCED_CHECKS.LIMIT_CONNECTIONS_PER_IP_WHEN_ATTACK) {
            this.advancedConnectionLimitter = new AdvancedConnectionLimitter();
        }

        /*this.proxies = new Proxies();
        try {
            this.proxies.init(new File("Aegis", "http_proxies.txt"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }*/

        final LOGIN_PACKETS loginPackets = Settings.IMP.AEGIS_SETTINGS.LOGIN_PACKETS;
        Varint21FrameDecoder.MIN_LENGTH_FIRST_PACKET = loginPackets.MIN_LENGTH_FIRST_PACKET;
        Varint21FrameDecoder.MAX_LENGTH_FIRST_PACKET = loginPackets.MAX_LENGTH_FIRST_PACKET;
        Varint21FrameDecoder.MIN_LENGTH_SECOND_PACKET = loginPackets.MIN_LENGTH_SECOND_PACKET;
        Varint21FrameDecoder.MAX_LENGTH_SECOND_PACKET = loginPackets.MAX_LENGTH_SECOND_PACKET;
        Varint21FrameDecoder.ALLOW_EMPTY_PACKETS = Settings.IMP.ALLOW_EMPTY_PACKETS;

        /*if (shellConfiguration.getConfig().getBoolean("firewall.enabled", true)) {
            final List<String> rules = shellConfiguration.getConfig()
                .getStringList("firewall.rules");
            this.executor
                .submit(() -> {
                    rules.forEach(rule -> {
                            try {
                                Runtime.getRuntime().exec(rule);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        });
                    BungeeCord.getInstance().getLogger().info("[Aegis] Executed " + rules.size() + " blacklist rules! Make sure that you have installed blacklist on your vps server (\"apt-get install blacklist\" is the command command), otherwise your server will be vulnerable to attack");
                });
        }*/

        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000L * 60 * 10);
                } catch (InterruptedException ex) {
                    continue;
                }

                if (this.authSystem != null) {
                    //final long start = System.currentTimeMillis();
                    // BungeeCord.getInstance().getLogger().info("[Aegis] Saving blacklisted ips and auth");
                    try {
                        this.authSystem.getDatabase().saveObjects();
                    } catch (SQLException ex) {
                        BungeeCord.getInstance().getLogger().log(Level.WARNING, "[Aegis]"
                                + " Could not save auth objects!", ex);
                    }
                    this.blacklistManager.saveToFile();
                    //BungeeCord.getInstance().getLogger().info(
                    //   "[Aegis] Saved blacklisted ips and auth objects in " + (System.currentTimeMillis()
                    //       - start) + "ms!");
                } else {

                    //final long start = System.currentTimeMillis();
                    //BungeeCord.getInstance().getLogger().info("[Aegis] Saving blacklisted ips..");
                    this.blacklistManager.saveToFile();
                    //BungeeCord.getInstance().getLogger().info(
                    //   "[Aegis] Saved blacklisted ips in" + (System.currentTimeMillis() - start) + " ms!");
                }
            }
        }).start();
    }

    public static Aegis getInstance() {
        return instance;
    }

    public void disable() {
        //nw czemu ale nie zapisuje
        this.blacklistManager.saveToFile();
        AegisThread.stop();
        if (this.authSystem != null) {
            this.authSystem.stop();
            AegisAuthThread.stop();

            for (AuthConnector connector : connectedAuthUsersSet.values()) {
                if (connector.getUserConnection() != null) {
                    connector.getUserConnection().disconnect("§c[Aegis] §aProxy reload");
                }
            }
        }
        for (Connector connector : connectedUsersSet.values()) {
            if (connector.getUserConnection() != null) {
                connector.getUserConnection().disconnect("§c[Aegis] §aProxy reload");
            }
            connector.setState(CheckState.FAILED);
        }
        if (this.vpnSystem != null) {
            this.vpnSystem.stop();
        }
        shellManager.clean();
        connectedUsersSet.clear();
        geoIp.close();
        sql.close();
        ManyChecksUtils.clear();
        executor.shutdownNow();
    }

    /**
     * Сохраняет игрока в памяти и в датебазе
     *
     * @param userName Имя игрока
     * @param address  InetAddress игрока
     */
    public void saveUser(String userName, InetAddress address) {
        userName = userName.toLowerCase();
        String ip = address.getHostAddress();
        userCache.put(userName, ip);
        if (sql != null) {
            sql.saveUser(userName, ip);
        }
    }

    public void saveUser(String userName, InetAddress address, boolean sqlExecute) {
        userName = userName.toLowerCase();
        String ip = address.getHostAddress();
        userCache.put(userName, ip);
        if (sql != null && sqlExecute) {
            sql.saveUser(userName, ip);
        }
    }

    /**
     * Удаляет игрока из памяти
     *
     * @param userName Имя игрока, которого следует удалить из памяти
     */
    public void removeUser(String userName) {
        userName = userName.toLowerCase();
        userCache.remove(userName);
    }

    /**
     * Добавляет игрока в мапу
     */
    public boolean addConnection(Connector connector) {
        return connectedUsersSet.putIfAbsent(connector.getName(), connector) == null;
    }

    public boolean addAuthConnection(AuthConnector connector) {
        return connectedAuthUsersSet.putIfAbsent(connector.getName(), connector) == null;
    }

    public void addVpnConnection(VPNConnector connector) {
        connectedVPNUsersSet.put(connector.getName(), connector);
    }

    /**
     * Убирает игрока из мапы.
     *
     * @param name      Имя игрока (lowercased)
     * @param connector Объект коннектора
     * @throws RuntimeException Имя игрока и коннектор null
     */
    public void removeConnection(String name, Connector connector) {
        name = name == null ? connector == null ? null : connector.getName() : name;
        if (name != null) {
            connectedUsersSet.remove(name);
        } else {
            throw new RuntimeException("Name and connector is null");
        }
    }

    public void removeAuthConnection(String name, AuthConnector connector) {
        name = name == null ? connector == null ? null : connector.getName() : name;
        if (name != null) {
            connectedAuthUsersSet.remove(name);
        } else {
            throw new RuntimeException("Name and connector is null");
        }
    }

    public void removeVPNConnection(String name, VPNConnector connector) {
        name = name == null ? connector == null ? null : connector.getName() : name;
        if (name != null) {
            connectedVPNUsersSet.remove(name);
        } else {
            throw new RuntimeException("Name and connector is null");
        }
    }

    /**
     * Увеличивает счетчик ботов
     */
    public void incrementBotCounter() {
        botCounter++;
    }

    /**
     * Количество подключений на проверке
     *
     * @return количество подключений на проверке
     */
    public int getOnlineOnFilter() {
        return connectedUsersSet.size() + this.connectedAuthUsersSet.size();
    }

    public int getAuthUsers() {
        return this.connectedAuthUsersSet.size();
    }

    /**
     * @return количество пользователей, которые прошли проверку
     */
    public int getUsersCount() {
        return userCache.size();
    }

    /**
     * Проверяет нужно ли игроку проходить проверку
     *
     * @param userName Имя игрока
     * @param address  InetAddress игрока
     * @return Нужно ли юзеру проходить проверку
     */
    public boolean needCheck(String userName, InetAddress address) {
        final String hostAddress = address.getHostAddress();

        if (this.whitelistManager.isWhitelisted(hostAddress) || this.whitelistManager
                .isWhitelisted(userName)) {
            return false;
        }

        String ip = userCache.get(userName.toLowerCase());
        if (ip != null && !ip.equals(hostAddress)) {
            return Settings.IMP.PROTECTION.VERIFY_AGAIN_ON_IP_CHANGE;
        }

        return ip == null || (Settings.IMP.FORCE_CHECK_ON_ATTACK && isUnderAttack());
    }

    public boolean doNeedCheckWithoutForceCheck(String userName, InetAddress address) {
        final String hostAddress = address.getHostAddress();

        if (this.whitelistManager.isWhitelisted(hostAddress) || this.whitelistManager
                .isWhitelisted(userName)) {
            return false;
        }

        String ip = userCache.get(userName.toLowerCase());
        if (ip != null && !ip.equals(hostAddress)) {
            return Settings.IMP.PROTECTION.VERIFY_AGAIN_ON_IP_CHANGE;
        }

        return ip == null;
    }

    public boolean needCheck(String address) {
        return !this.userCache.containsValue(address);
    }

    public boolean isChecked(String address) {
        return this.userCache.values()
                .stream()
                .anyMatch(ip -> ip.equals(address));
    }

    /**
     * Проверяет, находиться ли игрок на проверке
     *
     * @param name Имя игрока которого нужно искать на проверке
     * @return Находиться ли игрок на проверке
     */
    public boolean isOnChecking(String name) {
        return connectedUsersSet.containsKey(name.toLowerCase()) || connectedAuthUsersSet
                .containsKey(name.toLowerCase());
    }

    /**
     * Проверяет есть ли в текущий момент бот атака
     *
     * @return true Если в текущий момент идёт атака
     */
    public boolean isUnderAttack() {
        long currTime = System.currentTimeMillis();
        if (currTime - lastAttack < Settings.IMP.PROTECTION_TIME) {
            return true;
        }
        long diff = currTime - lastCheck;
        if ((diff <= ONE_MIN) && botCounter >= Settings.IMP.PROTECTION_THRESHOLD) {
            lastAttack = System.currentTimeMillis();
            lastCheck -= 61000;
            return true;
        } else if (diff >= ONE_MIN) {
            botCounter = 0;
            lastCheck = System.currentTimeMillis();
        }
        return false;
    }

    public boolean checkBigPing(double ping) {
        int mode = isUnderAttack() ? 1 : 0;
        return ping != -1 && Settings.IMP.PING_CHECK.MODE != 2 && (Settings.IMP.PING_CHECK.MODE == 0
                || Settings.IMP.PING_CHECK.MODE == mode) && ping >= Settings.IMP.PING_CHECK.MAX_PING;
    }

    public boolean isGeoIpEnabled() {
        int mode = isUnderAttack() ? 1 : 0;
        return geoIp.isEnabled() && (Settings.IMP.GEO_IP.MODE == 0 || Settings.IMP.GEO_IP.MODE == mode);
    }

    public boolean checkGeoIp(InetAddress address) {

        return !geoIp.isAllowed(address);
    }

    public void checkAsyncIfNeeded(InitialHandler handler) {
        this.statistics.addConnectionPerSecond();

        InetAddress address = handler.getAddress().getAddress();
        ChannelWrapper ch = handler.getCh();
        int version = handler.getVersion();
        BungeeCord bungee = BungeeCord.getInstance();

        final Settings settings = Settings.IMP;

        final boolean needCheck = doNeedCheckWithoutForceCheck(handler.getName(), address);
        if (needCheck && !settings.AEGIS_SETTINGS.IGNORE_CAPTCHA_GENERATION_KICK && !CachedCaptcha.generated) {
            PacketUtils.kickPlayer(
                    PacketUtils.createKickPacket(settings.MESSAGES.PROXY_NOT_LOADED.replace("{PERCENT}",
                            String.valueOf(CaptchaGeneration.getEnablingPercent()))), ch);
            return;
        }

        final String hostAddress = address.getHostAddress();
        final String name = handler.getName() == null ? hostAddress : handler.getName();
        if (needCheck && !whitelistManager.isWhitelisted(hostAddress) && !whitelistManager.isWhitelisted(name)) {

            if (Blackhole.INSTANCE.isEnabled()) {
                getStatistics().addBlockedConnection();

                final ChannelWrapper wrapper = handler.getCh();
                WritePacketUtil.writePacket(Blackhole.INSTANCE.getKickPacket(), wrapper);
                wrapper.close();

                handler.disconnect();

                getBlacklist().asyncDrop(hostAddress);
                return;
            }

            if (vpnSystem != null) {
                if (vpnSystem.getVpnRequester().isBlockedIp(hostAddress)) {
                    PacketUtils
                            .kickPlayer(PacketUtils.createKickPacket(settings.ANTIVPN.KICK_MESSAGE),
                                    ch);
                    return;
                }
            }

            if (ManyChecksUtils.isManyChecks(address)) {
                PacketUtils.kickPlayer(KickType.MANYCHECKS, Protocol.LOGIN, ch, version);

                if (!settings.AEGIS_SETTINGS.CLEAN_CONSOLE) {
                    bungee.getLogger()
                            .log(Level.INFO, "(Aegis) [{0}] disconnected: Too many checks in 10 min",
                                    address);
                }
                return;
            }

            if (settings.AEGIS_SETTINGS.ADVANCED_CHECKS.CHECK_DIFFERENCE_STRING_BYTES
                    && handler.getName() != null) {
                if (handler.getName().length() != handler.getName().getBytes().length) {

                    if (!settings.AEGIS_SETTINGS.CLEAN_CONSOLE) {
                        bungee.getLogger().log(Level.INFO,
                                "(Aegis) [{0}, {1}] disconnected: name.length() and nameBytes.length are different!",
                                new Object[]{address, handler.getName()});
                    }
                    statistics.addBlockedConnection();
                    return;
                }
            }

            if (!settings.AEGIS_SETTINGS.BYPASS_IPS.contains(hostAddress)) {
                for (BotShell shell : this.shellManager.getShells()) {
                    if (!shell.shouldCheck(this.statistics)) {
                        continue;
                    }
                    if (!shell.pass(handler)) {
                        PacketUtils.kickPlayer(handler, shell, ch);

                        if (!settings.AEGIS_SETTINGS.CLEAN_CONSOLE) {
                            bungee.getLogger().log(Level.INFO,
                                    "[Aegis Bot-Login-Filter] [{0}] disconnected: Did not pass antibot shell (" + shell.getName() + ")"
                                            + "| CPS: {1} PPS: {2}",
                                    new Object[]{address, this.statistics.getConnectionsPerSecond(),
                                            this.statistics.getPingsPerSecond()});
                        }
                        statistics.addBlockedConnection();

                        if (shell instanceof BotShellRunnable) {
                            ((BotShellRunnable) shell).advancedAction(hostAddress, statistics);
                        }
                        return;
                    }
                }
            }
        } else {
            if (!settings.AEGIS_SETTINGS.CLEAN_CONSOLE) {
                bungee.getLogger().log(Level.INFO,
                        "[Aegis Bot-Login-Filter] [{0}] is whitelisted, he bypasses every antibot check! :)",
                        handler.getName());
            }
        }
        /*ServerPingUtils ping = getServerPingUtils();
        if ( ping.needCheck() && ping.needKickOrRemove( address ) )
        {
            PacketUtils.kickPlayer( KickType.PING, Protocol.LOGIN, ch, version );
            bungee.getLogger().log( Level.INFO, "(Aegis) [{0}] disconnected: The player did not ping the server", address.getHostAddress() );
            return;
        }*/

        /*if ( bungee.getConnectionThrottle() != null && bungee.getConnectionThrottle().throttle( address ) )
        {
            PacketUtils.kickPlayer( KickType.THROTTLE, Protocol.LOGIN, ch, version ); //Aegis
            bungee.getLogger().log( Level.INFO, "[{0}] disconnected: Connection is throttled", address.getHostAddress() );
            return;
        }
         */
        if (isGeoIpEnabled()) {
            executor.execute(() ->
            {
                if (checkGeoIp(address)) {
                    PacketUtils.kickPlayer(KickType.COUNTRY, Protocol.LOGIN, ch, version);
                    if (!settings.AEGIS_SETTINGS.CLEAN_CONSOLE) {
                        bungee.getLogger()
                                .log(Level.INFO, "(Aegis) [{0}] disconnected: Country is not allowed",
                                        hostAddress);
                    }
                    return;
                }
                handler.delayedHandleOfLoginRequset();
            });
        } else {
            handler.delayedHandleOfLoginRequset();
        }
        connectionManager.login(handler);
    }

    public CheckState getCurrentCheckState() {
        return isUnderAttack() ? attackState : normalState;
    }

    private CheckState getCheckState(int mode) {
        switch (mode) {
            case 0:
                return CheckState.ONLY_CAPTCHA;
            case 1:
                return CheckState.CAPTCHA_POSITION;
            default:
                return CheckState.CAPTCHA_ON_POSITION_FAILED;
        }
    }

    public static enum CheckState {
        ONLY_POSITION,
        ONLY_CAPTCHA,
        CAPTCHA_POSITION,
        CAPTCHA_ON_POSITION_FAILED,
        SUCCESSFULLY,
        FAILED
    }
}
