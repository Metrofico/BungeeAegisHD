package net.md_5.bungee;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.util.ResourceLeakDetector;
import jline.console.ConsoleReader;
import lombok.Synchronized;
import net.md_5.bungee.api.*;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.config.ConfigurationAdapter;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.chat.*;
import net.md_5.bungee.command.*;
import net.md_5.bungee.compress.CompressFactory;
import net.md_5.bungee.conf.Configuration;
import net.md_5.bungee.conf.YamlConfig;
import net.md_5.bungee.forge.ForgeConstants;
import net.md_5.bungee.log.BungeeLogger;
import net.md_5.bungee.log.LoggingOutputStream;
import net.md_5.bungee.module.ModuleManager;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.Chat;
import net.md_5.bungee.protocol.packet.PluginMessage;
import net.md_5.bungee.query.RemoteQuery;
import net.md_5.bungee.scheduler.BungeeScheduler;
import net.md_5.bungee.util.CaseInsensitiveMap;
import org.fusesource.jansi.AnsiConsole;
import xyz.yooniks.aegis.Aegis;
import xyz.yooniks.aegis.command.AegisCommand;
import xyz.yooniks.aegis.config.Settings;
import xyz.yooniks.aegis.filter.AegisThread;
import xyz.yooniks.aegis.utils.FakeOnlineUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main BungeeCord proxy class.
 */
public class BungeeCord extends ProxyServer {

    /**
     * Configuration.
     */
    public final Configuration config = new Configuration();
    /**
     * Plugin manager.
     */
    public final PluginManager pluginManager;
    public final Gson gson = new GsonBuilder()
            .registerTypeAdapter(BaseComponent.class, new ComponentSerializer())
            .registerTypeAdapter(TextComponent.class, new TextComponentSerializer())
            .registerTypeAdapter(TranslatableComponent.class, new TranslatableComponentSerializer())
            .registerTypeAdapter(KeybindComponent.class, new KeybindComponentSerializer())
            .registerTypeAdapter(ScoreComponent.class, new ScoreComponentSerializer())
            .registerTypeAdapter(SelectorComponent.class, new SelectorComponentSerializer())
            .registerTypeAdapter(ServerPing.PlayerInfo.class, new PlayerInfoSerializer())
            .registerTypeAdapter(Favicon.class, Favicon.getFaviconTypeAdapter()).create();
    public final Gson gsonLegacy = new GsonBuilder()
            .registerTypeAdapter(BaseComponent.class, new ComponentSerializer())
            .registerTypeAdapter(TextComponent.class, new TextComponentSerializer())
            .registerTypeAdapter(TranslatableComponent.class, new TranslatableComponentSerializer())
            .registerTypeAdapter(ServerPing.PlayerInfo.class,
                    new PlayerInfoSerializer(ProtocolConstants.MINECRAFT_1_7_2))
            .registerTypeAdapter(Favicon.class, Favicon.getFaviconTypeAdapter()).create();
    /**
     * locations.yml save thread.
     */
    private final Timer saveThread = new Timer("Reconnect Saver");
    /**
     * Server socket listener.
     */
    private final Collection<Channel> listeners = new HashSet<>();
    /**
     * Fully qualified connections.
     */
    private final Map<String, UserConnection> connections = new CaseInsensitiveMap<>();
    //private final Timer metricsThread = new Timer( "Metrics Thread" );
    // Used to help with packet rewriting
    private final Map<UUID, UserConnection> connectionsByOfflineUUID = new HashMap<>();
    private final Map<UUID, UserConnection> connectionsByUUID = new HashMap<>();
    private final ReadWriteLock connectionLock = new ReentrantReadWriteLock();
    private final Collection<String> pluginChannels = new HashSet<>();
    private final File pluginsFolder = new File("plugins");
    private final BungeeScheduler scheduler = new BungeeScheduler();
    private final ConsoleReader consoleReader;
    private final Logger logger;
    private final ModuleManager moduleManager = new ModuleManager();
    /**
     * Current operation state.
     */
    public volatile boolean isRunning;
    public EventLoopGroup bossEventLoopGroup, workerEventLoopGroup, queryEventLoopGroup; //Aegis
    /**
     * Localization bundle.
     */
    private ResourceBundle baseBundle;
    private ResourceBundle customBundle;
    private ReconnectHandler reconnectHandler;
    private ConfigurationAdapter configurationAdapter = new YamlConfig();
    private ConnectionThrottle connectionThrottle;
    private Aegis aegis; //Aegis

