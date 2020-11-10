package xyz.yooniks.aegis.auth.user;

import java.util.UUID;

public class AuthUser {

  private final UUID id;
  private String name;

  private String password;
  private boolean registered = false;
  private boolean premium = false;

  private boolean logged = false;

  private boolean checkedIfPremium = false;

  private UUID onlineId;
  private PremiumAnswer premiumAnswer = PremiumAnswer.NONE;

  public AuthUser(UUID id, String name) {
    this.id = id;
    this.name = name;
  }

  public AuthUser(UUID id, String name, String password) {
    this.id = id;
    this.name = name;
    this.password = password;
  }

  public AuthUser(UUID id, String name, String password, boolean registered, boolean premium,
      UUID onlineId, PremiumAnswer premiumAnswer) {
    this.id = id;
    this.name = name;
    this.password = password;
    this.registered = registered;
    this.premium = premium;
    this.onlineId = onlineId;
    this.premiumAnswer = premiumAnswer;
  }

  public UUID getOnlineId() {
    return onlineId;
  }

  public void setOnlineId(UUID onlineId) {
    this.onlineId = onlineId;
  }

  public PremiumAnswer getPremiumAnswer() {
    return premiumAnswer;
  }

  public void setPremiumAnswer(PremiumAnswer premiumAnswer) {
    this.premiumAnswer = premiumAnswer;
  }

  public boolean isCheckedIfPremium() {
    return checkedIfPremium;
  }

  public void setCheckedIfPremium(boolean checkedIfPremium) {
    this.checkedIfPremium = checkedIfPremium;
  }

  public boolean isLogged() {
    return logged;
  }

  public void setLogged(boolean logged) {
    this.logged = logged;
  }

  public boolean isRegistered() {
    return registered;
  }

  public void setRegistered(boolean registered) {
    this.registered = registered;
  }

  public boolean isPremium() {
    return premium;
  }

  public void setPremium(boolean premium) {
    this.premium = premium;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public static enum PremiumAnswer {
    YES, NO, NONE;
  }

}
