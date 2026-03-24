package org.hane.utils;

import dev.langchain4j.community.store.embedding.duckdb.DuckDBEmbeddingStore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import static org.hane.utils.AppConfig.dbPath;

/**
 * DuckDB 数据库工具类
 * 注意：DuckDB 使用文件级锁，同一时间只允许一个写连接
 * 查询操作请使用 getReadOnlyConn() 避免锁冲突
 */
public class DuckDb {
  private static final String connString = "jdbc:duckdb:" + dbPath;

  /**
   * 获取 Embedding Store 连接（用于向量检索）
   * 注意：此连接会长期持有，避免与其他连接同时使用
   */
  public static DuckDBEmbeddingStore getRagConn() {
    return DuckDBEmbeddingStore
        .builder()
        .filePath(dbPath)
        .tableName("embeddings")
        .build();
  }

  /**
   * 获取普通连接（可用于读写）
   * 注意：如果 DuckDBEmbeddingStore 正在使用，可能会遇到锁冲突
   */
  public static Connection getConn() throws Exception {
    return getConnection(connString);
  }

  /**
   * 获取只读连接（推荐用于查询操作）
   * 只读模式可以避免与 DuckDBEmbeddingStore 的锁冲突
   */
  public static Connection getReadOnlyConn() throws Exception {
    // 设置只读模式，避免文件锁冲突
    Properties props = new Properties();
    props.setProperty("duckdb.read_only", "true");
    return getConnection(connString, props);
  }

  private static Connection getConnection(String url) throws Exception {
    return DriverManager.getConnection(url);
  }

  private static Connection getConnection(String url, Properties props) throws Exception {
    return DriverManager.getConnection(url, props);
  }
}
