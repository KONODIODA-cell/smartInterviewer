package org.hane.service.practiceService;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.hane.model.EvaluationResult;
import org.hane.model.InterviewQuestion;

/**
 * AI 面试服务接口
 * 注意：SystemMessage 在创建 AiServices 时动态设置，而非固定注解
 */
public interface InterviewAiService {

	/**
	 * 基于向量库检索生成题目（RAG 模式）
	 * 需要配合 SystemMessage 使用，包含人格设定
	 */
	@UserMessage("""
			基于以下参考资料生成面试题：
			
			{{context}}
			
			要求：
			1. 从参考资料中选择真实出现过的技术点（保证题目来源于真实面试）
			2. 标注题目来源（如"这是一道来自阿里巴巴三面的题目"）
			3. 难度分层：
			   - 基础：概念理解（如 HashMap 原理）
			   - 进阶：场景设计（如高并发下的缓存一致性）
			   - 挑战：架构权衡（如分布式事务的 CAP 取舍）
			4. 问题要开放，避免"是/否"能回答的
			
			输出格式：
			题目：[具体问题]
			考察点：[关键知识点列表]
			难度：[基础/进阶/挑战]
			来源：[公司]-[轮次]
			""")
	InterviewQuestion generateQuestion(@V("context") String context);

	/**
	 * 评估用户回答（结构化输出）
	 * 需要配合 SystemMessage 使用，包含人格设定
	 */
	@UserMessage("""
			参考资料（标准答案要点）：
			{{reference}}
			
			用户回答：
			{{answer}}
			
			评估维度（满分 10 分）：
			1. 准确性（40%）：技术细节是否正确（如 CMS 和 G1 的区别是否说反）
			2. 完整性（30%）：是否覆盖参考答案的关键要点（缺 1 个扣 2 分）
			3. 深度（30%）：是否触及原理层（如不只说"用 Redis"，还能说"为什么不用本地缓存"）
			
			评分标准：
			- 9-10 分：优秀，可进下一轮
			- 7-8 分：良好，有小瑕疵
			- 5-6 分：及格，需补充学习
			- <5 分：明显知识盲区
			
			必须按 JSON 格式返回：
			{
			  "score": 8,
			  "dimension_scores": {
			    "accuracy": 8,
			    "completeness": 7,
			    "depth": 9
			  },
			  "strengths": ["亮点 1", "亮点 2"],
			  "weaknesses": ["不足 1", "不足 2"],
			  "missing_points": ["遗漏的关键知识点"],
			  "suggestion": "具体的改进建议",
			  "follow_up": "如果要追问，可以问：..."
			}
			""")
	EvaluationResult evaluateAnswer(@V("reference") String reference,
	                                @V("answer") String answer);

	/**
	 * 追问生成（当回答不够深入时）
	 * 需要配合 SystemMessage 使用，包含人格设定
	 */
	@UserMessage("""
			基于之前的问答，提出一个更深入的追问：
			
			{{previousQa}}
			
			追问应该具体、有针对性，挖掘候选人对底层原理的理解。
			""")
	String generateFollowUp(@V("previousQa") String previousQa);
}
