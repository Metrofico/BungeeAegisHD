package xyz.yooniks.aegis.auth.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import xyz.yooniks.aegis.auth.user.AuthUser;
import xyz.yooniks.aegis.auth.user.AuthUser.PremiumAnswer;
import xyz.yooniks.aegis.auth.user.AuthUserManager;

public class SqlDefaultDatabase implements SqlDatabase<UUID, AuthUser> {

  private final AuthUserManager userManager;
  private final String tableName = "aegisauth_users";
  private Connection connection;

  public SqlDefaultDatabase(AuthUserManager userManager) {
    this.userManager = userManager;
  }

  @Override
  public void enableDatabase() throws SQLException {
    if (connection != null && connection.isValid(3)) {
      return;
    }
    long start = System.currentTimeMillis();
    try {
      Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException ex) {
      throw new SQLException(ex.getMessage());
    }
    connectToDatabase("JDBC:sqlite:Aegis/auth_database.db", null, null);
    createTable();
  }

  private void connectToDatabase(String url, String user, String password) throws SQLException {
    this.connection = DriverManager.getConnection(url, user, password);
  }

  private void createTable() throws SQLException {
    String sql = "CREATE TABLE IF NOT EXISTS " + this.tableName
        + " (uuid CHAR(36) PRIMARY KEY, name CHAR(16), password text, registered TINYINT(1), premium TINYINT(1), onlineId CHAR(37), premiumAnswer CHAR(4))";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.executeUpdate();
    }
  }

  @Override
  public void saveObjects() throws SQLException {
    for (AuthUser user : this.userManager.getUsers()) {
      final PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO `" + this.tableName + "` VALUES(?,?,?,?,?,?,?"
              + /*uuid, name, password, registered, premium, onlineId, premiumAnswer*/") "
              + "ON CONFLICT(uuid) DO UPDATE SET uuid=?, name=?, password=?, registered=?, premium=?, onlineId=?, premiumAnswer=?");

      statement.setString(1, user.getId().toString());
      statement.setString(2, user.getName());
      statement.setString(3, user.getPassword());
      statement.setBoolean(4, user.isRegistered());
      statement.setBoolean(5, user.isPremium());
      statement.setString(6, user.getOnlineId() == null ? "" : user.getOnlineId().toString() + ".");
      statement.setString(7, user.getPremiumAnswer().name());

      statement.setString(8, user.getId().toString());
      statement.setString(9, user.getName());
      statement.setString(10, user.getPassword());
      statement.setBoolean(11, user.isRegistered());
      statement.setBoolean(12, user.isPremium());

      statement.setString(13,
          user.getOnlineId() == null ? "empty" : user.getOnlineId().toString() + ".");
      statement.setString(14, user.getPremiumAnswer().name());

      statement.executeUpdate();

      statement.close();
    }
  }

  @Override
  public AuthUser loadObject(UUID uuid) throws SQLException {
    AuthUser user = null;
    try (final PreparedStatement statement = connection
        .prepareStatement("SELECT * FROM " + this.tableName + " WHERE uuid=?")) {
      statement.setString(1, uuid.toString());

      final ResultSet resultSet = statement.executeQuery();

      if (resultSet.next()) {
        final String name = resultSet.getString("name");
        final String password = resultSet.getString("password");
        final boolean registered = resultSet.getBoolean("registered");
        final boolean premium = resultSet.getBoolean("premium");
        //final boolean askedIfPremium = resultSet.getBoolean("askedIfPremium");
        final String id = resultSet.getString("onlineId");

        final UUID onlineId;
        if (id == null || id.isEmpty()) {
          onlineId = null;
        } else {
          onlineId = UUID.fromString(id.substring(0, 36));
        }
        final PremiumAnswer premiumAnswer = PremiumAnswer
            .valueOf(resultSet.getString("premiumAnswer"));

        user = new AuthUser(uuid, name, password, registered, premium, onlineId, premiumAnswer);
      }

      resultSet.close();

      return user;
    }
  }

  @Override
  public void removeObject(AuthUser authUser) throws SQLException {
    if (authUser != null && authUser.getName() != null) {
      try (final PreparedStatement statement = connection
          .prepareStatement("DELETE FROM " + this.tableName + " WHERE name=?")) {
        statement.setString(1, authUser.getName());
        statement.execute();
      }
    }
  }

  @Override
  public void removeObjectByName(String name) throws SQLException {
    try (final PreparedStatement statement = connection
        .prepareStatement("DELETE FROM " + this.tableName + " WHERE name=?")) {
      statement.setString(1, name);
      statement.execute();
    }
  }

  @Override
  public AuthUser loadByNameIgnoreCase(String nick) throws SQLException {
    AuthUser user = null;
    try (final PreparedStatement statement = connection
        .prepareStatement("SELECT * FROM " + this.tableName)) {
      final ResultSet resultSet = statement.executeQuery();

      while (resultSet.next()) {
        final String name = resultSet.getString("name");
        if (name.equalsIgnoreCase(nick) && !name.equals(nick)) {
          final String password = resultSet.getString("password");
          final boolean registered = resultSet.getBoolean("registered");
          final boolean premium = resultSet.getBoolean("premium");
          //final boolean askedIfPremium = resultSet.getBoolean("askedIfPremium");
          final String id = resultSet.getString("onlineId");
          final UUID uuid = UUID.fromString(resultSet.getString("uuid"));

          final UUID onlineId;
          if (id == null || id.isEmpty()) {
            onlineId = null;
          } else {
            onlineId = UUID.fromString(id.substring(0, 36));
          }
          final PremiumAnswer premiumAnswer = PremiumAnswer
              .valueOf(resultSet.getString("premiumAnswer"));

          user = new AuthUser(uuid, name, password, registered, premium, onlineId, premiumAnswer);
        }
      }

      resultSet.close();

      return user;
    }
  }

  @Override
  public void saveObject(AuthUser user) throws SQLException {
    try (final PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO `" + this.tableName + "` VALUES(?,?,?,?,?,?,?"
            + /*uuid, name, password, registered, premium, onlineId, premiumAnswer*/") "
            + "ON CONFLICT(uuid) DO UPDATE SET uuid=?, name=?, password=?, registered=?, premium=?, onlineId=?, premiumAnswer=?")) {

      statement.setString(1, user.getId().toString());
      statement.setString(2, user.getName());
      statement.setString(3, user.getPassword());
      statement.setBoolean(4, user.isRegistered());
      statement.setBoolean(5, user.isPremium());

      statement
          .setString(6, user.getOnlineId() == null ? "empty" : user.getOnlineId().toString() + ".");
      statement.setString(7, user.getPremiumAnswer().name());

      statement.setString(8, user.getId().toString());
      statement.setString(9, user.getName());
      statement.setString(10, user.getPassword());
      statement.setBoolean(11, user.isRegistered());
      statement.setBoolean(12, user.isPremium());

      statement.setString(13,
          user.getOnlineId() == null ? "empty" : user.getOnlineId().toString() + ".");
      statement.setString(14, user.getPremiumAnswer().name());

      statement.executeUpdate();
    }
  }

  @Override
  public void disableDatabase() throws SQLException {
    if (this.connection != null) {
      this.connection.close();
    }
  }
}
