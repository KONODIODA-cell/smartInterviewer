package org.hane.utils;


import lombok.Getter;
import lombok.ToString;

import java.nio.file.Files;
import java.nio.file.Path;

@ToString
public class AppConfig {
	public static String aiServiceUrl;
	public static String aiApiKey;
	public static String aiModelName;
	public static String mdPath;
	public static String dbPath;

	public void init() {
		// 从.env中获取环境变量
		DotenvLoader env = new DotenvLoader(Path.of(System.getProperty("user.dir")));

		// 从环境变量中获取
		// 环境变量中的优先级较高
		aiServiceUrl = System.getenv().getOrDefault("AI_SERVICE_URL", env.get("AI_SERVICE_URL"));
		aiApiKey = System.getenv().getOrDefault("AI_API_KEY", env.get("AI_API_KEY"));
		aiModelName = System.getenv().getOrDefault("AI_MODEL_NAME", env.get("AI_MODEL_NAME"));
		mdPath = System.getenv().getOrDefault("MD_PATH", env.get("MD_PATH"));
		dbPath = System.getenv().getOrDefault("DB_PATH", env.get("DB_PATH"));

		// 变量校验
		this.validate();
	}

	private void validate() {
		StringBuilder errors = new StringBuilder();
		
		// 验证 API Key（必需）
		if (aiApiKey == null || aiApiKey.isBlank()) {
			errors.append("❌ API Key 未配置\n");
			errors.append("   设置方式:\n");
			errors.append("     1. 环境变量：export AI_API_KEY=sk-xxx\n");
			errors.append("     2. .env 文件：echo 'AI_API_KEY=sk-xxx' > .env\n");
			errors.append("     3. 命令行参数：--api-key=sk-xxx\n\n");
		}
		
		// 验证 AI 服务地址（可选，但有默认值）
		if (aiServiceUrl == null || aiServiceUrl.isBlank()) {
			errors.append("❌ AI 服务地址未配置\n");
			errors.append("   设置方式：export AI_SERVICE_URL=https://api.openai.com/v1\n\n");
		}
		
		// 验证模型名称（可选）
		if (aiModelName == null || aiModelName.isBlank()) {
			errors.append("⚠️  模型名称未配置，将使用默认模型\n");
			errors.append("   设置方式：export AI_MODEL_NAME=gpt-4\n\n");
		}
		
		// 验证 MD 路径（必需）
		if (mdPath == null || mdPath.isBlank()) {
			errors.append("❌ Markdown 文件路径未配置\n");
			errors.append("   设置方式：export MD_PATH=/path/to/markdown\n\n");
		} else if (!Files.exists(Path.of(mdPath))) {
			errors.append("❌ Markdown 文件/目录不存在: ").append(mdPath).append("\n\n");
		}
		
		// 验证 DB 路径（可选，但需要检查父目录是否存在）
		if (dbPath != null && !dbPath.isBlank()) {
			Path dbFilePath = Path.of(dbPath);
			Path parentDir = dbFilePath.getParent();
			if (parentDir != null && !Files.exists(parentDir)) {
				errors.append("❌ 数据库文件父目录不存在: ").append(parentDir).append("\n\n");
			}
		}
		
		// 如果有错误，抛出异常
		if (errors.length() > 0) {
			throw new IllegalStateException("\n" + errors.toString().trim());
		}
	}
}