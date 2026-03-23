package org.hane.service.initService;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.hane.model.InterviewerPersona;
import org.hane.utils.AppConfig;
import org.hane.utils.DuckDb;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

import static java.sql.DriverManager.*;

/**
 * 面试官人格生成器
 * 采用分批处理策略，每批使用 LangChain4j 自动结构体映射
 */
public class InterviewerPersonaGenerator {

	// 每批处理的文件数量
	private static final int BATCH_SIZE = 30;
	// 最多生成的人格数量
	private static final int MAX_PERSONAS = 3;

	/**
	 * 基于所有文件路径，分批生成面试官人格
	 * 策略：将文件分成多组，每组调用一次 AI 生成一个人格
	 *
	 * @param mdFiles Markdown 文件列表
	 * @return 人格列表（1-3个）
	 */
	public List<InterviewerPersona> generatePersonasFromFiles(List<Path> mdFiles) {
		if (mdFiles.isEmpty()) {
			return List.of();
		}

		// 文件较少时，直接生成一个默认人格
		if (mdFiles.size() <= 10) {
			return List.of(createDefaultPersona("通用", mdFiles));
		}

		System.out.println("  🤖 AI 正在分析文件结构并生成面试官人格...");
		System.out.println("  📄 共 " + mdFiles.size() + " 个文件，将分批处理");

		List<InterviewerPersona> personas = new ArrayList<>();
		PersonaAiService personaAiService = createPersonaAiService();

		// 将文件分组
		List<List<Path>> batches = splitIntoBatches(mdFiles, BATCH_SIZE);
		int batchCount = Math.min(batches.size(), MAX_PERSONAS);

		for (int i = 0; i < batchCount; i++) {
			List<Path> batch = batches.get(i);
			System.out.println("  📝 处理第 " + (i + 1) + "/" + batchCount + " 批 (" + batch.size() + " 个文件)...");

			try {
				// 构建文件路径字符串
				String filePathsStr = batch.stream()
						.map(path -> {
							Path parent = path.getParent();
							String dir = parent != null ? parent.getFileName().toString() : "";
							String file = path.getFileName().toString();
							return dir + "/" + file;
						})
						.collect(Collectors.joining("\n"));

				// 调用 AI 生成人格（LangChain4j 自动映射到对象）
				InterviewerPersona persona = personaAiService.generatePersona(filePathsStr);

				// 补充缺失字段
				if (persona.getId() == null || persona.getId().isBlank()) {
					persona.setId(UUID.randomUUID().toString());
				}
				if (persona.getFilePatterns() == null || persona.getFilePatterns().isEmpty()) {
					// 从文件路径提取关键词
					String patterns = extractKeywords(batch);
					persona.setFilePatterns(patterns);
				}
				if (persona.getSystemPrompt() == null || persona.getSystemPrompt().isEmpty()) {
					persona.setSystemPrompt(getDefaultSystemPrompt());
				}

				personas.add(persona);
				System.out.println("    ✓ 生成人格：" + persona.getName() + " (" + persona.getExpertise() + ")");

				// 添加延迟，避免 API 限流
				if (i < batchCount - 1) {
					Thread.sleep(500);
				}

			} catch (Exception e) {
				System.err.println("    ✗ 第 " + (i + 1) + " 批生成失败：" + e.getMessage());
				e.printStackTrace();
				// 使用默认人格作为后备
				InterviewerPersona defaultPersona = createDefaultPersona("批次" + (i + 1), batch);
				personas.add(defaultPersona);
			}
		}

		// 如果所有批次都失败，返回默认人格
		if (personas.isEmpty()) {
			System.out.println("  ⚠️ 所有批次都失败，使用默认人格");
			return List.of(createDefaultPersona("通用", mdFiles));
		}

		System.out.println("  ✅ 成功生成 " + personas.size() + " 种面试官人格");
		return personas;
	}

	/**
	 * 将文件列表分批
	 */
	private List<List<Path>> splitIntoBatches(List<Path> mdFiles, int batchSize) {
		List<List<Path>> batches = new ArrayList<>();
		
		// 按目录分组
		Map<String, List<Path>> byDir = new LinkedHashMap<>();
		for (Path file : mdFiles) {
			Path parent = file.getParent();
			String dir = parent != null ? parent.toString() : "default";
			byDir.computeIfAbsent(dir, k -> new ArrayList<>()).add(file);
		}

		// 将目录分组转换为批次
		List<Path> currentBatch = new ArrayList<>();
		for (List<Path> dirFiles : byDir.values()) {
			if (currentBatch.size() + dirFiles.size() <= batchSize) {
				currentBatch.addAll(dirFiles);
			} else {
				if (!currentBatch.isEmpty()) {
					batches.add(new ArrayList<>(currentBatch));
					currentBatch.clear();
				}
				// 如果单个目录文件太多，需要拆分
				if (dirFiles.size() > batchSize) {
					for (int i = 0; i < dirFiles.size(); i += batchSize) {
						batches.add(dirFiles.subList(i, Math.min(i + batchSize, dirFiles.size())));
					}
				} else {
					currentBatch.addAll(dirFiles);
				}
			}
		}
		if (!currentBatch.isEmpty()) {
			batches.add(currentBatch);
		}

		return batches;
	}

