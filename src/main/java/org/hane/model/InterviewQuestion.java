package org.hane.model;

import java.util.List;

public record InterviewQuestion(
		String id,                    // UUID，用于关联评估
		String content,               // 题目内容
		List<String> keyPoints,       // 考察要点（AI 标注）
		Difficulty difficulty,        // 难度枚举
		String source,                // 来源：阿里巴巴-三面
		String topic,                 // 所属主题：JVM
		String suggestedAnswer        // 参考答案（用于评估对比，不展示给用户）
) {
	public enum Difficulty {
		BASIC, INTERMEDIATE, ADVANCED
	}
}