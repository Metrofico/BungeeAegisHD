package xyz.yooniks.aegis.connection.login;

import xyz.yooniks.aegis.Aegis;
import xyz.yooniks.aegis.config.Settings;

public class PlayerConnection {

    private int count;
    private long lastConnection;

    private long lastTotalConnection;

    private int suspiciousCount;

    public PlayerConnection(int count, long lastConnection, long lastTotalConnection) {
        this.count = count;
        this.lastConnection = lastConnection;
        this.lastTotalConnection = lastTotalConnection;
    }

    public PlayerConnection(int count, long lastTotalConnection) {
        this.count = count;
        this.lastTotalConnection = lastTotalConnection;
    }



    public long getLastTotalConnection() {
        return lastTotalConnection;
    }

    public void setLastTotalConnection(long lastTotalConnection) {
        this.lastTotalConnection = lastTotalConnection;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getLastConnection() {
        return lastConnection;
    }

    public void setLastConnection(long lastConnection) {
        this.lastConnection = lastConnection;
    }

    public int getSuspiciousCount() {
        return suspiciousCount;
    }

    public void setSuspiciousCount(int suspiciousCount) {
        this.suspiciousCount = suspiciousCount;
    }

}
