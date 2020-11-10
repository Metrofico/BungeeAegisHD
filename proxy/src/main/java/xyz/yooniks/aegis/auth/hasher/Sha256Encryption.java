package xyz.yooniks.aegis.auth.hasher;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sha256Encryption implements Encryption {

  @Override
  public String hash(String password) {
    return "$SHA$256$" + sha256(sha256(password) + "256");
  }

  @Override
  public boolean match(String password, String encryptedPassword) {
    String[] line = encryptedPassword.split("\\$");
    return line.length == 4 && isEqual(encryptedPassword, hash(password));
  }

  private boolean isEqual(String string1, String string2) {
    return MessageDigest.isEqual(
        string1.getBytes(StandardCharsets.UTF_8), string2.getBytes(StandardCharsets.UTF_8));
  }

  private String sha256(String message) {
    return hashMessage(message);
  }

  private String hash(String message, MessageDigest algorithm) {
    algorithm.reset();
    algorithm.update(message.getBytes());
    byte[] digest = algorithm.digest();
    return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1, digest));
  }

  private MessageDigest getDigest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new UnsupportedOperationException(
          "Your system seems not to support the hash algorithm 'SHA-256'");
    }
  }

  private String hashMessage(String message) {
    return hash(message, getDigest());
  }

}
