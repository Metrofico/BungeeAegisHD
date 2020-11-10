package xyz.yooniks.aegis.connection.ping;

public class Ping {

  private int count;
  private long lastPing;

  private int suspiciousCount;

  public Ping(int count, long lastPing) {
    this.count = count;
    this.lastPing = lastPing;
  }

  public int getSuspiciousCount() {
    return suspiciousCount;
  }

  public void setSuspiciousCount(int suspiciousCount) {
    this.suspiciousCount = suspiciousCount;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public long getLastPing() {
    return lastPing;
  }

  public void setLastPing(long lastPing) {
    this.lastPing = lastPing;
  }

}
