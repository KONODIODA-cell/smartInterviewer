package org.hane.service.initService;

import dev.langchain4j.community.store.embedding.duckdb.DuckDBEmbeddingStore;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallzh.BgeSmallZhEmbeddingModel;
import org.hane.model.InterviewerPersona;
import org.hane.service.initService.DocumentProcessor.ProcessResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * 知识库服务
 * 负责协调文档处理和 AI 面试官人格生成
 */
public class KnowledgeBaseService {

	private final DocumentProcessor documentProcessor;
	private final InterviewerPersonaGenerator personaGenerator;

	public KnowledgeBaseService() {
		this.documentProcessor = new DocumentProcessor();
		this.personaGenerator = new InterviewerPersonaGenerator();
	}

	/**
	 * 初始化知识库，构建向量索引
	 *
	 * @param dbPath 向量数据库文件路径
	 * @param mdPath Markdown 文件扫描路径
	 * @return 处理结果
	 */
	public InitResult init(String dbPath, String mdPath) {
		System.out.println("🚀 初始化知识库...");
		System.out.println("📂 扫描：" + mdPath);

		// 向量数据库
		DuckDBEmbeddingStore ragDb = DuckDBEmbeddingStore
				.builder()
				.filePath(dbPath)
				.build();

		// 嵌入模型 (中文优化)
		EmbeddingModel embeddingModel = new BgeSmallZhEmbeddingModel();

		// 扫描 Markdown 文件
		List<Path> mdFiles;
		try {
			mdFiles = documentProcessor.scanMarkdownFiles(mdPath);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("✗ 处理失败：" + e.getMessage());
			return new InitResult(false, 0, 0, 0, e.getMessage());
		}

		// 处理文档并构建向量索引
		ProcessResult processResult = documentProcessor
				.processDocuments(mdFiles, embeddingModel, ragDb);

		if (!processResult.success()) {
			return new InitResult(
					false,
					processResult.processedFiles(),
					processResult.errorFiles(),
					0,
					processResult.errorMessage()
			);
		}

		// 生成 AI 面试官人格
		System.out.println("🎭 正在分析文件结构，生成 AI 面试官人格...");
		List<InterviewerPersona> personas = personaGenerator
				.generatePersonasFromFiles(mdFiles);
		System.out.println("✅ 生成了 " + personas.size() + " 个 AI 面试官人格");

		// 保存人格到向量数据库
		System.out.println("💾 正在保存人格到向量数据库...");
		personaGenerator.savePersonasToVectorStore(personas, ragDb, embeddingModel);
		System.out.println("✅ 人格保存完成");

		return new InitResult(
				true,
				processResult.processedFiles(),
				processResult.errorFiles(),
				personas.size(),
				null
		);
	}

	/**
	 * 初始化结果
	 */
	public record InitResult(boolean success, int processedFiles, int errorFiles,
	                         int personaCount, String errorMessage) {
	}
}
