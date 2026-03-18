package org.hane.service.initService;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.hane.model.InterviewerPersona;

/**
 * AI 人格生成服务
 * 使用 LangChain4j 自动结构体映射
 */
public interface PersonaAiService {

	/**
	 * 基于文件路径列表分析并生成单个面试官人格
	 * LangChain4j 自动将 JSON 映射到 InterviewerPersona 对象
	 */
	@SystemMessage("""
		你是一个专业的 AI 面试官人格设计专家。
		
		你的任务是根据用户提供的 markdown 文件路径列表，分析这些文件可能包含的内容，
		然后设计一个最适合面试这些内容的 AI 面试官人格。
		
		分析维度：
		1. 专业领域：从文件路径推断技术方向（如 Java、Python、前端、算法等）
		2. 公司背景：从文件名推断公司风格（如阿里重视分布式、字节重视算法等）
		3. 难度层次：根据文件内容推测面试难度
		4. 面试风格：根据公司文化推断
		
		必须按以下 JSON 格式返回：
		{
		  "id": "uuid字符串",
		  "name": "人格名称（如：阿里巴巴Java专家）",
		  "description": "一句话描述这个人格的特点",
		  "systemPrompt": "完整的系统提示词，定义这个 AI 面试官的行为方式、语言风格、考察重点",
		  "filePatterns": "关键词1,关键词2,关键词3",
		  "expertise": "专业领域（如：Java后端）",
		  "style": "面试风格（如：严谨型、亲和型、挑战型）",
		  "difficultyBias": "难度偏好（BASIC/INTERMEDIATE/ADVANCED）",
		  "priority": 5
		}
		""")
	@UserMessage("""
		请分析以下 markdown 文件路径，生成对应的 AI 面试官人格：
		
		文件路径列表：
		{{filePaths}}
		
		要求：
		1. 分析目录结构和文件名语义，判断文件涵盖的技术领域
		2. 为该领域设计一个专业的面试官人格
		3. filePatterns 使用逗号分隔的关键词
		4. 只返回 JSON 对象，不要任何其他文字
		""")
	InterviewerPersona generatePersona(@V("filePaths") String filePaths);
}
