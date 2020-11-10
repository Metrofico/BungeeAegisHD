package xyz.yooniks.aegis.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Settings extends Config {

    @Ignore
    public static final Settings IMP = new Settings();

    @Comment(
            {
                    "Aegis",
                    "https://minemen.com/resources/216/ - purchase here",
                    "https://minemen.com/resources/175/ - our Spigot plugin AntiCrash",
                    "https://discord.gg/AmvcUfn - our discord server",
                    "The best protection ever. Protection against powerful bot attacks (fast + slow), nullping crashers (bungee exploits/smashers)",
                    "Aegis also contains lots of features from well-known BotFilter by leymooo + we have lots of our own features - and the most important thing: exploit patches"
            })
    @Final
    public final String HELP = "Discord: yooniks#0289";
    @Final
    @Ignore
    public String AEGIS_VERSION = "8.7.4 GENJUTSU";
    @Create
    public MESSAGES MESSAGES;
    @Create
    public QUEUE QUEUE;
    @Create
    public GEO_IP GEO_IP;
    @Create
    public PING_CHECK PING_CHECK;
    @Create
    public PROTECTION PROTECTION;
    @Create
    public AEGIS_SETTINGS AEGIS_SETTINGS;
    @Create
    public ANTIVPN ANTIVPN;
    @Create
    public AUTH AUTH;
    @Create
    public SQL SQL;
    @Comment(
            {
                    "How many players / bots should go in 1 minute for protection to be activated",
                    "Recommended options when there is no advertising: ",
                    "Up to 150 online - 25, up to 250 - 30, up to 350 - 35, up to 550 - 40.45, above - adjust to yourself ",
                    "It is recommended to increase these values ​​during an advertisement or when a current is flowing."
            })
    public int PROTECTION_THRESHOLD = 30;
    @Comment("How long is automatic protection active? In milliseconds. 1 second = 1000")
    public int PROTECTION_TIME = 120000;
    @Comment("Check whether the bot on entering the server during a bot attack, regardless of whether the check passed or not")
    public boolean FORCE_CHECK_ON_ATTACK = true;
    @Comment("Show online with filter (will show players that are being verified in motd), we do not want to show bots in players count so it is disabled by default")
    public boolean SHOW_ONLINE = false;
    @Comment("How much time does the player have to go through the defense. In milliseconds. 1 second = 1000")
    public int TIME_OUT = 12700;
    @Comment(
            {
                    "Enable / Disable compatibility with old plugins that use ScoreBoard on a bungee?",
                    "Set to false if there are problems with new plugins."
            })
    public boolean FIX_SCOREBOARDS = false;
    @Comment("Fix for: 'Team 'xxx' already exist in this scoreboard'")
    public boolean FIX_SCOREBOARD_TEAMS = true;
    public boolean ALLOW_EMPTY_PACKETS = false;

    public void reload(File file) {
        load(file);
        save(file);
    }

    @Comment("Do not use '\\ n', use %nl%")
    public static class MESSAGES {

        public String PROXY_NOT_LOADED = "&cAegis &8> &7Server is loading, please wait! &a{PERCENT}&8/&c900";
        public String BUNGEECORD_COMMAND = "&9The most powerful protection for your proxy server. /aegis";

        public String PREFIX = "&cAegis ";
        public String STATISTICS = "&c&nStatistics&r&7 -> &cTotal blocked: &6{TOTAL-BLOCKED} &7| &cLOGINs/S: &6{CPS} &7| &cPINGs/S: &6{PPS} &8(&cTotal CPS: &6{TOTAL-CPS}&8) &7| &cCH: &6{CHECKING} &a&l{FANCY-CHAR}";

        public String CHECKING = "%prefix% &8[AntiBot] &7>> Please wait...";
        public String CHECKING_CAPTCHA = "%prefix% &8[AntiBot] &7>> [AntiBot] &7Enter the number from &cthe image&c in the chat. (captcha)";
        public String CHECKING_CAPTCHA_WRONG = "%prefix% &8[AntiBot] &7>> &cYou entered the captcha incorrectly, please try again. ";
        public String SUCCESSFULLY = "%prefix% &8[AntiBot] &7>> &7Check &apassed&7, enjoy the game.";
        public String KICK_MANY_CHECKS = "%prefix%%nl%%nl%&cSuspicious activity noticed from your IP Address%nl%%nl%&6Try again in 10 minutes.%nl%&c(Make sure you do not use cheats like NoFall & AntiKnock)";
        public String KICK_NOT_PLAYER = "%prefix%%nl%%nl%&cYou did not pass the test, maybe you are a bot or you have cheats (nofall or antiknockback)%nl%&7&oIf it is a mistake, please try again.";
        public String KICK_COUNTRY = "%prefix%%nl%%nl%&cYour country is banned on the server.";
        public String KICK_BIG_PING = "%prefix%%nl%%nl%&cYou have a very high ping, most likely you are a bot.";
        @Comment(
                {
                        "Title%nl%Subtitle", "Leave blank to stop ( edit: CHECKING_TITLE = \"\" )",
                        "Turning off titles can slightly improve performance."
                })
        public String CHECKING_TITLE = "&r&cAegis &8[&7AntiBot&8]%nl%&cVerifying...";
        public String CHECKING_TITLE_SUS = "&cAntiBot > &7Check passed%nl%&7Have a &enice game&7!";
        public String CHECKING_TITLE_CAPTCHA = " %nl%&7Enter captcha in chat!";

        public String BOT_BEHAVIOUR = "%prefix%%nl%%nl%&cYour are acting like a bot, please try again";

        public String BLACKHOLE_KICK = "&cAegis &8[Blackhole] >> &cBlackhole mode is enabled, you are blacklisted from this server now.";
    }

    @Comment("Soon!")
    public static class QUEUE {

        public boolean ENABLED = false;
        public String BYPASS_PERMISSION = "aegis.queue.bypass";
        public String ACTIONBAR_MESSAGE = "&cQueue&8: &7You are &c{PLACE}&7 in queue to join &c{TARGET}&7!";
    }

    @Comment("Enable or Disable GeoIp")
    public static class GEO_IP {

        @Comment(
                {
                        "When verification is working",
                        "0 - Always",
                        "1 - Only during the bot attack",
                        "2 - Never"
                })
        public int MODE = 2;
        @Comment(
                {
                        "How exactly does GeoIp work",
                        "0 - White list(Only those countries in the list can enter)",
                        "1 - Black list(Only countries that are not in the list can enter)"
                })
        public int TYPE = 0;
        @Comment(
                {"Please DO NOT USE THAT if you do not have GEOIP bought, you should use country blocker from anti-vpn configuration that is below.",
                        "URL to download GEOIP",
                        "Change the link if it does not working.",
                        "The filename must end with .mmdb or be packed in .tar.gz"
                })
        public String GEOIP_DOWNLOAD_URL = "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-Country&license_key=hcLI5gctJWDpDfez&suffix=tar.gz";
        @Comment("Allowed country (default: Asean)")
        public List<String> ALLOWED_COUNTRIES = Arrays
                .asList("TH", "LA", "MM", "KH", "VN", "PH", "MY", "SG", "ID", "BN, PL");
    }

    @Comment("Enable or stop high ping checking")
    public static class PING_CHECK {

        @Comment(
                {
                        "When verification is working",
                        "0 - Always",
                        "1 - Only during the bot attack",
                        "2 - Never"
                })
        public int MODE = 1;
        @Comment("Maximum allowed ping")
        public int MAX_PING = 350;
    }

    @Comment(
            {
                    "Setting how protection will work",
                    "0 - Only check with captcha",
                    "1 - Drop check + captcha",
                    "2 - Drop check, if failed, then captcha",
                    "I heard that there are some new bots that bypass drop/falling test so i recommend to use 1 on big servers"
            })
    public static class PROTECTION {

        @Comment("Should we verify player again when he passed falling test but his ip changed?")
        public boolean VERIFY_AGAIN_ON_IP_CHANGE = true;

        @Comment("Operation mode when no attack")
        public int NORMAL = 2;
        @Comment("Operation mode during the attack")
        public int ON_ATTACK = 1;

        @Comment({"Should we listen for Settings packet?",
                "Bots very often do not send that thing, normal clients send it always, to inform server about client settings",
                "If you set to TRUE -> better protection against very good bypassing bots"})
        public boolean CHECK_SETTINGS = false;
    }

    public static class AEGIS_SETTINGS {

        @Comment("Put your license key here, to get the license key you have to contact me on discord: yooniks#0289 and send proof of payment from paypal")
        public String LICENSE = "yourLicenseKeyHere";

        @Comment("Should Aegis support forge? It may cause disconnect issues on 1.7.x")
        public boolean FORGE_SUPPORT = false;

        @Comment("Do you want pretty clean console? When aegis blocks bots etc. it will not show it in the console if it's set to true.")
        public boolean CLEAN_CONSOLE = true;
        @Comment("Should we disable entity metadata rewriting? It helps when there is one error with <unknown metadata type> and players are kicked..")
        public boolean DISABLE_ENTITY_METADATA_REWRITE = false;
        @Comment("List of bypass/skipped ips - e.g. your other bungee ip (if you run multi proxies) or your HAProxy.")
        public Collection<String> BYPASS_IPS = Arrays
                .asList("127.0.0.1", "localhost", "yourHAProxy-AndOtherBungeeIP");
        @Comment("Do you want to allow 1.7 clients to join your server?")
        public boolean ALLOW_V1_7_SUPPORT = true;
        @Comment({"Matters only when AUTH-SYSTEM is disabled! Otherwise this option is useless.",
                "Should we support online uuids? It will automatically stop falling/drop test (falling verification).",
                "If you do not use our own auth-system and you have plugin that auto-logins premium players like FastLogin or JHPremium then set it to true."})
        public boolean ONLINE_UUIDS_SUPPORT = false;
        @Comment("Should we print exceptions from disconnects? Set to false to have cleaner console.")
        public boolean PRINT_EXCEPTIONS = true;
        @Comment("Should we print stacktraces of exceptions from disconnects? If you are being disconnected when you join etc. it helps to solve problems thanks to logs file. Set to false for cleaner console.")
        public boolean PRINT_STACKTRACES_FROM_EXCEPTIONS = false;
        @Comment("Game version? In some clients it is shown right next to the motd/players count. You can set it to your server name like UltraPVP")
        public String GAME_VERSION = "Aegis";

        public boolean CHANGE_MOJANG_SESSION_URL = false;
        public String CHANGED_MOJANG_SESSION_URL = "https://sessionserver.mojang.com/session/minecraft/hasJoined?username={ENCNAME}&serverId={SERVERID}";

        @Comment({"Blocked protocols of versions (If you want e.g to block 1.14.4 players, "
                + "this option is for you!",
                "Minecraft protocols (versions): https://wiki.vg/Protocol_version_numbers for example: 1.8.8 is 47"})
        public List<Integer> BLOCKED_PROTOCOLS = new ArrayList<>(Arrays.asList(9999));
        @Comment("Kick message when your version is blocked")
        public String PROTOCOL_BLOCKED = "&cYour &6minecraft version&c is blocked! We allow only X.X-X.X!";

        @Comment("Delayed disconnections in auth-system? Can look better")
        public boolean DELAYED_DISCONNECTIONS = false;

        public int MIN_LENGTH_NAME = 3;
        public int MAX_LENGTH_NAME = 16;

        @Comment("Should we limit serverConnector connects? Like 1 connect per second to server (per ip)")
        public boolean LIMIT_SERVERCONNECTOR_CONNECTS = false;
        @Comment("In millis")
        public int LIMIT_SERVERCONNECTOR_TIME = 1000;

        public boolean IGNORE_CAPTCHA_GENERATION_KICK = false;

        public int MAX_DELAYED_PLUGIN_MESSAGES_LIST_SIZE = 128;

        @Create
        public LOGIN_PACKETS LOGIN_PACKETS;
        @Create
        public ADVANCED_CHECKS ADVANCED_CHECKS;
        @Create
        public BLACKLIST BLACKLIST;

        @Comment("Advanced configuration, you can change min and max lengths of login packets if you have any bugs with current ones")
        public static class LOGIN_PACKETS {

            public int MIN_LENGTH_FIRST_PACKET = 1 + 1 + (1 + 1) + 2 + 1 - 4;
            public int MAX_LENGTH_FIRST_PACKET = 1 + 5 + (3 + 255 * 4) + 2 + 1 + 3;
            public int MIN_LENGTH_SECOND_PACKET = 3;
            public int MAX_LENGTH_SECOND_PACKET = 1 + (1 + 16 * 4);
        }

        public static class ADVANCED_CHECKS {

            @Comment("Should we check if string.length() == stringBytes.length? China chars etc. will be blocked")
            public boolean CHECK_DIFFERENCE_STRING_BYTES = false;

            public boolean NAME_PATTERN_CHECK = true;
            public String ALLOWED_PATTERN = "^[a-zA-Z0-9_.-]*$";

            public boolean ENCRYPTION_LIMITTER = true;

            @Comment({
                    "Should we block every new connection when cps are higher than X? Set to -1 to disable.",
                    "If you have any issues with AuthSmasher and it is not fixed yet please set it to something like 150 or bigger to block only authsmasher, not bot attacks"})
            public int BLOCK_NEW_CONNECTIONS_WHEN_CPS_IS_HIGHER_THAN = -1;

            @Comment("Should we limit connections per ip for 3 seconds if BLOCK_NEW_CONNECTIONS_WHEN_CPS_IS_HIGHER_THAN exceed?")
            public boolean LIMIT_CONNECTIONS_PER_IP_WHEN_ATTACK = true;

            @Comment("If LIMIT_CONNECTIONS_PER_IP_WHEN_ATTACK is true, how high is the limit?")
            public int LIMIT_CONNECTIONS_PER_IP_WHEN_ATTACK_LIMIT = 5;

            @Comment({"Is your server being attacked with AuthSmasher?",
                    "Do you have cracked players only? (Like online-mode=false and there is no plugin like FastLogin or JPremium)",
                    "If yes - please enable this option, it will surely prevent AuthSmasher, because they use this packet to crash the server while.... packet is useless so we can remove it!"})
            public boolean REMOVE_ENCRYPTION = false;
        }

        @Comment({"Blacklist configuration"})
        public static class BLACKLIST {

            @Comment("Should we block addresses/ips if they send invalid packets? (null ping / bungee smasher etc)")
            public boolean ENABLED = true;

            @Comment({"Blacklist mode!", "0 - IPSet + File + Runtime (blacklist.txt)",
                    "1 - Runtime + File",
                    "Default: 1 - Runtime block and file should be enough. But I recommend to use 0 if your server is under attack all the time, or file blacklist is not enough"})
            public int BLACKLIST_MODE = 1;

            @Create
            public FILE FILE;
            @Create
            public COMMANDS COMMANDS;
            @Comment("Should AntiBot block connections with blacklist if we are sure that it is bot?")
            public boolean BLOCK_WHEN_SURE = true;
            @Comment({"Max captcha failures in 5 minutes, per one ip address",
                    "If address exceed this limit it will be blocked",
                    "Works only if connections per sec > 2 or pings per second > 10"})
            public int MAX_CAPTCHA_FAILURES = 5;
            @Comment({"If ip cannot go through ping-shell 10 times, per one ip address",
                    "Then it will be blocked with blacklist",
                    "Works only when connections per second > 3 or pings per second > 20"})
            public int MAX_PING_SHELL_FAILURES = 10;
            @Comment({"If ip cannot go through name-match more than 1 time, per one ip address",
                    "Then it will be blocked with blacklist",
                    "Works only when connections per second > 5 or pings per second > 50"})
            public int MAX_NAME_MATCH_FAILURES = 1;

            @Comment("Configuration of blacklist in file (blacklist.txt)")
            public static class FILE {

                @Comment("When file is empty (0 blacklisted proxies) should we download proxies from yooniks website to your blacklist.txt?")
                public boolean LOAD_FROM_URL_WHEN_FILE_IS_EMPTY = false;

                @Comment({"I VERY RECOMMEND TO USE THAT!!!", "Works only when IPSet is installed",
                        "Run these commands in root, not bungee! ",
                        "To install IPSet: apt-get install ipset",
                        "After installing IPSet you have to create blacklist list: ipset create -! blacklist hash:ip hashsize 15000",
                        "The next step is to tell iptables that we will use IPSet blacklist for incoming connections:",
                        "iptables -I INPUT -m set --match-set blacklist src -j DROP",
                        "iptables -I FORWARD -m set --match-set blacklist src -j DROP",
                        "That is all! More about that shit: https://confluence.jaytaala.com/display/TKB/Using+ipset+to+block+IP+addresses+-+firewall",
                        "If you do that all the blacklist size will be much faster and bad connections will be blocked much faster which blocks every attack much better, before getting connections through bungeecord, which will reduce bungee attacks!"})
                public boolean BLOCK_WITH_IPSET_WHEN_FILE_LOADED = false;
            }

            public static class COMMANDS {

                @Comment({"Command that is being executed when we block an address",
                        "If you are using Windows (instead of Unix-like system), replace the command with that one:",
                        "cmd /c netsh advfirewall firewall add rule name=blacklist dir=in interface=any protocol=TPC localport=25565 action=block remoteip={ip}/32"})
                public String BLOCK_COMMAND = "ipset -! -A blacklist {ADDRESS}";

                @Comment({"A command that we will use to install IPSet on your machine.",
                        "apt --yes install ipset    <--- APT (e.g. Debian, Ubuntu)",
                        "yum -y install ipset   <--- YUM (e.g. CentOS, older versions of Fedora)",
                        "dnf -assumeyes install ipset <--- DNF (e.g. newer versions of Fedora)"})
                public String INSTALL_IPSET_COMMAND = "apt-get --yes install ipset";

                @Comment("A command that we will use to configure/setup the blacklist with ipset")
                public String CONFIGURE_IPSET_COMMAND = "ipset create -! blacklist hash:ip hashsize 15000";
            }
        }
    }

    @Comment({"AntiVPN settings!",
            "You can configure websites (url), keys for websites etc. in aegis_antibot.yml"})
    public static class ANTIVPN {

        public boolean ENABLED = false;
        public String TITLE_CHECKING = "&7&l[AntiVPN]%nl%&cChecking...";
        public String TITLE_CHECKED = "&7[AntiVPN] &aCheck passed%nl%&aHave a nice game!";
        public String CHAT_CHECKING = "&cAegis &8[AntiVPN] > &7Checking vpn..";
        public String CHAT_CHECKED = "&cAegis &8[AntiVPN] > &7You are &anot using VPN&7! Enjoy the game!";

        public String KICK_MESSAGE = "&c&lAnti&4&lVPN%nl%&cYour ip is blocked! You are using vpn/proxy!";
        public String TOO_LONG = "&c&lAnti&4&lVPN%nl%&cToo long verification!";

        @Create
        public COUNTRY_CHECKER COUNTRY_CHECKER;

        @Comment("Country checker settings")
        public static class COUNTRY_CHECKER {

            @Comment({"Should we block some countries? Is it feature enabled?",
                    "To check the country of player we use website: http://ip-api.com/"})
            public boolean ENABLED = false;
            @Comment("List of allowed countries (country codes!)")
            public List<String> ALLOWED_COUNTRIES = Arrays.asList("DE", "US", "FR", "PL");
            @Comment({"URL of website", "You can put your paid key here.",
                    "Default limit of requests per minute is 150, if it exceed the limit we allow the player."})
            public String URL = "http://ip-api.com/json/{ADDRESS}";
            @Comment({"Limit of requests per minute",
                    "If you have any paid key you can change that value to higher one."})
            public int REQUESTS_LIMIT = 150;
        }
    }

    @Comment("Auth system configuration")
    public static class AUTH {

        @Comment("Is auth system enabled?")
        public boolean ENABLED = false;

        @Comment("If player uses /remember after he register/login he will be automatically logged in the next time he join, if his ip does not change")
        public boolean ALLOW_LOGIN_SESSIONS = true;

        @Create
        public MYSQL MYSQL;

        @Create
        public MESSAGES MESSAGES;

        @Comment("Auth messages")
        public static class MESSAGES {

            public String LOADING_USER_CHAT = "&cAegis &8[Auth] > &cLoading&7 your data..";
            public String LOADING_USER_TITLE = "&6&l[Auth]%nl%&cLoading your data..";

            public String LOADED_USER = "&cAegis &8[Auth] > &aLoaded&7 your data!";

            public String LOGIN_SESSION_LOGGED_IN = "&cAegis &8[Auth] > &6Login session&7, logged in automatically!";
            public String LOGIN_SESSION_IP_CHANGED = "&cAegis &8[Auth] > &6Login session&7, was enabled but your ip has changed, you have to login again and use &6/remember&7!";

            public String LOGIN_SESSION_ENABLED = "&cAegis &8[Auth] > &6Login session&7 has been &aenabled&7!";
            public String LOGIN_SESSION_DISABLED = "&cAegis &8[Auth] > &6Login session&7 has been &cdisabled&7!";

            public String PREMIUM_MESSAGE_CHAT = "&cAegis &8[Auth] > &7You are&a premium&7 user! Moving to the server automatically..";
            public String PREMIUM_MESSAGE_TITLE = "&6&l[Auth]%nl%&cYou are premium user!";

            public String LOGIN_MESSAGE = "&cAegis &8[Auth] > &7Please login by using &6/login [password]&7!";
            public String REGISTER_MESSAGE = "&cAegis &8[Auth] > &7Please register by using &6/register [password] [password]&7!";

            public String LOGGED_MESSAGE = "&cAegis &8[Auth] > &7You have been &alogged-in&7 successfully!";
            public String REGISTERED_MESSAGE = "&cAegis &8[Auth] > &7Your account has been &aregistered&7 successfully!";

            public String LOGGED_MESSAGE_TITLE = "&6&l[Auth]%nl%&cLogged in!";

            public String LOGIN_MESSAGE_TITLE = "&6&l[Auth]%nl%&cPlease log-in. /login.";
            public String REGISTER_MESSAGE_TITLE = "&6&l[Auth]%nl%&cPlease register your acc.";

            public String YOUR_DATA_IS_BEING_LOADED = "&cAegis &8[Auth] > &cYou cannot use this command! Your data is not loaded yet.";

            public String PREMIUM_USER_CANNOT_DO_THAT = "&cAegis &8[Auth] > &7You are &6premium user&7, you cannot do that!";

            public String PASSWORDS_DO_NOT_MATCH = "&cAegis &8[Auth] > &cPasswords do not match!";

            public String KICK_MESSAGE_TOO_LONG = "&6&lAegis%nl%&cYou have to log-in in 30 seconds!";

            public String PREMIUM_NEED_RELOGIN = "&6&l[Aegis Auth]%nl%&cYou are &6PREMIUM &cuser!\n&cPlease re-login to finish verification!";

            public String WRONG_NAME = "&6&lAegis%nl%&cYou should join with nickname &6{OLD-NAME}&c!";
            public String CHANGEPASSWORD_SAME_PASS = "&cAegis &8[Auth] > &cCurrent and new password are the same!";
            public String CHANGEPASSWORD_USAGE = "&cAegis &8[Auth] > &cCorrect usage: &6/changepassword [current password] [new password]";
            public String CHANGEPASSWORD_ERROR_PREMIUM = "&cAegis &8[Auth] > &cYou are premium user! You cannot change your password because it doesn't exist.";

            public String CHANGEPASSWORD_WRONG_PASS = "&cAegis &8[Auth] > &cTyped password is incorrect!";
            public String CHANGEPASSWORD_SUCCESS = "&cAegis &8[Auth] > &cYou have changed your &6password&c!";
            public String CHANGEPASSWORD_NOT_LOGGED = "&cAegis &8[Auth] > &cYou are not logged!";

            @Comment("If you want every player to /register and /login, even if they are premium players and you don't want them to be auto-logged just set it to false.")
            public boolean ALLOW_PREMIUM_USERS = true;
            @Comment("Should we ask user if he is seriously premium user? Maybe it's cracked player with cracked launcher that just uses premium nickname? Then he will not be able to join with cracked launcher when he use premium nickname. If you want to allow these cracked people to use premium nicknames and ask them if they are premium then just set it to true.")
            public boolean ASK_USERS_IF_PREMIUM_WHEN_PREMIUM = true;
            public String ASK_USER_WHEN_PREMIUM_CHAT = "&cAegis &8[Auth] --> &6Are you premium user? &7We detected that your nickname is premium name, if you are using premium launcher please type &aYES&7 on the chat, if you are cracked (non-premium) user - type &cNO&7 in chat.";
            public String ASK_USER_WHEN_PREMIUM_TITLE = "&cAre you &6premium user&c?%nl%&cType &a&lYES &cor &c&lNO &cin chat.";

            @Comment("What the asked player has to write on chat to agree that he is premium user?")
            public String ASK_USER_IF_PREMIUM_YES_ANSWER = "yes";
            @Comment({"YES answer of premium user in other languages like German and Polish",
                    "In lowerCase (small letters)! So please do not write e.g JA here, just ja!"})
            public List<String> ASK_USER_IF_PREMIUM_YES_ANSWER_ALIASES = Arrays.asList("ja", "tak");
            @Comment("What the asked player has to write on chat to agree that he is cracked/non-premium user?")
            public String ASK_USER_IF_PREMIUM_NO_ANSWER = "no";
            @Comment({"NO answer of premium user in other languages like German and Polish",
                    "In lowerCase (small letters)! So please do not write e.g NEIN here, just nein!"})
            public List<String> ASK_USER_IF_PREMIUM_NO_ANSWER_ALIASES = Arrays.asList("nein", "nie");

            public boolean CLEAR_CHAT_AFTER_LOGIN = true;
            //public String UNREGISTERED_USER_CANNOT_DO_THAT = "&cYou cann";
        }

        @Comment("MySQL configuration")
        public static class MYSQL {

            @Comment("Server certification?")
            public boolean USE_SSL = false;
            public String HOSTNAME = "127.0.0.1";
            public int PORT = 3306;
            public String USER = "user";
            public String PASSWORD = "password";
            public String DATABASE = "database";

            @Comment("Database type: mysql or sqlite")
            public String TYPE = "sqlite";

            @Comment("Encryption/hash type, bcrypt or sha-256 (bcrypt is much better)")
            public String ENCRYPTION = "bcrypt";
        }
    }

    @Comment("Database Setup")
    public static class SQL {

        @Comment("Database type. sqlite or mysql")
        public String STORAGE_TYPE = "sqlite";
        @Comment("After how many days to remove players from the database, which have been tested and no longer entered. 0 or less to stop")
        public int PURGE_TIME = 14;
        @Comment("Settings for mysql")
        public String HOSTNAME = "localhost";
        public int PORT = 3306;
        public String USER = "user";
        public String PASSWORD = "password";
        public String DATABASE = "database";
    }

}