	/**
	 * 从文件路径提取关键词
	 */
	private String extractKeywords(List<Path> files) {
		Set<String> keywords = new HashSet<>();
		for (Path file : files) {
			String name = file.getFileName().toString().toLowerCase();
			// 提取有意义的词
			if (name.contains("java")) keywords.add("Java");
			if (name.contains("spring")) keywords.add("Spring");
			if (name.contains("jvm")) keywords.add("JVM");
			if (name.contains("redis")) keywords.add("Redis");
			if (name.contains("mysql")) keywords.add("MySQL");
			if (name.contains("算法")) keywords.add("算法");
			if (name.contains("system")) keywords.add("System Design");
			if (name.contains("分布式")) keywords.add("分布式");
			if (name.contains("微服务")) keywords.add("微服务");
			if (name.contains("阿里")) keywords.add("阿里");
			if (name.contains("字节")) keywords.add("字节");
			if (name.contains("腾讯")) keywords.add("腾讯");
		}
		if (keywords.isEmpty()) {
			return "通用,技术";
		}
		return String.join(",", keywords);
	}

	/**
	 * 获取默认系统提示词
	 */
	private String getDefaultSystemPrompt() {
		return """
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
			""";
	}

	/**
	 * 保存人格到数据库
	 * @param personas 人格列表
	 */
	public void savePersonas(List<InterviewerPersona> personas) {
		for (InterviewerPersona persona : personas) {
			try(Connection conn = DuckDb.getConn()) {
				// 1. 创建人格表（如果不存在）
				try (Statement stmt = conn.createStatement()) {
					stmt.execute("""
						CREATE TABLE IF NOT EXISTS interviewer_personas (
							id VARCHAR PRIMARY KEY,
							name VARCHAR NOT NULL,
							description VARCHAR,
							system_prompt TEXT,
							file_patterns VARCHAR,
							expertise VARCHAR,
							style VARCHAR,
							difficulty_bias VARCHAR,
							priority INT,
							created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
						)
					""");
				}

				// 2. 插入或更新人格记录
				try (PreparedStatement ps = conn.prepareStatement("""
					INSERT OR REPLACE INTO interviewer_personas 
					(id, name, description, system_prompt, file_patterns, expertise, style, difficulty_bias, priority)
					VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
				""")) {
					ps.setString(1, persona.getId());
					ps.setString(2, persona.getName());
					ps.setString(3, persona.getDescription());
					ps.setString(4, persona.getSystemPrompt());
					ps.setString(5, persona.getFilePatterns());
					ps.setString(6, persona.getExpertise());
					ps.setString(7, persona.getStyle());
					ps.setString(8, persona.getDifficultyBias());
					ps.setInt(9, persona.getPriority());
					ps.executeUpdate();
				}

				System.out.println("  💾 已保存人格：" + persona.getName());
			} catch (Exception e) {
				System.err.println("  ✗ 保存人格 '" + persona.getName() + "' 失败：" + e.getMessage());
			}
		}
	}

	/**
	 * 创建 AI 服务用于生成人格
	 */
	private PersonaAiService createPersonaAiService() {
		ChatModel model = OpenAiChatModel.builder()
				.baseUrl(AppConfig.aiServiceUrl)
				.apiKey(AppConfig.aiApiKey)
				.modelName(AppConfig.aiModelName)
				.temperature(1.0)
				.build();

		return AiServices.builder(PersonaAiService.class)
				.chatModel(model)
				.build();
	}

	/**
	 * 创建默认人格（当 AI 生成失败时使用）
	 */
	private InterviewerPersona createDefaultPersona(String groupName, List<Path> files) {
		String id = UUID.randomUUID().toString();
		String name = groupName + "面试官";
		String description = "基于 " + groupName + " 面经文档的默认面试官人格";
		String patterns = extractKeywords(files);

		InterviewerPersona persona = new InterviewerPersona();
		persona.setId(id);
		persona.setName(name);
		persona.setDescription(description);
		persona.setSystemPrompt(getDefaultSystemPrompt());
		persona.setFilePatterns(patterns);
		persona.setExpertise("通用技术");
		persona.setStyle("专业型");
		persona.setDifficultyBias("INTERMEDIATE");
		persona.setPriority(5);
		return persona;
	}
}
