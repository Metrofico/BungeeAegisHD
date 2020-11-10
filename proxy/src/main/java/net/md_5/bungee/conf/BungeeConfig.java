package net.md_5.bungee.conf;

import java.io.File;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;

public class BungeeConfig {

  private final Configuration configBungee;
  private final ConfigurationProvider configProvider;
  private final File configfile;

  public BungeeConfig(Configuration configBungee,
      ConfigurationProvider configProvider, File configfile) {
    this.configBungee = configBungee;
    this.configProvider = configProvider;
    this.configfile = configfile;
  }

  public Configuration getConfigBungee() {
    return configBungee;
  }

  public ConfigurationProvider getConfigProvider() {
    return configProvider;
  }

  public File getConfigfile() {
    return configfile;
  }

}
