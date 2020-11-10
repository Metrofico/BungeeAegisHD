package xyz.yooniks.aegis.auth.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class ConcurrentManager {

  private final ExecutorService executorService, executorSaver, executorServiceDelayed;

  public ConcurrentManager(int poolSize) {
    this.executorService = Executors.newFixedThreadPool(poolSize);
    this.executorServiceDelayed = Executors.newFixedThreadPool(poolSize);
    this.executorSaver = Executors.newFixedThreadPool(poolSize);

  }

  public <T> Future<T> runAsync(Supplier<T> supplier) {
    return CompletableFuture.supplyAsync(supplier, this.executorService);
  }

  public <T> Future<T> runAsyncSave(Supplier<T> supplier) {
    return CompletableFuture.supplyAsync(supplier, this.executorSaver);
  }

  public <T> Future<T> runAsyncDelayed(Supplier<T> supplier) {
    return CompletableFuture.supplyAsync(supplier, this.executorServiceDelayed);
  }

}
