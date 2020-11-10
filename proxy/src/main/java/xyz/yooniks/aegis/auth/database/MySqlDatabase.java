package xyz.yooniks.aegis.auth.database;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import xyz.yooniks.aegis.auth.user.AuthUser;
import xyz.yooniks.aegis.auth.user.AuthUser.PremiumAnswer;
import xyz.yooniks.aegis.auth.user.AuthUserManager;
import xyz.yooniks.aegis.config.Settings;
import xyz.yooniks.aegis.config.Settings.AUTH.MYSQL;

public class MySqlDatabase implements SqlDatabase<UUID, AuthUser> {

  private final AuthUserManager userManager;

  private final String tableName = "aegisauth_users";
  private final HikariDataSource hikariDataSource = new HikariDataSource();

  public MySqlDatabase(AuthUserManager userManager) {
    this.userManager = userManager;

    final MYSQL sql = Settings.IMP.AUTH.MYSQL;
    hikariDataSource.setUsername(sql.USER);
    hikariDataSource.addDataSourceProperty("password", sql.PASSWORD);
    hikariDataSource.setJdbcUrl("jdbc:mysql://" + sql.HOSTNAME
        + ":" + sql.PORT + "/" + sql.DATABASE + "?useSSL=" + sql.USE_SSL);

    hikariDataSource.setMaximumPoolSize(Runtime.getRuntime().availableProcessors() * 2 + 1);
    hikariDataSource.addDataSourceProperty("cachePrepStmts", true);
    hikariDataSource.addDataSourceProperty("prepStmtCacheSize", 250);
    hikariDataSource.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
    hikariDataSource.addDataSourceProperty("useServerPrepStmts", true);
  }

  @Override
  public void enableDatabase() throws SQLException {
    try (final Connection connection = this.hikariDataSource.getConnection();
        final Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          "CREATE TABLE IF NOT EXISTS " + this.tableName + " (uuid CHAR(36) PRIMARY KEY,"
              + " name CHAR(16), password text, registered TINYINT(1), premium TINYINT(1), onlineId CHAR(37), premiumAnswer CHAR(4))");
    }
  }

  @Override
  public void disableDatabase() {
    this.hikariDataSource.close();
  }

  @Override
  public void removeObject(AuthUser authUser) throws SQLException {
    try (final Connection connection = this.hikariDataSource.getConnection();
        final PreparedStatement statement = connection.prepareStatement("DELETE FROM " + this.tableName + " WHERE name=?")) {
      statement.setString(1, authUser.getName());
      statement.execute();
    }
  }

  @Override
  public void removeObjectByName(String name) throws SQLException {
    try (final Connection connection = this.hikariDataSource.getConnection();
        final PreparedStatement statement = connection.prepareStatement("DELETE FROM " + this.tableName + " WHERE name=?")) {
      statement.setString(1, name);
      statement.execute();
    }
  }

  /*@Override
  public void loadObjects() throws SQLException {
    try (final Connection connection = this.hikariDataSource.getConnection();
        final Statement statement = connection.createStatement();
        final ResultSet resultSet = statement.executeQuery("SELECT * FROM " + this.tableName)) {
      while (resultSet.next()) {
        final UUID uuid = UUID.fromString(resultSet.getString("uuid"));
        final String name = resultSet.getString("name");
        final String password = resultSet.getString("password");
        final boolean registered = resultSet.getBoolean("registered");
        final boolean premium = resultSet.getBoolean("premium");

        final String id = resultSet.getString("onlineId");

        final UUID onlineId;
        if (id == null || id.isEmpty()) {
          onlineId = null;
        }
        else {
          onlineId = UUID.fromString(id.substring(0, 36));
        }

        final PremiumAnswer premiumAnswer = PremiumAnswer.valueOf(resultSet.getString("premiumAnswer"));
        this.userManager.putUser(new AuthUser(uuid, name, password, registered, premium, onlineId, premiumAnswer));
      }
    }
  }*/

  @Override
  public void saveObjects() throws SQLException {
    try (final Connection connection = this.hikariDataSource.getConnection()) {
      for (AuthUser user : this.userManager.getUsers()) {
        final PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO " + this.tableName + " VALUES(?,?,?,?,?,?,?"
                + /*uuid, name, password, registered, premium, onlineId, premiumAnswer*/") "
                + "ON DUPLICATE KEY UPDATE uuid=?, name=?, password=?, registered=?, premium=?, onlineId=?, premiumAnswer=?");

        statement.setString(1, user.getId().toString());
        statement.setString(2, user.getName());
        statement.setString(3, user.getPassword());
        statement.setBoolean(4, user.isRegistered());
        statement.setBoolean(5, user.isPremium());
        statement
            .setString(6, user.getOnlineId() == null ? "" : user.getOnlineId().toString() + ".");
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
  }

  @Override
  public AuthUser loadByNameIgnoreCase(String nick) throws SQLException {
    AuthUser user = null;
    try (final Connection connection = this.hikariDataSource.getConnection();
        final PreparedStatement statement = connection
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
  public AuthUser loadObject(UUID uuid) throws SQLException {
    AuthUser user = null;
    try (final Connection connection = this.hikariDataSource.getConnection();
        final PreparedStatement statement = connection
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
  public void saveObject(AuthUser user) throws SQLException {
    try (final Connection connection = this.hikariDataSource.getConnection();
        final PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO " + this.tableName + " VALUES(?,?,?,?,?,?,?"
                + /*uuid, name, password, registered, premium, onlineId, premiumAnswer*/") "
                + "ON DUPLICATE KEY UPDATE uuid=?, name=?, password=?, registered=?, premium=?, onlineId=?, premiumAnswer=?")) {

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

}
