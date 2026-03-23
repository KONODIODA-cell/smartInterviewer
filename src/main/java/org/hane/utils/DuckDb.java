package org.hane.utils;

import dev.langchain4j.community.store.embedding.duckdb.DuckDBEmbeddingStore;

import java.sql.Connection;

import static java.sql.DriverManager.getConnection;
import static org.hane.utils.AppConfig.dbPath;

public class DuckDb {
  private static final String connString = "jdbc:duckdb:"+dbPath;

  public static DuckDBEmbeddingStore getRagConn() {
   return DuckDBEmbeddingStore
        .builder()
        .filePath(dbPath)
        .build();
  }

  public static Connection getConn() throws Exception {
    return getConnection(connString);
  }
}
