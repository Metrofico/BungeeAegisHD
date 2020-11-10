package xyz.yooniks.aegis.connection;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AegisStatistics {

  private final AtomicLong blockedConnections = new AtomicLong(0);
  private AtomicInteger connectionsPerSecond = new AtomicInteger(),
      pingsPerSecond = new AtomicInteger(),
      totalConnectionsPerSecond = new AtomicInteger(),
  encryptionsPerSecond = new AtomicInteger();

  public void addBlockedConnection() {
    this.blockedConnections.incrementAndGet();
  }

  public long getBlockedConnections() {
    return this.blockedConnections.longValue();
  }

  public int getTotalConnectionsPerSecond() {
    return totalConnectionsPerSecond.get();
  }

  public int getConnectionsPerSecond() {
    return connectionsPerSecond.get();
  }

  public void addConnectionPerSecond() {
    this.connectionsPerSecond.incrementAndGet();
  }

  public int getPingsPerSecond() {
    return pingsPerSecond.get();
  }

  public void addPingPerSecond() {
    this.pingsPerSecond.incrementAndGet();
  }

  public void addTotalConnectionPerSecond() {
    this.totalConnectionsPerSecond.incrementAndGet();
  }

  public void addEncryptionPerSecond() {
    this.encryptionsPerSecond.incrementAndGet();
  }

  public void startUpdating() {
    new Timer().scheduleAtFixedRate(new TimerTask() {
      int cpsBefore = 0;
      int ppsBefore = 0;
      int totalBefore = 0;
      int encryptBefore = 0;

      @Override
      public void run() {
        int currentCps = connectionsPerSecond.get();
        if (currentCps > 0) {
          connectionsPerSecond.set(connectionsPerSecond.get() - cpsBefore);

          cpsBefore = connectionsPerSecond.get();
        }
        int currentPps = pingsPerSecond.get();
        if (currentPps > 0) {
          pingsPerSecond.set(pingsPerSecond.get() - ppsBefore);

          ppsBefore = pingsPerSecond.get();
        }

        int currentEnc = encryptionsPerSecond.get();
        if (currentEnc > 0) {
          encryptionsPerSecond.set(encryptionsPerSecond.get() - encryptBefore);

          encryptBefore = encryptionsPerSecond.get();
        }

        int total = totalConnectionsPerSecond.get();
        if (total > 0) {
          totalConnectionsPerSecond.set(totalConnectionsPerSecond.get() - totalBefore);

          totalBefore = totalConnectionsPerSecond.get();
        }
      }
    }, 1000, 1000);
  }

}
