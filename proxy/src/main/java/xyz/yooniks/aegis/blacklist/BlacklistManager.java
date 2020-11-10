package xyz.yooniks.aegis.blacklist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.BungeeCord;
import xyz.yooniks.aegis.config.Settings;
import xyz.yooniks.aegis.vpn.StringReplacer;

public class BlacklistManager {

  private final List<String> blacklist = new ArrayList<>();

  public void saveToFile() {
    final File file = new File("Aegis", "blacklist.txt");
    try {
      file.renameTo(new File("Aegis", "blacklist-old.txt"));

      final File newFile = new File("Aegis", "blacklist.txt");
      newFile.createNewFile();

      final FileOutputStream fos = new FileOutputStream(file);

      final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

      for (String text : new ArrayList<>(blacklist)) {
        bw.write(text);
        bw.newLine();
      }

      bw.close();

    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public void init() {
    final File file = new File("Aegis", "blacklist.txt");
    try {
      if (!file.exists()) {
        file.createNewFile();
      }
      final BufferedReader reader = new BufferedReader(new FileReader(file));

      String nextLine;
      while ((nextLine = reader.readLine()) != null) {

        final String line = nextLine.replace(" ", "");

        if (line.startsWith("##")) {
          continue;
        }

        final String[] ip = line.split(":");
        final String hostname = ip[0];

        if (!this.blacklist.contains(hostname)) {
          this.blacklist.add(hostname);
        }
      }
      reader.close();
    } catch (Exception ignored) {
    }
    if (this.blacklist.size() == 0
        && Settings.IMP.AEGIS_SETTINGS.BLACKLIST.FILE.LOAD_FROM_URL_WHEN_FILE_IS_EMPTY) {
      BungeeCord.getInstance().getLogger().info(
          "[Aegis] Blacklist.txt is empty, downloading content to this file from our website...");
      try {
        final URLConnection urlConnection = new URL(
            "https://raw.githubusercontent.com/yooniks/CasualProxy/master/default_proxies.txt")
            .openConnection();
        urlConnection.setConnectTimeout(7500);
        urlConnection.setReadTimeout(7500);

        final BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(urlConnection.getInputStream()));

        String line;

        while ((line = bufferedReader.readLine()) != null) {
          line = line.replace(" ", "");

          if (line.startsWith("##")) {
            continue;
          }

          final String[] ip = line.split(":");
          final String hostname = ip[0];

          if (!this.blacklist.contains(hostname)) {
            this.blacklist.add(hostname);
          }
        }
        bufferedReader.close();
      } catch (Exception ignored) {
      }
      BungeeCord.getInstance().getLogger()
          .info("[Aegis] Downloaded content to blacklist.txt from our website successfully!");
    }

    if (this.blacklist.size() > 0
        && Settings.IMP.AEGIS_SETTINGS.BLACKLIST.FILE.BLOCK_WITH_IPSET_WHEN_FILE_LOADED) {
      new Thread(() -> {
        final long start = System.currentTimeMillis();
        BungeeCord.getInstance().getLogger().info(
            "[Aegis Blacklist IPSet] Blacklisting every proxy from blacklist.txt with IPSet in progress..");
        for (String address : new ArrayList<>(this.blacklist)) {
          try {
            Runtime.getRuntime().exec(StringReplacer.replace(
                Settings.IMP.AEGIS_SETTINGS.BLACKLIST.COMMANDS.BLOCK_COMMAND, "{ADDRESS}",
                address));
          } catch (IOException ex) {
            BungeeCord.getInstance().getLogger()
                .warning("[Aegis Blacklist IPSet] Could not execute blacklist ipset command! "
                    + "Please make sure ipset is installed in your vps/dedicated server and bungee has enough permissions to run it. "
                    + ex.getMessage());
          }
        }
        BungeeCord.getInstance().getLogger().info(
            "[Aegis Blacklist IPSet] Blacklisted every proxy from blacklist.txt with IPSet in " + (
                System.currentTimeMillis() - start) + "ms!");
        Thread.currentThread().interrupt();
      }).start();
    }
    BungeeCord.getInstance().getLogger()
        .info("[Aegis] Loaded " + this.blacklist.size() + " blacklisted proxies!");
  }

  public void addBlacklist(String address) {
    this.blacklist.add(address);
  }

  public void removeBlacklist(String address) {
    this.blacklist.remove(address);
  }

  public boolean isBlacklisted(String address) {
    return this.blacklist.contains(address);
  }

}
