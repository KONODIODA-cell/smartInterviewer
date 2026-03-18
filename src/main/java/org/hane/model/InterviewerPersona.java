package org.hane.model;

import dev.langchain4j.model.output.structured.Description;

import java.util.Arrays;
import java.util.List;

/**
 * AI 面试官人格配置
 * 基于 markdown 文件路径和内容生成
 */
public class InterviewerPersona {

    @Description("UUID，唯一标识符")
    private String id;

    @Description("人格名称，如：阿里巴巴Java专家")
    private String name;

    @Description("一句话描述这个人格的特点")
    private String description;

    @Description("完整的系统提示词，定义这个 AI 面试官的行为方式、语言风格、考察重点")
    private String systemPrompt;

    @Description("匹配的文件路径关键词，逗号分隔，如：阿里,Java,后端")
    private String filePatterns;

    @Description("专业领域，如：Java后端")
    private String expertise;

    @Description("面试风格，如：严谨型、亲和型、挑战型")
    private String style;

    @Description("难度偏好：BASIC(基础)、INTERMEDIATE(进阶)、ADVANCED(挑战)")
    private String difficultyBias;

    @Description("优先级，数字越大越优先匹配，范围1-10")
    private int priority;

    // 无参构造器（用于 JSON 反序列化）
    public InterviewerPersona() {
    }

    // 全参构造器
    public InterviewerPersona(String id, String name, String description, String systemPrompt,
                              String filePatterns, String expertise, String style,
                              String difficultyBias, int priority) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.systemPrompt = systemPrompt;
        this.filePatterns = filePatterns;
        this.expertise = expertise;
        this.style = style;
        this.difficultyBias = difficultyBias;
        this.priority = priority;
    }

    // Getters
    public String id() { return id; }
    public String name() { return name; }
    public String description() { return description; }
    public String systemPrompt() { return systemPrompt; }
    public String filePatterns() { return filePatterns; }
    public String expertise() { return expertise; }
    public String style() { return style; }
    public String difficultyBias() { return difficultyBias; }
    public int priority() { return priority; }

    // Setters（用于 JSON 反序列化）
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public void setFilePatterns(String filePatterns) { this.filePatterns = filePatterns; }
    public void setExpertise(String expertise) { this.expertise = expertise; }
    public void setStyle(String style) { this.style = style; }
    public void setDifficultyBias(String difficultyBias) { this.difficultyBias = difficultyBias; }
    public void setPriority(int priority) { this.priority = priority; }

    /**
     * 获取文件模式列表
     */
    public List<String> getFilePatternList() {
        if (filePatterns == null || filePatterns.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(filePatterns.split(","));
    }

    /**
     * 检查文件路径是否匹配此人格
     */
    public boolean matchesFilePath(String filePath) {
        if (filePath == null || filePatterns == null) {
            return false;
        }
        String lowerPath = filePath.toLowerCase();
        return getFilePatternList().stream()
                .anyMatch(pattern -> lowerPath.contains(pattern.toLowerCase()));
    }

    @Override
    public String toString() {
        return "InterviewerPersona{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", expertise='" + expertise + '\'' +
                ", style='" + style + '\'' +
                '}';
    }
}
