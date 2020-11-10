package xyz.yooniks.aegis.auth.hasher;

import org.mindrot.jbcrypt.BCrypt;

public class BCryptEncryption implements Encryption {

  private final int logRounds;

  public BCryptEncryption(int logRounds) {
    this.logRounds = logRounds;
  }

  @Override
  public boolean match(String password, String encryptedPassword) {
    return BCrypt.checkpw(password, encryptedPassword);
  }

  @Override
  public String hash(String password) {
    return BCrypt.hashpw(password, BCrypt.gensalt(this.logRounds));
  }

}
