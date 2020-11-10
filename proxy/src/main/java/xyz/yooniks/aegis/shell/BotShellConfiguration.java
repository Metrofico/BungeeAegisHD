package xyz.yooniks.aegis.shell;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class BotShellConfiguration {

  private Configuration config;

  public void saveConfig() {

    final File dir = new File("Aegis");
    if (!dir.exists()) {
      dir.mkdirs();
    }
    final File file = new File(dir, "aegis_antibot.yml");

    if (!file.exists()) {
      try (InputStream in = this.getClass().getClassLoader()
          .getResourceAsStream("aegis_antibot.yml")) {
        if (in != null) {
          Files.copy(in, file.toPath());
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    try {
      this.config = ConfigurationProvider
          .getProvider(YamlConfiguration.class).load(file);
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public Configuration getConfig() {
    return config;
  }

}
