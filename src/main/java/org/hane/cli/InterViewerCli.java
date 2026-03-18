package org.hane.cli;

import org.hane.utils.AppConfig;
import picocli.CommandLine;

import static picocli.CommandLine.*;

@Command(
		name = "ai-interviewer",
		version = "1.0.0",
		description = "AI 面试官",
		mixinStandardHelpOptions = true,
		subcommands = {InitCommand.class, PracticeCommand.class}
)
public class InterViewerCli {

	public static void main(String[] args) {
		new AppConfig().init();
		int exitCode = new CommandLine(new InterViewerCli())
				.setExecutionExceptionHandler((ex, cmd, parseResult) -> {
					System.err.println("❌ 错误：" + ex.getMessage());
					return 1;
				})
				.execute(args);
		System.exit(exitCode);
	}
}
