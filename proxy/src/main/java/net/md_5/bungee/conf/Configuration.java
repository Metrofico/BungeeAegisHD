package net.md_5.bungee.conf;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import gnu.trove.map.TMap;
import lombok.Getter;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ProxyConfig;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ConfigurationAdapter;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.util.CaseInsensitiveMap;
import net.md_5.bungee.util.CaseInsensitiveSet;
import xyz.yooniks.aegis.config.Settings;
import xyz.yooniks.aegis.config.Settings.AEGIS_SETTINGS;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Core configuration for the proxy.
 */
@Getter
public class Configuration implements ProxyConfig {

    /**
     * Time before users are disconnected due to no network activity.
     */
    private int timeout = 30000;
    /**
     * UUID used for metrics.
     */
    private String uuid = UUID.randomUUID().toString();
    /**
     * Set of all listeners.
     */
    private Collection<ListenerInfo> listeners;
    /**
     * Set of all servers.
     */
    private TMap<String, ServerInfo> servers;
    /**
     * Should we check minecraft.net auth.
     */
    private boolean onlineMode = true;

    private boolean disableEntityMetadataRewrite = false;
    /**
     * Whether we log proxy commands to the proxy log
     */
    private boolean logCommands;
    private boolean logPings = true;
    private int playerLimit = -1;
    private Collection<String> disabledCommands;
    private int throttle = 2000;
    private int throttleLimit = 5;
    private boolean ipForward;
    private Favicon favicon;
    private int compressionThreshold = 256;
    private boolean preventProxyConnections;
    private boolean forgeSupport;
    private boolean injectCommands;


    public void load() {

        ConfigurationAdapter adapter = ProxyServer.getInstance().getConfigurationAdapter();
        adapter.load();

        Settings.IMP.reload(new File("Aegis", "config.yml"));

        final AEGIS_SETTINGS settings = Settings.IMP.AEGIS_SETTINGS;

        File fav = new File("server-icon.png");
        if (fav.exists()) {
            try {
                favicon = Favicon.create(ImageIO.read(fav));
            } catch (IOException | IllegalArgumentException ex) {
                ProxyServer.getInstance().getLogger().log(Level.WARNING, "Could not load server icon", ex);
            }
        }

        listeners = adapter.getListeners();

        //Aegis
        // this.disableEntityMetadataRewrite = adapter.getBoolean("aegis.stop-entity-metadata-rewrite", this.disableEntityMetadataRewrite);
        if (!adapter.getString("aegis.license", "empty").equals("empty")) {
            settings.LICENSE = adapter.getString("aegis.license", settings.LICENSE);
        }

        /*if (adapter.getBoolean("aegis.clean-console", settings.CLEAN_CONSOLE)) {
            settings.CLEAN_CONSOLE = adapter.getBoolean("aegis.clean-console", settings.CLEAN_CONSOLE);
        }*/

        if (adapter.getList("aegis.bypassIps", null) != null) {
            settings.BYPASS_IPS = (Collection<String>) adapter
                    .getList("aegis.bypassIps", settings.BYPASS_IPS);
        }

        if (adapter.getBoolean("aegis.online-uuids-support", settings.ONLINE_UUIDS_SUPPORT)) {
            settings.ONLINE_UUIDS_SUPPORT = adapter.getBoolean("aegis.online-uuids-support",
                    settings.ONLINE_UUIDS_SUPPORT);
        }

        if (adapter.getString("aegis.game-version", null) != null) {
            settings.GAME_VERSION = adapter.getString("aegis.game-version", settings.GAME_VERSION);
        }

        if (adapter.getBoolean("aegis.allow-v1_7-support", settings.ALLOW_V1_7_SUPPORT)) {
            settings.ALLOW_V1_7_SUPPORT = adapter.getBoolean("aegis.allow-v1_7-support",
                    settings.ALLOW_V1_7_SUPPORT);
        }
        if (adapter.getBoolean("aegis.print-exceptions-from-disconnects", settings.PRINT_EXCEPTIONS)) {
            settings.PRINT_EXCEPTIONS = adapter.getBoolean("aegis.print-exceptions-from-disconnects",
                    settings.PRINT_EXCEPTIONS);
        }

        if (adapter.getBoolean("aegis.print-stacktraces-of-exceptions-from-disconnects",
                settings.PRINT_STACKTRACES_FROM_EXCEPTIONS)) {
            settings.PRINT_STACKTRACES_FROM_EXCEPTIONS = adapter
                    .getBoolean("aegis.print-stacktraces-of-exceptions-from-disconnects",
                            settings.PRINT_STACKTRACES_FROM_EXCEPTIONS);
        }

        Settings.IMP.save(new File("Aegis", "config.yml"));

        timeout = adapter.getInt("timeout", timeout);
        uuid = adapter.getString("stats", uuid);
        onlineMode = adapter.getBoolean("online_mode", onlineMode);
        logCommands = adapter.getBoolean("log_commands", logCommands);
        logPings = adapter.getBoolean("log_pings", logPings);
        playerLimit = adapter.getInt("player_limit", playerLimit);
        throttle = adapter.getInt("connection_throttle", throttle);
        throttleLimit = adapter.getInt("connection_throttle_limit", throttleLimit);
        ipForward = adapter.getBoolean("ip_forward", ipForward);
        compressionThreshold = adapter.getInt("network_compression_threshold", compressionThreshold);
        preventProxyConnections = adapter
                .getBoolean("prevent_proxy_connections", preventProxyConnections);
        forgeSupport = adapter.getBoolean("forge_support", forgeSupport);
        injectCommands = adapter.getBoolean("inject_commands", injectCommands);
        if (injectCommands) {
            System.setProperty("net.md-5.bungee.protocol.register_commands", "true");
        }

        disabledCommands = new CaseInsensitiveSet((Collection<String>) adapter
                .getList("disabled_commands", Arrays.asList("disabledcommandhere")));
        disabledCommands.remove("bungee"); //Aegis
        disabledCommands.remove("aegis"); //Aegis

        Preconditions.checkArgument(listeners != null && !listeners.isEmpty(), "No listeners defined.");

        loadServers(adapter, false);

        for (ListenerInfo listener : listeners) {
            for (int i = 0; i < listener.getServerPriority().size(); i++) {
                String server = listener.getServerPriority().get(i);
                Preconditions
                        .checkArgument(servers.containsKey(server), "Server %s (priority %s) is not defined",
                                server, i);
            }
            for (String server : listener.getForcedHosts().values()) {
                if (!servers.containsKey(server)) {
                    ProxyServer.getInstance().getLogger()
                            .log(Level.WARNING, "Forced host server {0} is not defined", server);
                }
            }
        }
    }

