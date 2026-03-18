package org.hane.model;

import dev.langchain4j.data.document.Metadata;

/**
 * 面试题引用（从向量库检索出的原始面经片段）
 */
public record InterviewReference(
    String content,      // 面经文本内容（可能包含 Q&A）
    Metadata metadata,   // 来源文件、公司等元数据
    double score         // 相似度分数（0-1）
) {}