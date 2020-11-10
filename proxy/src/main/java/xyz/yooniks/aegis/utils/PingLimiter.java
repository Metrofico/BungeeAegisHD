package xyz.yooniks.aegis.utils;

/**
 * @author Leymooo
 */
public class PingLimiter {

  private static final long ONE_MIN = 60000;
  private static final long BAN_TIME = ONE_MIN * 5;
  private static int THRESHOLD = 950;
  private static long LASTCHECK = System.currentTimeMillis();
  private static int currNumOfPings = 0;
  private static boolean banned = false;
  private PingLimiter() {

  }

  public static boolean handle() {
    currNumOfPings++;
    long currTime = System.currentTimeMillis();
    if (banned) {
      if (currTime - LASTCHECK > BAN_TIME) {
        banned = false;
        currNumOfPings = 0;
        LASTCHECK = currTime;
      }
      return banned;
    }

    if ((currTime - LASTCHECK) <= ONE_MIN && currNumOfPings >= THRESHOLD) {
      banned = true;
      LASTCHECK = currTime;
    } else if (currTime - LASTCHECK >= ONE_MIN) {
      currNumOfPings = 0;
      LASTCHECK = currTime;
    }
    return banned;
  }

}
