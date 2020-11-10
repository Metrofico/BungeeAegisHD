package xyz.yooniks.aegis.auth.database;

import java.sql.SQLException;

public interface SqlDatabase<ID, TYPE> {

  void enableDatabase() throws SQLException;

  void disableDatabase() throws SQLException;

  //void loadObjects() throws SQLException;

  void saveObjects() throws SQLException;

  TYPE loadObject(ID id) throws SQLException;

  void saveObject(TYPE type) throws SQLException;

  TYPE loadByNameIgnoreCase(String name) throws SQLException;

  void removeObject(TYPE type) throws SQLException;

  void removeObjectByName(String name) throws SQLException;


}
