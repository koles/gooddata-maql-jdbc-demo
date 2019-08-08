package com.gooddata.maql.jdbc.driver;

import java.sql.DriverManager;

public class RunQuery {
  public static final String DEFAULT_QUERY = "SELECT Product, Revenue";
 
  public static void main(String[] args) throws Exception {
    if (args.length < 3) {
      throw new IllegalArgumentException("Usage: RunQuery <jdbcUrl> <username> <password>");
    }
    String jdbcUrl = args[0];
    String username = args[1];
    String password = args[2];
    String query = args.length > 3 ? args[3] : DEFAULT_QUERY;
    System.out.println(query);
    
    // DriverManager.registerDriver (new Driver());
    java.sql.Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
    java.sql.Statement stmt = conn.createStatement();
    java.sql.ResultSet rs = stmt.executeQuery(query);
    if (rs == null) {
      throw new IllegalStateException("null ResultSet");
    }
    int columnCount = rs.getMetaData().getColumnCount();
    while (rs.next()) {
      StringBuffer sb = new StringBuffer();
      for (int i = 1; i <= columnCount; i++) {
        sb.append(rs.getString(i));
        if (i != columnCount) {
          sb.append("\t");
        }
      }
      System.out.println(sb.toString());
    }
    rs.close();
    stmt.close();
    conn.close();
  }

}