    {
        registerChannel("BungeeCord");
    }

    //@Getter
    //private static BungeeConfig bungeeConfig;


    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    public BungeeCord() throws IOException {
        // Java uses ! to indicate a resource inside of a jar/zip/other container. Running Bungee from within a directory that has a ! will cause this to muck up.
        Preconditions.checkState(new File(".").getAbsolutePath().indexOf('!') == -1,
                "Cannot use BungeeCord in directory with ! in path.");

        System.setSecurityManager(new BungeeSecurityManager());

        try {
            baseBundle = ResourceBundle.getBundle("messages");
        } catch (MissingResourceException ex) {
            baseBundle = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        }
        reloadMessages();

        // This is a workaround for quite possibly the weirdest bug I have ever encountered in my life!
        // When jansi attempts to extract its natives, by default it tries to extract a specific version,
        // using the loading class's implementation version. Normally this works completely fine,
        // however when on Windows certain characters such as - and : can trigger special behaviour.
        // Furthermore this behaviour only occurs in specific combinations due to the parsing done by jansi.
        // For example test-test works fine, but test-test-test does not! In order to avoid this all together but
        // still keep our versions the same as they were, we set the override property to the essentially garbage version
        // BungeeCord. This version is only used when extracting the libraries to their temp folder.
        System.setProperty("library.jansi.version", "BungeeCord");

        AnsiConsole.systemInstall();
        consoleReader = new ConsoleReader();
        consoleReader.setExpandEvents(false);
        consoleReader.addCompleter(new ConsoleCommandCompleter(this));

        new File("logs").mkdirs();
        logger = new BungeeLogger("BungeeCord", "logs/proxy.log", consoleReader);
        System.setErr(new PrintStream(new LoggingOutputStream(logger, Level.SEVERE), true));
        System.setOut(new PrintStream(new LoggingOutputStream(logger, Level.INFO), true));

        logger.info("[Aegis] =-=-=-= Registering commands =-=-=-=");
        pluginManager = new PluginManager(this);
        // getPluginManager().registerCommand( null, new CommandReload() );
        getPluginManager().registerCommand(null, new CommandEnd());
        //getPluginManager().registerCommand( null, new CommandIP() );
        getPluginManager().registerCommand(null, new CommandBungee());
        getPluginManager().registerCommand(null, new CommandPerms());
        getPluginManager().registerCommand(null, new AegisCommand()); //Aegis
        getPluginManager().registerCommand(null, new CommandReload());
        //getPluginManager().registerCommand( null, new CommandReloadServers() ); //Aegis

        //getPluginManager().registerCommand( null, new CommandServerEditor() ); //Aegis
        //getPluginManager().registerCommand( null, new CommandGroup() ); //Aegis

        if (!Boolean.getBoolean("net.md_5.bungee.native.stop")) {
            if (EncryptionUtil.nativeFactory.load()) {
                logger.info("Using mbed TLS based native cipher.");
            } else {
                logger.info("Using standard Java JCE cipher.");
            }
            if (CompressFactory.zlib.load()) {
                logger.info("Using zlib based native compressor.");
            } else {
                logger.info("Using standard Java compressor.");
            }
        }
    }

    public static BungeeCord getInstance() {
        return (BungeeCord) ProxyServer.getInstance();
    }

    @Override
    public Configuration getConfig() {
        return config;
    }

    @Override
    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public ResourceBundle getBaseBundle() {
        return baseBundle;
    }

    public void setBaseBundle(ResourceBundle baseBundle) {
        this.baseBundle = baseBundle;
    }

    public ResourceBundle getCustomBundle() {
        return customBundle;
    }

    public void setCustomBundle(ResourceBundle customBundle) {
        this.customBundle = customBundle;
    }

    public EventLoopGroup getBossEventLoopGroup() {
        return bossEventLoopGroup;
    }

