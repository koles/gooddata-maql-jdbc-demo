package com.gooddata.maql.jdbc.driver;

import java.sql.DriverManager;

public class RunQuery {

  public static void main(String[] args) throws Exception {
    if (args.length != 3) {
      throw new IllegalArgumentException("Usage: RunQuery <jdbcUrl> <username> <password>");
    }
    String jdbcUrl = args[0];
    String username = args[1];
    String password = args[2];
    
    DriverManager.registerDriver (new Driver());
    java.sql.Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
    java.sql.Statement stmt = conn.createStatement();
    java.sql.ResultSet rs = stmt.executeQuery("SELECT Product, Revenue");
    if (rs == null) {
      throw new IllegalStateException("null ResultSet");
    }
    while (rs.next()) {
      System.out.printf("%s\t%s\n", rs.getString(1), rs.getString(2));
    }
    rs.close();
    stmt.close();
    conn.close();
  }

}
