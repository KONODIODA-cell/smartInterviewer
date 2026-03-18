package org.hane.service.practiceService;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.SystemMessage;

/**
 * 带工具调用的服务（让 AI 主动检索 DuckDB）
 */
public interface InterviewAiServiceWithTools {

	@SystemMessage("""
			你是一位 Java 面试官。你可以访问知识库检索相关面试题。
			当用户要求出题时，先调用 searchKnowledge 获取相关资料，再生成题目。
			""")
	String chat(String message);

	/**
	 * 检索工具（供 AI 自主调用）
	 */
	@Tool("从 DuckDB 向量库中检索与主题相关的面试题和答案")
	default String searchKnowledge(@P("查询主题，如：JVM垃圾回收、Redis分布式锁") String topic,
	                               @P("限制条数") int limit) {
		// 实际实现会注入 InterviewKnowledgeBase
		return "";
	}
}
