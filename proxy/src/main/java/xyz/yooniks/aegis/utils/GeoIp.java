package xyz.yooniks.aegis.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.ice.tar.TarEntry;
import com.ice.tar.TarInputStream;
import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import lombok.Getter;
import net.md_5.bungee.BungeeCord;
import xyz.yooniks.aegis.config.Settings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * @author Leymooo
 */
public class GeoIp {

    private static final Logger LOGGER = BungeeCord.getInstance().getLogger();

    private final HashSet<String> countries = new HashSet<>();
    @Getter
    private final Cache<InetAddress, String> cached;

    @Getter
    private final boolean enabled = Settings.IMP.GEO_IP.MODE != 2;

    private final boolean whiteList = Settings.IMP.GEO_IP.TYPE == 0;

    private DatabaseReader reader;

    public GeoIp(boolean startup) {
        if (enabled) {
            countries.addAll(Settings.IMP.GEO_IP.ALLOWED_COUNTRIES);
            setupDataBase(startup);
            cached = CacheBuilder.newBuilder()
                    .concurrencyLevel(Runtime.getRuntime().availableProcessors())
                    .expireAfterAccess(5, TimeUnit.MINUTES).initialCapacity(200).build();

        } else {
            cached = null;
        }
    }

    public boolean isAllowed(InetAddress address) {
        if (!enabled || reader == null || address.isAnyLocalAddress() || address.isLoopbackAddress()) {
            return true;
        }
        String country = cached.getIfPresent(address);
        if (country != null) {
            /*blacklist*/
            return whiteList == countries.contains(country);
        }
        try {
            country = reader.country(address).getCountry().getIsoCode();
        } catch (IOException | GeoIp2Exception ex) {
            return false;
            //logger.log( Level.WARNING, "[Aegis] Could not get country for " + address.getHostAddress() );
        }
        cached.put(address, country);
        /*blacklist*/
        return whiteList == countries.contains(country);
    }

    public boolean isAvailable() {
        return reader != null;
    }

    private void setupDataBase(boolean startup) {
        File file = new File("Aegis", "GeoIP.mmdb");
        if (!file.exists() || (startup
                && (System.currentTimeMillis() - file.lastModified()) > TimeUnit.DAYS.toMillis(14))) {
            file.delete();
            downloadDataBase(file);
        } else {
            try {
                reader = new DatabaseReader.Builder(file).withCache(new CHMCache(4096 * 4)).build();
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "[Aegis] Unable to read GeoLite2 database.", ex);
                file.delete();
                setupDataBase(true);
            }
        }
    }

    private void downloadDataBase(final File out) {
        LOGGER.log(Level.INFO, "[Aegis] Downloading GeoLite2 database...");
        long start = System.currentTimeMillis();
        try {
            URL downloadUrl = new URL(Settings.IMP.GEO_IP.GEOIP_DOWNLOAD_URL);
            URLConnection conn = downloadUrl.openConnection();
            conn.setConnectTimeout(35000);
            try (InputStream input = conn.getInputStream()) {
                if (downloadUrl.getFile().endsWith(".mmdb")) {
                    saveToFile(input, out);
                } else if (downloadUrl.getFile().endsWith("tar.gz")) {
                    try (GZIPInputStream gzipIn = new GZIPInputStream(
                            input); TarInputStream tarIn = new TarInputStream(gzipIn)) {
                        TarEntry entry;
                        while ((entry = tarIn.getNextEntry()) != null) {
                            if (entry.getName().endsWith("mmdb")) {
                                saveToFile(tarIn, out);
                            }
                        }
                    }
                } else {
                    throw new IOException("File type is not supported ");
                }
            }
            setupDataBase(true);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "[Aegis] Unable to download GeoLite2 database.", ex);
            return;
        }
        LOGGER.log(Level.INFO, "[Aegis] GeoLite2 loaded ({0} ms)", System.currentTimeMillis() - start);
    }

    private void saveToFile(InputStream stream, File out) throws IOException {
        try (FileOutputStream fis = new FileOutputStream(out)) {
            byte[] buffer = new byte[2048];
            int count = 0;
            while ((count = stream.read(buffer, 0, 2048)) != -1) {
                if (Thread.interrupted()) {
                    fis.close();
                    out.delete();
                    LOGGER.log(Level.WARNING,
                            "[Aegis] Failed to download GeoLite2 database. Removing garbage file.");
                    return;
                }
                fis.write(buffer, 0, count);
            }
        }
    }

    public void close() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ignore) {
            }
        }
        if (cached != null) {
            cached.invalidateAll();
        }
    }

    public void tryClenUP() {
        if (cached != null) {
            cached.cleanUp();
        }
    }

}
