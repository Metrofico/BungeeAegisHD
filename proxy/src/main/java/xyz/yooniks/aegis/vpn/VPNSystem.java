package xyz.yooniks.aegis.vpn;

import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.config.Configuration;
import xyz.yooniks.aegis.config.Settings;
import xyz.yooniks.aegis.config.Settings.ANTIVPN.COUNTRY_CHECKER;
import xyz.yooniks.aegis.shell.BotShellConfiguration;

public class VPNSystem {

  private VPNRequester vpnRequester;
  private Thread clearLimitsThread;

  private List<VPNDetector> vpnDetectors;

  public void init(BotShellConfiguration configuration) {
    VPNThread.start();
    this.vpnRequester = new VPNRequester(Runtime.getRuntime().availableProcessors() * 2);

    this.vpnDetectors = new ArrayList<>();

    BungeeCord.getInstance().getLogger().info("[Aegis] Loading AntiVPN checkers");

    final COUNTRY_CHECKER countryChecker = Settings.IMP.ANTIVPN.COUNTRY_CHECKER;
    if (countryChecker.ENABLED) {
      vpnDetectors.add(new VPNDetectorIPApi(5000,
          countryChecker.REQUESTS_LIMIT, true));
    }

    final Configuration config = configuration.getConfig();
    if (config.getSection("anti-vpn") == null) {
      BungeeCord.getInstance().getLogger().warning(
          "[Aegis] \"anti-vpn\" config section in aegis_antibot.yml doesn't exist! AntiVPN will not work.");
      return;
    }
    for (String checkerId : config.getSection("anti-vpn").getKeys()) {
      final Configuration section = config.getSection("anti-vpn." + checkerId);
      final VPNDetector vpnDetector = new VPNDetectorDefault(checkerId,
          section.getString("url"),
          section.getString("excepted-result"), section.getInt("timeout", 5000),
          !section.getString("excepted-type").equalsIgnoreCase("contains"),
          section.getInt("requests-limit", 148),
          section.getBoolean("limitable", false));
      vpnDetectors.add(vpnDetector);

      BungeeCord.getInstance().getLogger()
          .info("[Aegis] Loaded AntiVPN checker: " + vpnDetector.getName());
    }
    if (vpnDetectors.size() > 0) {
      this.clearLimitsThread = new Thread(() -> {
        try {
          Thread.sleep(1000L * 60);
        } catch (InterruptedException ex) {
          return;
        }
        new VPNDetectorClearLimitTask(vpnDetectors).run();
      });
      this.clearLimitsThread.start();
    }
  }

  public void stop() {
    if (this.clearLimitsThread != null) {
      this.clearLimitsThread.interrupt();
      this.clearLimitsThread = null;
    }
    if (this.vpnRequester != null) {
      this.vpnRequester.shutDown();
      this.vpnRequester = null;
    }
    VPNThread.stop();
  }

  public List<VPNDetector> getVpnDetectors() {
    return vpnDetectors;
  }

  public VPNRequester getVpnRequester() {
    return vpnRequester;
  }

}
