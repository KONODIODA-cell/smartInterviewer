package org.hane.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 评估结果（结构化 JSON）
 */
public record EvaluationResult(
		int score,                                    // 总分 0-10
		@JsonProperty("dimension_scores") DimensionScores dimensionScores,              // 分项得分
		List<String> strengths,                       // 亮点
		List<String> weaknesses,                      // 不足
		@JsonProperty("missing_points") List<String> missingPoints,                   // 遗漏点
		String suggestion,                            // 改进建议
		@JsonProperty("follow_up") String followUp                               // 建议追问
) {
	public record DimensionScores(
			@JsonProperty("accuracy") int accuracy,      // 准确性 0-10
			@JsonProperty("completeness") int completeness,  // 完整性 0-10
			@JsonProperty("depth") int depth          // 深度 0-10
	) {}
}