    public void setBossEventLoopGroup(EventLoopGroup bossEventLoopGroup) {
        this.bossEventLoopGroup = bossEventLoopGroup;
    }

    public EventLoopGroup getWorkerEventLoopGroup() {
        return workerEventLoopGroup;
    }

    public void setWorkerEventLoopGroup(EventLoopGroup workerEventLoopGroup) {
        this.workerEventLoopGroup = workerEventLoopGroup;
    }

    public EventLoopGroup getQueryEventLoopGroup() {
        return queryEventLoopGroup;
    }

    public void setQueryEventLoopGroup(EventLoopGroup queryEventLoopGroup) {
        this.queryEventLoopGroup = queryEventLoopGroup;
    }

    public Timer getSaveThread() {
        return saveThread;
    }

    public Collection<Channel> getListeners() {
        return listeners;
    }

    public Map<String, UserConnection> getConnections() {
        return connections;
    }

    public Map<UUID, UserConnection> getConnectionsByOfflineUUID() {
        return connectionsByOfflineUUID;
    }

    public Map<UUID, UserConnection> getConnectionsByUUID() {
        return connectionsByUUID;
    }

    public ReadWriteLock getConnectionLock() {
        return connectionLock;
    }

    @Override
    public ReconnectHandler getReconnectHandler() {
        return reconnectHandler;
    }

    @Override
    public void setReconnectHandler(ReconnectHandler reconnectHandler) {
        this.reconnectHandler = reconnectHandler;
    }

    @Override
    public ConfigurationAdapter getConfigurationAdapter() {
        return configurationAdapter;
    }

    @Override
    public void setConfigurationAdapter(ConfigurationAdapter configurationAdapter) {
        this.configurationAdapter = configurationAdapter;
    }

    public Collection<String> getPluginChannels() {
        return pluginChannels;
    }

    @Override
    public File getPluginsFolder() {
        return pluginsFolder;
    }

    @Override
    public BungeeScheduler getScheduler() {
        return scheduler;
    }

