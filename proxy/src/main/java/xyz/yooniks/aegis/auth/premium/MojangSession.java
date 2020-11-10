package xyz.yooniks.aegis.auth.premium;

public class MojangSession {

  private String name;
  private String id;

  public MojangSession(String name, String id) {
    this.name = name;
    this.id = id;
  }

  public MojangSession() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

}
