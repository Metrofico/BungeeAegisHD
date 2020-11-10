package xyz.yooniks.aegis.vpn;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import net.md_5.bungee.BungeeCord;
import xyz.yooniks.aegis.Aegis;
import xyz.yooniks.aegis.config.Settings;
import xyz.yooniks.aegis.vpn.AddressInfoResponse.VPNResponsable;

public final class VPNDetectorIPApi implements VPNDetector, VPNResponsable {

  private static final Gson GSON = new Gson();
  private final int timeout;

  private final int limit;
  private final boolean limitable;

  private final AtomicInteger count = new AtomicInteger();

  public VPNDetectorIPApi(int timeout, int limit,
      boolean limitable) {
    this.timeout = timeout;
    this.limit = limit;
    this.limitable = limitable;
  }

  @Override
  public String getName() {
    return "ip-api (country checker)";
  }

  @Override
  public boolean isBad(String address) throws IOException {
    final AddressInfoResponse addressInfoResponse = this.info(address);
    final boolean bad =
        addressInfoResponse.isProxy() || !Settings.IMP.ANTIVPN.COUNTRY_CHECKER.ALLOWED_COUNTRIES
            .contains(addressInfoResponse.getCountryCode());
    if (bad) {
      Aegis.getInstance().getVpnSystem().getVpnRequester().addBlockedIp(address);
      BungeeCord.getInstance().getLogger().info(
          "[Aegis AntiVPN] " + address + " detected blocked country! Coutry code: "
              + addressInfoResponse.getCountryCode());
    }
    return bad;
  }

  @Override
  public AddressInfoResponse info(String address) throws IOException {
    return GSON.fromJson(this.query("http://ip-api.com/json/" + address, this.timeout),
        AddressInfoResponse.class);
  }

  @Override
  public boolean isLimitable() {
    return limitable;
  }

  @Override
  public int getLimit() {
    return limit;
  }

  @Override
  public int count() {
    return this.count.getAndIncrement();
  }

  @Override
  public void clearCount() {
    this.count.set(0);
  }

}