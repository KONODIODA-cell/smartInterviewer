package org.hane.service.initService;

import dev.langchain4j.community.store.embedding.duckdb.DuckDBEmbeddingStore;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.markdown.MarkdownDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 文档处理器
 * 负责 Markdown 文件的扫描、解析、分块和向量化
 */
public class DocumentProcessor {

	// 批量处理大小 - 避免内存溢出
	private static final int BATCH_SIZE = 100;
	// 并行处理文件数
	private static final int PARALLEL_FILES = 4;

	private final MarkdownDocumentParser mdParser;
	private final DocumentSplitter docSplitter;

	public DocumentProcessor() {
		this.mdParser = new MarkdownDocumentParser();
		this.docSplitter = DocumentSplitters.recursive(500, 50);
	}

	/**
	 * 扫描指定路径下的所有 Markdown 文件
	 *
	 * @param mdPath 扫描路径
	 * @return Markdown 文件路径列表
	 * @throws IOException 文件扫描失败时抛出
	 */
	public List<Path> scanMarkdownFiles(String mdPath) throws IOException {
		return Files.walk(Path.of(mdPath))
				.filter(p -> p.toString().endsWith(".md"))
				.toList();
	}

	/**
	 * 处理文档并构建向量索引
	 *
	 * @param mdFiles        Markdown 文件列表
	 * @param embeddingModel 嵌入模型
	 * @param ragDb          向量数据库
	 * @return 处理结果
	 */
	public ProcessResult processDocuments(List<Path> mdFiles,
	                                      EmbeddingModel embeddingModel,
	                                      DuckDBEmbeddingStore ragDb) {
		System.out.println("📄 发现 " + mdFiles.size() + " 个 md 文件");

		AtomicInteger processedCount = new AtomicInteger(0);
		AtomicInteger errorCount = new AtomicInteger(0);

		try (ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_FILES)) {
			List<Future<List<TextSegment>>> futures = new ArrayList<>();

			for (Path path : mdFiles) {
				Future<List<TextSegment>> future = executor.submit(() -> {
					try (FileInputStream fis = new FileInputStream(path.toFile())) {
						Document document = mdParser.parse(fis);

						// 为数据添加元信息
						Map<String, Object> metaMap = Map.of(
								"file_name", path.getFileName().toString(),
								"file_path", path.toAbsolutePath().toString(),
								"indexed_at", System.currentTimeMillis());
						document.metadata().putAll(metaMap);

						// 分块处理
						List<TextSegment> textSegments = docSplitter.split(document);
						textSegments.forEach(e -> e.metadata().putAll(metaMap));

						processedCount.incrementAndGet();
						return textSegments;
					} catch (Exception e) {
						errorCount.incrementAndGet();
						System.err.println("✗ 处理失败：" + path + " - " + e.getMessage());
						return new ArrayList<>();
					}
				});
				futures.add(future);
			}

			// 分批处理结果，避免内存溢出
			List<TextSegment> batchSegments = new ArrayList<>(BATCH_SIZE);
			int batchNum = 0;

			for (Future<List<TextSegment>> future : futures) {
				batchSegments.addAll(future.get());

				// 达到批量大小时，立即处理并清空
				if (batchSegments.size() >= BATCH_SIZE) {
					batchNum++;
					System.out.printf("⚙️ 处理批次 %d (%d 个片段)%n", batchNum, batchSegments.size());
					processBatch(embeddingModel, ragDb, batchSegments);
					batchSegments = new ArrayList<>(BATCH_SIZE);
				}
			}

			// 处理剩余片段
			if (!batchSegments.isEmpty()) {
				batchNum++;
				System.out.printf("⚙️ 处理批次 %d (%d 个片段)%n", batchNum, batchSegments.size());
				processBatch(embeddingModel, ragDb, batchSegments);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			System.err.println("✗ 处理被中断：" + e.getMessage());
			return new ProcessResult(false, processedCount.get(), errorCount.get(), e.getMessage());
		} catch (ExecutionException e) {
			System.err.println("✗ 处理异常：" + e.getCause().getMessage());
			return new ProcessResult(false, processedCount.get(), errorCount.get(), e.getCause().getMessage());
		}

		System.out.println("✅ 面经文档处理完成！处理：" + processedCount + " 文件，失败：" + errorCount + " 文件");
		return new ProcessResult(true, processedCount.get(), errorCount.get(), null);
	}

	/**
	 * 批量处理片段并入库
	 */
	private void processBatch(EmbeddingModel embeddingModel, DuckDBEmbeddingStore ragDb,
	                          List<TextSegment> segments) {
		if (segments.isEmpty()) {
			return;
		}
		List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
		// 同时存入向量和对应的 TextSegment，否则检索时 embedded() 会返回 null
		ragDb.addAll(embeddings, segments);
	}

	/**
	 * 文档处理结果
	 */
	public record ProcessResult(boolean success, int processedFiles, int errorFiles, String errorMessage) {
	}
}
