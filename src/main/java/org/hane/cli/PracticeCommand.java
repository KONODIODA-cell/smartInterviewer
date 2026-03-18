package org.hane.cli;

import org.hane.model.EvaluationResult;
import org.hane.model.InterviewQuestion;
import org.hane.model.InterviewerPersona;
import org.hane.service.practiceService.InterviewSessionService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Scanner;

/**
 * 模拟面试命令
 * 支持交互式面试流程，自动匹配 AI 面试官人格
 */
@Command(
		name = "start",
		description = "开始模拟面试",
		mixinStandardHelpOptions = true
)
public class PracticeCommand implements Runnable {

	@Option(
			names = {"-d", "--difficulty"},
			description = "题目难度：BASIC(基础)、INTERMEDIATE(进阶)、ADVANCED(挑战)",
			defaultValue = "INTERMEDIATE"
	)
	private InterviewQuestion.Difficulty difficulty;

	@Option(
			names = {"-c", "--count"},
			description = "面试题数量",
			defaultValue = "5"
	)
	private int questionCount;

	@Override
	public void run() {
		// 初始化面试会话服务（自动载入 AI 人格）
		InterviewSessionService sessionService = new InterviewSessionService();

		Scanner scanner = new Scanner(System.in);

		try {
			// 列出所有可用的人格供用户选择
			InterviewerPersona selectedPersona = selectPersona(sessionService, scanner);
			if (selectedPersona == null) {
				System.out.println("未选择面试官，退出面试。");
				return;
			}
			
			// 设置选定的人格
			sessionService.setPersona(selectedPersona);
			
			// 获取面试主题
			String topic = selectedPersona.expertise() != null && !selectedPersona.expertise().isEmpty() 
					? selectedPersona.expertise() 
					: "通用技术";
			
			// 开始面试
			System.out.println(sessionService.startInterview(topic));
			System.out.println("\n按 Enter 键开始第一题...");
			scanner.nextLine();

			int questionIndex = 1;
			int totalScore = 0;

			while (questionIndex <= questionCount) {
				// 生成题目
				System.out.printf("\n========== 第 %d/%d 题 ==========%n", questionIndex, questionCount);
				InterviewQuestion question = sessionService.nextQuestion(topic, difficulty);

				System.out.println("\n📝 " + question.content());
				System.out.println("\n考察点：" + String.join("、", question.keyPoints()));
				System.out.println("难度：" + question.difficulty());
				if (question.source() != null) {
					System.out.println("来源：" + question.source());
				}

				// 获取用户回答
				System.out.println("\n💬 请输入你的回答（输入空行结束）：");
				StringBuilder answerBuilder = new StringBuilder();
				String line;
				while (!(line = scanner.nextLine()).isEmpty()) {
					answerBuilder.append(line).append("\n");
				}
				String userAnswer = answerBuilder.toString().trim();

				if (userAnswer.isEmpty()) {
					System.out.println("⚠️ 回答为空，跳过本题...");
					continue;
				}

				// 评估回答
				System.out.println("\n🤖 AI 评估中...");
				EvaluationResult result = sessionService.evaluate(question.id(), userAnswer, question);

				// 显示评估结果
				System.out.println("\n📊 评估结果：");
				System.out.println("总分：" + result.score() + "/10");
				System.out.println("分项得分：");
				System.out.println("  - 准确性：" + result.dimensionScores().accuracy() + "/10");
				System.out.println("  - 完整性：" + result.dimensionScores().completeness() + "/10");
				System.out.println("  - 深度：" + result.dimensionScores().depth() + "/10");

				if (!result.strengths().isEmpty()) {
					System.out.println("\n✅ 亮点：");
					result.strengths().forEach(s -> System.out.println("  • " + s));
				}

				if (!result.weaknesses().isEmpty()) {
					System.out.println("\n⚠️ 不足：");
					result.weaknesses().forEach(w -> System.out.println("  • " + w));
				}

				if (!result.missingPoints().isEmpty()) {
					System.out.println("\n📌 遗漏的关键点：");
					result.missingPoints().forEach(m -> System.out.println("  • " + m));
				}

				System.out.println("\n💡 建议：" + result.suggestion());

				// 追问（根据得分决定是否追问）
				if (result.score() < 9) {
					System.out.println("\n🔍 追问：" + result.followUp());
					System.out.println("\n💬 请回答追问（输入空行跳过）：");
					StringBuilder followUpBuilder = new StringBuilder();
					while (!(line = scanner.nextLine()).isEmpty()) {
						followUpBuilder.append(line).append("\n");
					}
					String followUpAnswer = followUpBuilder.toString().trim();

					if (!followUpAnswer.isEmpty()) {
						System.out.println("✅ 已记录追问回答");
					}
				}

				totalScore += result.score();
				questionIndex++;

				if (questionIndex <= questionCount) {
					System.out.println("\n按 Enter 键进入下一题...");
					scanner.nextLine();
				}
			}

			// 面试结束，显示总结
			double averageScore = (double) totalScore / questionCount;
			System.out.println("\n========== 面试结束 ==========");
			System.out.printf("📊 平均得分：%.1f/10%n", averageScore);

			if (averageScore >= 9) {
				System.out.println("🌟 评价：表现优秀！可以冲击一线大厂");
			} else if (averageScore >= 7) {
				System.out.println("👍 评价：基础扎实，部分知识点需要深化");
			} else if (averageScore >= 5) {
				System.out.println("📚 评价：需要加强基础知识的学习");
			} else {
				System.out.println("💪 评价：建议系统学习相关技术栈");
			}

		} catch (Exception e) {
			System.err.println("❌ 面试过程出错：" + e.getMessage());
			e.printStackTrace();
		} finally {
			scanner.close();
		}
	}
	
	/**
	 * 交互式选择面试官人格
	 * 
	 * @param sessionService 会话服务
	 * @param scanner 输入扫描器
	 * @return 选定的人格，如果用户取消则返回 null
	 */
	private InterviewerPersona selectPersona(InterviewSessionService sessionService, Scanner scanner) {
		List<InterviewerPersona> personas = sessionService.getAllPersonas();
		
		if (personas.isEmpty()) {
			System.out.println("⚠️ 没有找到可用的面试官人格，将使用默认人格。");
			return null;
		}
		
		System.out.println("\n🎭 请选择 AI 面试官人格：\n");
		
		// 显示所有可用人格
		for (int i = 0; i < personas.size(); i++) {
			InterviewerPersona persona = personas.get(i);
			System.out.printf("  [%d] %s\n", i + 1, persona.name());
			System.out.printf("      专业领域：%s\n", persona.expertise() != null ? persona.expertise() : "通用");
			System.out.printf("      面试风格：%s\n", persona.style() != null ? persona.style() : "标准");
			if (persona.description() != null && !persona.description().isEmpty()) {
				System.out.printf("      简介：%s\n", persona.description());
			}
			System.out.println();
		}
		
		// 循环直到用户做出有效选择
		while (true) {
			System.out.print("请输入序号选择面试官 (1-" + personas.size() + ", 输入 0 取消): ");
			String input = scanner.nextLine().trim();
			
			try {
				int choice = Integer.parseInt(input);
				
				if (choice == 0) {
					return null;
				}
				
				if (choice >= 1 && choice <= personas.size()) {
					return personas.get(choice - 1);
				}
				
				System.out.println("⚠️ 无效的选择，请输入 1-" + personas.size() + " 之间的数字。");
				
			} catch (NumberFormatException e) {
				System.out.println("⚠️ 请输入有效的数字。");
			}
		}
	}
}