    //Aegis start
    public void loadServers(ConfigurationAdapter adapter, boolean load) {
        if (load) {
            adapter.load();
        }
        Map<String, ServerInfo> newServers = adapter.getServers();
        Preconditions.checkArgument(newServers != null && !newServers.isEmpty(), "No servers defined");

        if (servers == null) {
            servers = new CaseInsensitiveMap<>(newServers);
        } else {
            final Map<String, ServerInfo> oldServers = this.getServersCopy();
            for (final ServerInfo oldServer : oldServers.values()) {
                final ServerInfo newServer = (ServerInfo) newServers.get(oldServer.getName());
                if ((newServer == null || !oldServer.getAddress().equals(newServer.getAddress())) && !oldServer.getPlayers().isEmpty()) {
                    BungeeCord.getInstance().getLogger().info("Moving players off of server: " + oldServer.getName());
                    for (final ProxiedPlayer player : oldServer.getPlayers()) {
                        final ListenerInfo listener = player.getPendingConnection().getListener();
                        final String destinationName = (newServers.get(listener.getDefaultServer()) == null) ? listener.getDefaultServer() : listener.getFallbackServer();
                        final ServerInfo destination = (ServerInfo) newServers.get(destinationName);
                        if (destination == null) {
                            BungeeCord.getInstance().getLogger().severe("Couldn't find server " + listener.getDefaultServer() + " or " + listener.getFallbackServer() + " to put player " + player.getName() + " on");
                            player.disconnect(ChatColor.translateAlternateColorCodes('&', "&cCould not connect to a default or fallback server, please try again later: &6Not found on reload"));
                        } else {
                            final PendingConnection connection = player.getPendingConnection();
                            player.connect(destination, (success, cause) -> {
                                if (!success) {
                                    BungeeCord.getInstance().getLogger().log(Level.WARNING, "Failed to connect " + connection.getName() + " to " + oldServer.getName(), cause);
                                    connection.disconnect(ChatColor.translateAlternateColorCodes('&', "&cCould not connect to a default or fallback server, please try again later: &6" + cause.getCause().getClass().getName()));
                                }
                            });
                        }
                    }
                } else {
                    newServers.put(oldServer.getName(), oldServer);
                }
            }
            this.servers = (TMap<String, ServerInfo>) new CaseInsensitiveMap(newServers);
        }
    }
    // Aegis end

    @Override
    @Deprecated
    public String getFavicon() {
        return getFaviconObject().getEncoded();
    }

    @Override
    public Favicon getFaviconObject() {
        return favicon;
    }

    @Override
    public Map<String, ServerInfo> getServersCopy() {
        return ImmutableMap.copyOf(this.servers);
    }

}
