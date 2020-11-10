package xyz.yooniks.aegis.blacklist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.BungeeCord;

public class WhitelistManager {

  private final List<String> whitelist = new ArrayList<>();

  public void saveToFile() {
    final File file = new File("Aegis", "whitelist.txt");
    try {
      file.renameTo(new File("Aegis", "whitelist-old.txt"));

      final File newFile = new File("Aegis", "whitelist.txt");
      newFile.createNewFile();

      final FileOutputStream fos = new FileOutputStream(file);

      final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

      for (String text : new ArrayList<>(whitelist)) {
        bw.write(text);
        bw.newLine();
      }

      bw.close();

    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public void init() {
    final File file = new File("Aegis", "whitelist.txt");
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

        if (!this.whitelist.contains(hostname)) {
          this.whitelist.add(hostname);
        }
      }
      reader.close();
    } catch (Exception ignored) {
    }

    BungeeCord.getInstance().getLogger()
        .info("[Aegis] Loaded " + this.whitelist.size() + " whitelited ips & names!");
  }

  public void addWhitelist(String address) {
    this.whitelist.add(address);
  }

  public boolean isWhitelisted(String address) {
    return this.whitelist.contains(address);
  }

}
