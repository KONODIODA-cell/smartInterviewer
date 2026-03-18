package org.hane.model;

import java.util.List;

/**
 * 评估结果（结构化 JSON）
 */
public record EvaluationResult(
		int score,                                    // 总分 0-10
		DimensionScores dimensionScores,              // 分项得分
		List<String> strengths,                       // 亮点
		List<String> weaknesses,                      // 不足
		List<String> missingPoints,                   // 遗漏点
		String suggestion,                            // 改进建议
		String followUp                               // 建议追问
) {
	public record DimensionScores(
			int accuracy,      // 准确性 0-10
			int completeness,  // 完整性 0-10
			int depth          // 深度 0-10
	) {}
}