    public ConsoleReader getConsoleReader() {
        return consoleReader;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    public Gson getGson() {
        return gson;
    }

    public Gson getGsonLegacy() {
        return gsonLegacy;
    }

    public ConnectionThrottle getConnectionThrottle() {
        return connectionThrottle;
    }

    public void setConnectionThrottle(ConnectionThrottle connectionThrottle) {
        this.connectionThrottle = connectionThrottle;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public Aegis getAegis() {
        return aegis;
    }

    public void setAegis(Aegis aegis) {
        this.aegis = aegis;
    }

    /**
     * Start this proxy instance by loading the configuration, plugins and starting the connect
     * thread.
     *
     * @throws Exception
     */
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public void start() throws Exception {
        System.setProperty("io.netty.selectorAutoRebuildThreshold",
                "0"); // Seems to cause Bungee to stop accepting connections
        if (System.getProperty("io.netty.leakDetectionLevel") == null) {
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED); // Eats performance
        }

        this.aegis = new Aegis(true); //Hook Aegis into Bungee
        new FakeOnlineUtils(); //Init fake online
        AegisThread.startCleanUpThread(); //Aegis

        bossEventLoopGroup = PipelineUtils.newEventLoopGroup(0,
                new ThreadFactoryBuilder().setNameFormat("Netty Boss IO Thread #%1$d")
                        .build()); //Aegis //WaterFall backport
        workerEventLoopGroup = PipelineUtils.newEventLoopGroup(0,
                new ThreadFactoryBuilder().setNameFormat("Netty Worker IO Thread #%1$d")
                        .build());//Aegis //WaterFall backport
        queryEventLoopGroup = PipelineUtils.newEventLoopGroup(1,
                new ThreadFactoryBuilder().setNameFormat("Query Netty IO Thread #%1$d").build());//Aegis

        logger.info("[Aegis] =-=-=-= Loading modules =-=-=-=");
        File moduleDirectory = new File("modules");
        moduleManager.load(this, moduleDirectory);
        logger.info("[Aegis] =-=-=-= Loading plugins =-=-=-=");
        pluginManager.detectPlugins(moduleDirectory);
        pluginsFolder.mkdir();
        pluginManager.detectPlugins(pluginsFolder);

        pluginManager.loadPlugins();
        config.load();

        if (config.isForgeSupport()) {
            registerChannel(ForgeConstants.FML_TAG);
            registerChannel(ForgeConstants.FML_HANDSHAKE_TAG);
            registerChannel(ForgeConstants.FORGE_REGISTER);

            getLogger().warning(
                    "MinecraftForge support is currently unmaintained and may have unresolved issues. Please use at your own risk.");
        }

        isRunning = true;

        pluginManager.enablePlugins();

        if (config.getThrottle() > 0) {
            connectionThrottle = new ConnectionThrottle(config.getThrottle(), config.getThrottleLimit());
        }
        startListeners();

        saveThread.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (getReconnectHandler() != null) {
                    getReconnectHandler().save();
                }
            }
        }, 0, TimeUnit.MINUTES.toMillis(5));

        //aegis
        //final File configFile = new File("config.yml");
        //bungeeConfig = new BungeeConfig(ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile),
        //   ConfigurationProvider.getProvider(YamlConfiguration.class), configFile);

        //metricsThread.scheduleAtFixedRate( new Metrics(), 0, TimeUnit.MINUTES.toMillis( Metrics.PING_INTERVAL ) );
    }

    public void startListeners() {
        for (final ListenerInfo info : config.getListeners()) {
            if (info.isProxyProtocol()) {
                getLogger().log(Level.WARNING,
                        "Using PROXY protocol for listener {0}, please ensure this listener is adequately firewalled.",
                        info.getHost());
            }


            ChannelFutureListener listener = new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        listeners.add(future.channel());
                        getLogger().log(Level.INFO, "Listening on {0}", info.getHost());
                    } else {
                        getLogger()
                                .log(Level.WARNING, "Could not bind to host " + info.getHost(), future.cause());
                    }
                }
            };
            new ServerBootstrap()
                    .channel(PipelineUtils.getServerChannel())
                    .option(ChannelOption.SO_REUSEADDR, true) // TODO: Move this elsewhere!
                    .childAttr(PipelineUtils.LISTENER, info)
                    .childHandler(PipelineUtils.SERVER_CHILD)
                    .group(bossEventLoopGroup, workerEventLoopGroup) //Aegis //WaterFall backport
                    .localAddress(info.getHost())
                    .bind().addListener(listener);

            if (info.isQueryEnabled()) {
                ChannelFutureListener bindListener = new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            listeners.add(future.channel());
                            getLogger().log(Level.INFO, "Started query on {0}", future.channel().localAddress());
                        } else {
                            getLogger()
                                    .log(Level.WARNING, "Could not bind to host " + info.getHost(), future.cause());
                        }
                    }
                };
                new RemoteQuery(this, info).start(PipelineUtils.getDatagramChannel(),
                        new InetSocketAddress(info.getHost().getAddress(), info.getQueryPort()),
                        queryEventLoopGroup, bindListener); //Aegis
            }
        }
    }

    public void stopListeners() {
        for (Channel listener : listeners) {
            getLogger().log(Level.INFO, "Closing listener {0}", listener);
            try {
                listener.close().syncUninterruptibly();
            } catch (ChannelException ex) {
                getLogger().severe("Could not close listen thread");
            }
        }
        listeners.clear();
    }

    @Override
    public void stop() {
        stop(getTranslation("restart"));
    }

    @Override
    public synchronized void stop(final String reason) {
        if (!isRunning) {
            return;
        }
        isRunning = false;

        new Thread("Shutdown Thread") {
            @Override
            @SuppressFBWarnings("DM_EXIT")
            @SuppressWarnings("TooBroadCatch")
            public void run() {
                stopListeners();
                getLogger().info("Closing pending connections");

                connectionLock.readLock().lock();
                try {
                    getLogger().log(Level.INFO, "Disconnecting {0} connections", connections.size());
                    for (UserConnection user : connections.values()) {
                        user.disconnect(reason);
                    }
                } finally {
                    connectionLock.readLock().unlock();
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                }

                if (reconnectHandler != null) {
                    getLogger().info("Saving reconnect locations");
                    reconnectHandler.save();
                    reconnectHandler.close();
                }
                saveThread.cancel();
                //metricsThread.cancel();

                // TODO: Fix this shit
                getLogger().info("Disabling plugins");
                for (Plugin plugin : Lists.reverse(new ArrayList<>(pluginManager.getPlugins()))) {
                    try {
                        plugin.onDisable();
                        for (Handler handler : plugin.getLogger().getHandlers()) {
                            handler.close();
                        }
                    } catch (Throwable t) {
                        getLogger().log(Level.SEVERE,
                                "Exception disabling plugin " + plugin.getDescription().getName(), t);
                    }
                    getScheduler().cancel(plugin);
                    plugin.getExecutorService().shutdownNow();
                }

                getLogger().info("Closing IO threads");
                bossEventLoopGroup.shutdownGracefully(); //Aegis //WaterFall backport
                workerEventLoopGroup.shutdownGracefully(); //Aegis //WaterFall backport
                queryEventLoopGroup.shutdownGracefully(); //Aegis
                while (true) //Aegis //WaterFall backport
                {
                    try {
                        bossEventLoopGroup
                                .awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);//Aegis //WaterFall backport
                        workerEventLoopGroup.awaitTermination(Long.MAX_VALUE,
                                TimeUnit.NANOSECONDS); //Aegis //WaterFall backport
                        queryEventLoopGroup.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS); //Aegis
                        break;
                    } catch (InterruptedException ignored) {
                    }
                }

                getLogger().info("Thank you and goodbye");
                // Need to close loggers after last message!
                for (Handler handler : getLogger().getHandlers()) {
                    handler.close();
                }
                System.exit(0);
            }
        }.start();
    }

    /**
     * Broadcasts a packet to all clients that is connected to this instance.
     *
     * @param packet the packet to send
     */
    public void broadcast(DefinedPacket packet) {
        connectionLock.readLock().lock();
        try {
            for (UserConnection con : connections.values()) {
                con.unsafe().sendPacket(packet);
            }
        } finally {
            connectionLock.readLock().unlock();
        }
    }

    @Override
    public String getName() {
        return "Aegis";
    }

    @Override
    public String getVersion() {
        return (BungeeCord.class.getPackage().getImplementationVersion() == null) ? "unknown"
                : BungeeCord.class.getPackage().getImplementationVersion();
    }

    public void reloadMessages() {
        File file = new File("messages.properties");
        if (file.isFile()) {
            try (FileReader rd = new FileReader(file)) {
                customBundle = new PropertyResourceBundle(rd);
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE, "Could not load custom messages.properties", ex);
            }
        }
    }

    @Override
    public String getTranslation(String name, Object... args) {
        String translation = "<translation '" + name + "' missing>";
        try {
            translation = MessageFormat.format(
                    customBundle != null && customBundle.containsKey(name) ? customBundle.getString(name)
                            : baseBundle.getString(name), args);
        } catch (MissingResourceException ex) {
        }
        return translation;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<ProxiedPlayer> getPlayers() {
        connectionLock.readLock().lock();
        try {
            return Collections.unmodifiableCollection(new HashSet(connections.values()));
        } finally {
            connectionLock.readLock().unlock();
        }
    }

    @Override
    public int getOnlineCount() {
        return connections.size();
    }

    //Aegis start
    @Override
    public int getOnlineCountBF(boolean fake) {
        int online = connections.size();
        if (fake) {
            online = FakeOnlineUtils.getInstance().getFakeOnline(online);
        }

        if (Settings.IMP.SHOW_ONLINE) {
            online += aegis.getOnlineOnFilter();
        } else {
            if (this.aegis.getAuthSystem() != null) {
                online += aegis.getAuthUsers();
            }
        }

        return online;
    }
    //Aegis end

    @Override
    public ProxiedPlayer getPlayer(String name) {
        connectionLock.readLock().lock();
        try {
            return connections.get(name);
        } finally {
            connectionLock.readLock().unlock();
        }
    }

    public UserConnection getPlayerByOfflineUUID(UUID name) {
        connectionLock.readLock().lock();
        try {
            return connectionsByOfflineUUID.get(name);
        } finally {
            connectionLock.readLock().unlock();
        }
    }

    @Override
    public ProxiedPlayer getPlayer(UUID uuid) {
        connectionLock.readLock().lock();
        try {
            return connectionsByUUID.get(uuid);
        } finally {
            connectionLock.readLock().unlock();
        }
    }

    @Override
    public Map<String, ServerInfo> getServers() {
        return config.getServers();
    }

    @Override
    public ServerInfo getServerInfo(String name) {
        return getServers().get(name);
    }

    @Override
    @Synchronized("pluginChannels")
    public void registerChannel(String channel) {
        pluginChannels.add(channel);
    }

    @Override
    @Synchronized("pluginChannels")
    public void unregisterChannel(String channel) {
        pluginChannels.remove(channel);
    }

    @Override
    @Synchronized("pluginChannels")
    public Collection<String> getChannels() {
        return Collections.unmodifiableCollection(pluginChannels);
    }

    public PluginMessage registerChannels(int protocolVersion) {
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_13) {
            return new PluginMessage("minecraft:register",
                    Util.format(Iterables.transform(pluginChannels, PluginMessage.MODERNISE), "\00")
                            .getBytes(Charsets.UTF_8), false);
        }

        return new PluginMessage("REGISTER",
                Util.format(pluginChannels, "\00").getBytes(Charsets.UTF_8), false);
    }

    @Override
    public int getProtocolVersion() {
        return ProtocolConstants.SUPPORTED_VERSION_IDS
                .get(ProtocolConstants.SUPPORTED_VERSION_IDS.size() - 1);
    }

    @Override
    public String getGameVersion() {
        return ProtocolConstants.SUPPORTED_VERSIONS.get(0) + "-" + ProtocolConstants.SUPPORTED_VERSIONS
                .get(ProtocolConstants.SUPPORTED_VERSIONS.size() - 1);
    }

    @Override
    public ServerInfo constructServerInfo(String name, InetSocketAddress address, String motd,
                                          boolean restricted) {
        return new BungeeServerInfo(name, address, motd, restricted);
    }

    @Override
    public CommandSender getConsole() {
        return ConsoleCommandSender.getInstance();
    }

    @Override
    public void broadcast(String message) {
        broadcast(TextComponent.fromLegacyText(message));
    }

    @Override
    public void broadcast(BaseComponent... message) {
        getConsole().sendMessage(BaseComponent.toLegacyText(message));
        broadcast(new Chat(ComponentSerializer.toString(message)));
    }

    @Override
    public void broadcast(BaseComponent message) {
        getConsole().sendMessage(message.toLegacyText());
        broadcast(new Chat(ComponentSerializer.toString(message)));
    }

    public void addConnection(UserConnection con) {
        connectionLock.writeLock().lock();
        try {
            connections.put(con.getName(), con);
            connectionsByUUID.put(con.getUniqueId(), con);
            connectionsByOfflineUUID.put(con.getPendingConnection().getOfflineId(), con);
        } finally {
            connectionLock.writeLock().unlock();
        }
    }

    public void removeConnection(UserConnection con) {
        connectionLock.writeLock().lock();
        try {
            // TODO See #1218
            if (connections.get(con.getName()) == con) {
                connections.remove(con.getName());
                connectionsByUUID.remove(con.getUniqueId());
                connectionsByOfflineUUID.remove(con.getPendingConnection().getOfflineId());
            }
        } finally {
            connectionLock.writeLock().unlock();
        }
    }

    @Override
    public Collection<String> getDisabledCommands() {
        return config.getDisabledCommands();
    }

    @Override
    public Collection<ProxiedPlayer> matchPlayer(final String partialName) {
        Preconditions.checkNotNull(partialName, "partialName");

        ProxiedPlayer exactMatch = getPlayer(partialName);
        if (exactMatch != null) {
            return Collections.singleton(exactMatch);
        }

        return Sets.newHashSet(Iterables.filter(getPlayers(), new Predicate<ProxiedPlayer>() {

            @Override
            public boolean apply(ProxiedPlayer input) {
                return (input == null) ? false : input.getName().toLowerCase(Locale.ROOT)
                        .startsWith(partialName.toLowerCase(Locale.ROOT));
            }
        }));
    }

    @Override
    public Title createTitle() {
        return new BungeeTitle();
    }
}
