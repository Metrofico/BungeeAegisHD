package xyz.yooniks.aegis.vpn;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class VPNRequester {

  private final ExecutorService executorService;

  private final List<String> blockedIps = new ArrayList<>();

  public VPNRequester(int poolSize) {
    this.executorService = Executors.newFixedThreadPool(poolSize);
  }

  public boolean isBlockedIp(String ip) {
    return this.blockedIps.contains(ip);
  }

  public void addBlockedIp(String ip) {
    this.blockedIps.add(ip);
  }

  public <T> Future<T> runAsync(Supplier<T> supplier) {
    return CompletableFuture.supplyAsync(supplier, this.executorService);
  }

  public void shutDown() {
    this.executorService.shutdown();
  }

}
