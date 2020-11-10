package net.md_5.bungee.protocol;

public class FastException extends RuntimeException {

  public FastException(String message) {
    super(message);
  }

  @Override
  public synchronized Throwable initCause(Throwable cause) {
    return this;
  }

  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }

}