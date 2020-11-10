package xyz.yooniks.aegis.auth;

import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.md_5.bungee.BungeeCord;
import xyz.yooniks.aegis.auth.command.ChangePaswordCommand;
import xyz.yooniks.aegis.auth.concurrent.ConcurrentManager;
import xyz.yooniks.aegis.auth.database.MySqlDatabase;
import xyz.yooniks.aegis.auth.database.SqlDatabase;
import xyz.yooniks.aegis.auth.database.SqlDefaultDatabase;
import xyz.yooniks.aegis.auth.hasher.BCryptEncryption;
import xyz.yooniks.aegis.auth.hasher.Encryption;
import xyz.yooniks.aegis.auth.hasher.Sha256Encryption;
import xyz.yooniks.aegis.auth.user.AuthUser;
import xyz.yooniks.aegis.auth.user.AuthUserManager;
import xyz.yooniks.aegis.config.Settings;

public class AuthSystem {

  private final Logger logger = BungeeCord.getInstance().getLogger();

  private SqlDatabase<UUID, AuthUser> database;
  private ConcurrentManager concurrentManager;
  private AuthUserManager userManager;

  private Encryption encryption;

  public void init() {
    this.userManager = new AuthUserManager();
    this.database =
        Settings.IMP.AUTH.MYSQL.TYPE.equalsIgnoreCase("mysql") ? new MySqlDatabase(this.userManager)
            : new SqlDefaultDatabase(this.userManager);
    this.concurrentManager = new ConcurrentManager(this.getConcurrentPoolSize());
    this.encryption = Settings.IMP.AUTH.MYSQL.ENCRYPTION.equals("sha-256") ? new Sha256Encryption()
        : new BCryptEncryption(6);

    this.concurrentManager.runAsync(() -> {
      try {
        this.database.enableDatabase();
      } catch (SQLException ex) {
        this.logger.log(Level.WARNING, "[Aegis Auth] Could not init the mysql database! ", ex);
      }
      return this.database;
    });

    BungeeCord.getInstance().getPluginManager()
        .registerCommand(null, new ChangePaswordCommand(this));

    new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          Thread.sleep(1000L * 60 * 60 * 30);
        } catch (InterruptedException ex) {
          continue;
        }
        if (this.database == null) {
          continue;
        }
        try {
          this.database.saveObjects();
          this.logger.info("[Aegis Auth Task] Saved auth users data!");
        } catch (SQLException ex) {
          this.logger.log(Level.WARNING, "[Aegis Auth Task] Could not save the auth users! ", ex);
        }
      }
    }).start();

    Runtime.getRuntime().addShutdownHook(new Thread(this::saveAndClose));
  }

  public void stop() {
    new Thread(this::saveAndClose).start();
  }

  private void saveAndClose() {
    if (this.database == null) {
      return;
    }
    try {
      this.database.saveObjects();
      this.logger.info("[Aegis Auth] Saved auth users data!");
      this.database.disableDatabase();
    } catch (SQLException ex) {
      this.logger.log(Level.WARNING, "[Aegis Auth] Could not save the auth users! ", ex);
    }
  }

  private int getConcurrentPoolSize() {
    return Runtime.getRuntime().availableProcessors() * 2;
  }

  public Encryption getEncryption() {
    return encryption;
  }

  public SqlDatabase<UUID, AuthUser> getDatabase() {
    return database;
  }

  public ConcurrentManager getConcurrentManager() {
    return concurrentManager;
  }

  public AuthUserManager getUserManager() {
    return userManager;
  }

}
