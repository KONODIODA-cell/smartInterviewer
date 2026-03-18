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
     * @param company    可选：限定公司（如"阿里巴巴"），null 表示不限
     * @param maxResults 返回条数
     * @return 匹配结果列表（包含文本、元数据、相似度分数）
     */
    public List<InterviewReference> search(String query, String company, int maxResults) {
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
                .filter(ref -> company == null || company.equals(ref.metadata().getString("company")))
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    /**
     * 检索最合适的 AI 面试官人格
     * 根据主题匹配，返回最相关的人格
     *
     * @param topic 面试主题（如"Java", "算法"）
     * @return 最匹配的面试官人格
     */
    public Optional<InterviewerPersona> findBestPersona(String topic) {
        // 生成查询向量
        Embedding queryEmbedding = embeddingModel.embed(topic).content();

        // 构建检索请求
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(10)
                .minScore(0.5)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

        // 从结果中找出类型为 interviewer_persona 的
        return result.matches().stream()
                .filter(match -> "interviewer_persona".equals(
                        match.embedded().metadata().getString("type")))
                .sorted(Comparator.comparingDouble(match -> -match.score())) // 按分数降序
                .findFirst()
                .map(match -> {
                    var metadata = match.embedded().metadata();
                    InterviewerPersona persona = new InterviewerPersona();
                    persona.setId(metadata.getString("persona_id"));
                    persona.setName(metadata.getString("persona_name"));
                    persona.setDescription("");
                    persona.setSystemPrompt(match.embedded().text());
                    persona.setFilePatterns(metadata.getString("file_patterns"));
                    persona.setExpertise(metadata.getString("expertise"));
                    persona.setStyle(metadata.getString("style"));
                    persona.setDifficultyBias(metadata.getString("difficulty_bias"));
                    persona.setPriority(Integer.parseInt(metadata.getString("priority")));
                    return persona;
                });
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

    /**
     * 获取所有可用的面试官人格
     * 
     * @return 人格列表
     */
    public List<InterviewerPersona> getAllPersonas() {
        List<InterviewerPersona> personas = new ArrayList<>();
        
        // 先添加默认人格
        personas.add(getDefaultPersona());
        
        // 从数据库查询所有人格
        String sql = """
            SELECT metadata, text 
            FROM interviews 
            WHERE metadata->>'type' = 'interviewer_persona'
            """;
        
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:" + dbPath.toString());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String metadataJson = rs.getString("metadata");
                String text = rs.getString("text");
                
                InterviewerPersona persona = parsePersonaFromMetadata(metadataJson, text);
                if (persona != null) {
                    personas.add(persona);
                }
            }
        } catch (SQLException e) {
            System.err.println("查询人格数据失败: " + e.getMessage());
        }
        
        return personas;
    }
    
    /**
     * 从 metadata JSON 解析人格
     */
    private InterviewerPersona parsePersonaFromMetadata(String metadataJson, String text) {
        try {
            InterviewerPersona persona = new InterviewerPersona();
            persona.setSystemPrompt(text);
            
            // 手动解析 JSON（简化处理，避免引入 JSON 库依赖）
            persona.setId(extractJsonValue(metadataJson, "persona_id"));
            persona.setName(extractJsonValue(metadataJson, "persona_name"));
            persona.setExpertise(extractJsonValue(metadataJson, "expertise"));
            persona.setStyle(extractJsonValue(metadataJson, "style"));
            persona.setDifficultyBias(extractJsonValue(metadataJson, "difficulty_bias"));
            persona.setFilePatterns(extractJsonValue(metadataJson, "file_patterns"));
            
            String priorityStr = extractJsonValue(metadataJson, "priority");
            if (priorityStr != null && !priorityStr.isEmpty()) {
                try {
                    persona.setPriority(Integer.parseInt(priorityStr));
                } catch (NumberFormatException e) {
                    persona.setPriority(5);
                }
            } else {
                persona.setPriority(5);
            }
            
            return persona;
        } catch (Exception e) {
            System.err.println("解析人格数据失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从 JSON 字符串中提取字段值
     */
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"";
        int keyIndex = json.indexOf(pattern);
        if (keyIndex == -1) return "";
        
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return "";
        
        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        
        if (valueStart < json.length() && json.charAt(valueStart) == '"') {
            // 字符串值
            valueStart++;
            int valueEnd = json.indexOf("\"", valueStart);
            // 处理转义字符
            StringBuilder value = new StringBuilder();
            for (int i = valueStart; i < valueEnd; i++) {
                if (json.charAt(i) == '\\' && i + 1 < valueEnd) {
                    value.append(json.charAt(i + 1));
                    i++;
                } else {
                    value.append(json.charAt(i));
                }
            }
            return value.toString();
        } else {
            // 非字符串值（数字等）
            int valueEnd = valueStart;
            while (valueEnd < json.length() && 
                   (Character.isLetterOrDigit(json.charAt(valueEnd)) || json.charAt(valueEnd) == '.')) {
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd).trim();
        }
    }

    public record KnowledgeStats(int documentCount, long dbSizeBytes) {
    }
}
