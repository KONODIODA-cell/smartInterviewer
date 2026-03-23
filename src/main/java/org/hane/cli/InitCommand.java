package org.hane.cli;

import org.hane.service.initService.KnowledgeBaseService;
import org.hane.utils.AppConfig;
import picocli.CommandLine.Command;

@Command(name = "init", description = "扫描 Markdown 建立向量索引")
public class InitCommand implements Runnable {

	@Override
	public void run() {
		KnowledgeBaseService knowledgeBaseService = new KnowledgeBaseService();
		KnowledgeBaseService.InitResult result = knowledgeBaseService.init();

		if (!result.success()) {
			System.exit(1);
		}
	}
}
