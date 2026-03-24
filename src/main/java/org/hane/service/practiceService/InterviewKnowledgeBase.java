package org.hane.service.practiceService;

import dev.langchain4j.community.store.embedding.duckdb.DuckDBEmbeddingStore;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzh.BgeSmallZhEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.hane.model.InterviewReference;
import org.hane.utils.DuckDb;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class InterviewKnowledgeBase {

  private final EmbeddingStore<TextSegment> embeddingStore;
  private final EmbeddingModel embeddingModel;
  private final Path dbPath;

  public InterviewKnowledgeBase(Path dbPath) {
    this.dbPath = dbPath;
    // 使用本地中文 Embedding 模型（首次会自动下载 ONNX 模型文件到 ~/.cache/langchain4j/）
    this.embeddingModel = new BgeSmallZhEmbeddingModel();

    // 初始化 DuckDB（如果不存在会自动创建）
    this.embeddingStore = DuckDb.getRagConn();

    System.out.println("知识库初始化完成: " + dbPath.toAbsolutePath());
  }

  /**
   * 向量检索：根据查询返回最相似的面经片段
   *
   * @param query      查询文本（如"JVM垃圾回收"）
   * @param maxResults 返回条数
   * @return 匹配结果列表（包含文本、元数据、相似度分数）
   */
  public List<InterviewReference> search(String query, int maxResults) {
    // 生成查询向量
    Embedding queryEmbedding = embeddingModel.embed(query).content();

    // 构建检索请求
    EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
        .queryEmbedding(queryEmbedding)
        .maxResults(maxResults * 2) // 多检索一些，后续过滤
        .minScore(0.1) // 降低阈值，避免漏掉相关内容
        .build();

    EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

    System.out.println("   [DuckDB] 查询 '" + query + "'，原始结果：" + result.matches().size() + " 条");

    // 调试：打印原始结果的详细信息
//    result.matches().forEach(match -> {
//      System.out.println("   [DEBUG] embedded=" + (match.embedded() != null ? "not null" : "null"));
//      if (match.embedded() != null) {
//        System.out.println("   [DEBUG] text=" + (match.embedded().text() != null ? "not null" : "null"));
//        System.out.println("   [DEBUG] metadata=" + (match.embedded().metadata() != null ? "not null" : "null"));
//        if (match.embedded().metadata() != null) {
//          System.out.println("   [DEBUG] type=" + match.embedded().metadata().getString("type"));
//        }
//      }
//    });

    return result.matches().stream()
        .filter(match -> match.embedded() != null) // 过滤掉 embedded 为 null 的无效结果
        .filter(match -> match.embedded().metadata() != null) // 过滤掉 metadata 为 null 的结果
        .filter(match -> {
          // 过滤掉人格数据 (type 为 null 或不是 interviewer_persona 的保留)
          String type = match.embedded().metadata().getString("type");
          return type == null || !"interviewer_persona".equals(type);
        })
        .map(match -> new InterviewReference(
            match.embedded().text(),
            match.embedded().metadata(),
            match.score()
        ))
        .limit(maxResults)
        .collect(Collectors.toList());
  }

}
