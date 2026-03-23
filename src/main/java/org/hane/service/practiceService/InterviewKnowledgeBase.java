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
import org.hane.model.InterviewerPersona;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
    this.embeddingStore = DuckDBEmbeddingStore.builder()
        .filePath(dbPath.toString())
        .tableName("interviews")
        .build();

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
        .minScore(0.6) // 相似度阈值，低于 0.6 视为不相关
        .build();

    EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

    return result.matches().stream()
        .filter(match -> !"interviewer_persona".equals(
            match.embedded().metadata().getString("type"))) // 过滤掉人格数据
        .map(match -> new InterviewReference(
            match.embedded().text(),
            match.embedded().metadata(),
            match.score()
        ))
        .limit(maxResults)
        .collect(Collectors.toList());
  }

  /**
   * 检索最合适的 AI 面试官人格
   * 根据主题匹配 file_patterns 关键词，返回最相关的人格
   *
   * @param topic 面试主题（如"Java", "算法"）
   * @return 最匹配的面试官人格
   */
  public Optional<InterviewerPersona> findBestPersona(String topic) {
    String sql = """
        SELECT id, name, description, system_prompt, file_patterns, expertise, style, difficulty_bias, priority
        FROM interviewer_personas
        ORDER BY priority DESC
        """;

    try (Connection conn = DriverManager.getConnection("jdbc:duckdb:" + dbPath.toString());
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

      List<InterviewerPersona> allPersonas = new ArrayList<>();
      while (rs.next()) {
        InterviewerPersona persona = new InterviewerPersona();
        persona.setId(rs.getString("id"));
        persona.setName(rs.getString("name"));
        persona.setDescription(rs.getString("description"));
        persona.setSystemPrompt(rs.getString("system_prompt"));
        persona.setFilePatterns(rs.getString("file_patterns"));
        persona.setExpertise(rs.getString("expertise"));
        persona.setStyle(rs.getString("style"));
        persona.setDifficultyBias(rs.getString("difficulty_bias"));
        persona.setPriority(rs.getInt("priority"));
        allPersonas.add(persona);
      }

      // 如果 topic 为空或默认主题，返回优先级最高的人格
      if (topic == null || topic.isBlank() || "通用技术".equals(topic)) {
        return allPersonas.stream()
            .filter(p -> !"default".equals(p.getId()))
            .findFirst()
            .or(() -> Optional.of(allPersonas.get(0)));
      }

      // 根据 topic 匹配 file_patterns 和 expertise
      String lowerTopic = topic.toLowerCase();
      return allPersonas.stream()
          .filter(p -> !"default".equals(p.getId()))
          .filter(p -> matchesTopic(p, lowerTopic))
          .sorted(Comparator.comparingInt(InterviewerPersona::getPriority).reversed())
          .findFirst()
          .or(() -> allPersonas.stream()
              .filter(p -> !"default".equals(p.getId()))
              .findFirst());
    } catch (SQLException e) {
      System.err.println("查询人格数据失败：" + e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * 检查人格是否匹配给定主题
   */
  private boolean matchesTopic(InterviewerPersona persona, String lowerTopic) {
    // 匹配 file_patterns
    if (persona.getFilePatterns() != null && !persona.getFilePatterns().isEmpty()) {
      String[] patterns = persona.getFilePatterns().split(",");
      for (String pattern : patterns) {
        if (pattern.toLowerCase().contains(lowerTopic) || lowerTopic.contains(pattern.toLowerCase())) {
          return true;
        }
      }
    }
    // 匹配 expertise
    if (persona.getExpertise() != null && !persona.getExpertise().isEmpty()) {
      String lowerExpertise = persona.getExpertise().toLowerCase();
      if (lowerExpertise.contains(lowerTopic) || lowerTopic.contains(lowerExpertise)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 获取默认面试官人格
   */
  public InterviewerPersona getDefaultPersona() {
    InterviewerPersona persona = new InterviewerPersona();
    persona.setId("default");
    persona.setName("通用技术面试官");
    persona.setDescription("默认的通用技术面试官人格");
    persona.setSystemPrompt("""
        你是一位拥有 10 年经验的资深技术面试官。
        
        你的风格：
        1. 专业且友善：语气像资深同事，而非冷冰冰的考官
        2. 循循善诱：当回答不完整时，会追问"你提到 X，那如果场景 Y 呢？"
        3. 注重原理：不只考察"是什么"，更要考察"为什么"和"场景化应用"
        4. 实事求是：承认优秀回答，也明确指出技术误区
        
        规则：
        - 一次只问一道题，等待回答后再评估
        - 使用中文交流，技术术语可保留英文（如 GC、OOP）
        - 不透露标准答案，只通过追问引导思考
        """);
    persona.setFilePatterns("");
    persona.setExpertise("通用技术");
    persona.setStyle("专业型");
    persona.setDifficultyBias("INTERMEDIATE");
    persona.setPriority(1);
    return persona;
  }

  /**
   * 获取知识库统计信息
   */
  public KnowledgeStats getStats() {
    return new KnowledgeStats(0, dbPath.toFile().length());
  }



  public record KnowledgeStats(int documentCount, long dbSizeBytes) {
  }
}
