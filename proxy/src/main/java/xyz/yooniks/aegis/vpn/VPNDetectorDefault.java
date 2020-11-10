package xyz.yooniks.aegis.vpn;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public final class VPNDetectorDefault implements VPNDetector {

  private final String url, result;
  private final int timeout;
  private final boolean mustEqual;

  private final int limit;
  private final boolean limitable;

  private final AtomicInteger count = new AtomicInteger();
  private final String name;

  public VPNDetectorDefault(String id, String url, String result, int timeout, boolean mustEqual,
      int limit,
      boolean limitable) {
    this.url = url;
    this.result = result;
    this.timeout = timeout;
    this.mustEqual = mustEqual;
    this.limit = limit;
    this.limitable = limitable;
    this.name = id;
  }

  @Override
  public boolean isBad(String address) throws IOException {
    final String query = this
        .query(StringReplacer.replace(this.url, "{ADDRESS}", address), this.timeout);
    return this.mustEqual ? query.equalsIgnoreCase(this.result)
        : query.toLowerCase().replace("%x", "\"").contains(this.result.toLowerCase()
            .replace("%x", "\""));
  }

  @Override
  public String getName() {
    return this.name;
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