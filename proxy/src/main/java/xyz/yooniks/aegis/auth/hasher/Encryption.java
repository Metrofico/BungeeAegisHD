package xyz.yooniks.aegis.auth.hasher;

public interface Encryption {

  String hash(String password);

  boolean match(String password, String encryptedPassword);

}
