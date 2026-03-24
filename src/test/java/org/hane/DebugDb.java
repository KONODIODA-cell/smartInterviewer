package org.hane;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

public class DebugDb {
  public static void main(String[] args) throws Exception {
    String dbPath = System.getenv("DB_PATH");
    if (dbPath == null || dbPath.isBlank()) {
      System.err.println("DB_PATH 未设置");
      return;
    }

    String connString = "jdbc:duckdb:" + dbPath;
    Properties props = new Properties();
    props.setProperty("duckdb.read_only", "true");
    
    try (Connection conn = DriverManager.getConnection(connString, props);
         Statement stmt = conn.createStatement()) {
      
      System.out.println("=== 数据库表 ===");
      ResultSet tables = stmt.executeQuery("SHOW TABLES");
      while (tables.next()) {
        System.out.println("表名：" + tables.getString(1));
      }
      
      System.out.println("\n=== interviews 表结构 ===");
      try {
        ResultSet columns = stmt.executeQuery("DESCRIBE interviews");
        while (columns.next()) {
          System.out.println(columns.getString(1) + " : " + columns.getString(2));
        }
      } catch (Exception e) {
        System.out.println("表 interviews 不存在");
      }
      
      System.out.println("\n=== 数据统计 ===");
      try {
        ResultSet count = stmt.executeQuery("SELECT COUNT(*) FROM interviews");
        if (count.next()) {
          System.out.println("总记录数：" + count.getInt(1));
        }
      } catch (Exception e) {
        System.out.println("查询失败：" + e.getMessage());
      }
      
      System.out.println("\n=== 数据类型分布 ===");
      try {
        ResultSet typeDist = stmt.executeQuery(
            "SELECT metadata->>'type' as type, COUNT(*) as cnt " +
            "FROM interviews " +
            "GROUP BY metadata->>'type'");
        while (typeDist.next()) {
          System.out.println(typeDist.getString(1) + ": " + typeDist.getInt(2));
        }
      } catch (Exception e) {
        System.out.println("查询失败：" + e.getMessage());
      }
      
      System.out.println("\n=== 文件来源分布 (Top 10) ===");
      try {
        ResultSet fileDist = stmt.executeQuery(
            "SELECT metadata->>'file_name' as file_name, COUNT(*) as cnt " +
            "FROM interviews " +
            "GROUP BY metadata->>'file_name' " +
            "ORDER BY cnt DESC " +
            "LIMIT 10");
        while (fileDist.next()) {
          System.out.println(fileDist.getString(1) + ": " + fileDist.getInt(2) + " 条");
        }
      } catch (Exception e) {
        System.out.println("查询失败：" + e.getMessage());
      }
    }
  }
}
