package org.hane.cli;

import org.hane.cli.practice.*;
import org.hane.model.InterviewQuestion;
import org.hane.model.InterviewerPersona;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

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

    // 组件注入
    private final PersonaSelector personaSelector;
    private final UserInputHandler inputHandler;

    public PracticeCommand() {
        this.personaSelector = new PersonaSelector();
        this.inputHandler = new UserInputHandler();
    }

    @Override
    public void run() {
        try {
            // 1. 选择面试官人格
            InterviewerPersona selectedPersona = selectPersona();
            if (selectedPersona == null) {
                System.out.println("已取消面试");
                return;
            }

            // 2. 初始化面试会话
            InterviewSession session = new InterviewSession(selectedPersona, questionCount, difficulty);
            QuestionFlowController flowController = new QuestionFlowController(session);
            InterviewResultDisplayer resultDisplayer = new InterviewResultDisplayer();

            // 3. 开始面试流程
            executeInterview(session, flowController, resultDisplayer);

        } catch (Exception e) {
            System.err.println("❌ 面试过程出错：" + e.getMessage());
            e.printStackTrace();
        } finally {
            inputHandler.close();
        }
    }

    /**
     * 选择面试官人格
     */
    private InterviewerPersona selectPersona() {
        try {
            return personaSelector.selectPersona(inputHandler);
        } catch (Exception e) {
            System.out.println("查询面试官人格失败：" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行完整的面试流程
     */
    private void executeInterview(InterviewSession session, 
                                  QuestionFlowController flowController,
                                  InterviewResultDisplayer resultDisplayer) {
        // 等待用户开始
        inputHandler.waitForEnter("\n按 Enter 键开始第一题...");

        // 逐题进行面试
        while (session.hasMoreQuestions()) {
            processSingleQuestion(session, flowController);
            
            // 如果不是最后一题，等待进入下一题
            if (session.hasMoreQuestions()) {
                inputHandler.waitForEnter("\n按 Enter 键进入下一题...");
            }
        }

        // 显示面试总结
        resultDisplayer.displaySummary(session.getAverageScore(), session.getTotalQuestions());
    }

    /**
     * 处理单道题目的完整流程
     */
    private void processSingleQuestion(InterviewSession session, 
                                       QuestionFlowController flowController) {
        // 生成并显示题目
        InterviewQuestion question = session.nextQuestion();
        flowController.displayQuestion(question, 
            session.getCurrentQuestionIndex(), 
            session.getTotalQuestions());

        // 获取用户回答
        String userAnswer = inputHandler.getUserAnswer();
        if (userAnswer.isEmpty()) {
            System.out.println("⚠️ 回答为空，跳过本题...");
            return;
        }

        // 处理回答（评估、追问、显示结果）
        boolean hasFollowUp = flowController.processQuestion(question, userAnswer);

        // 获取追问回答（如果有追问）
        if (hasFollowUp) {
            String followUpAnswer = inputHandler.getFollowUpAnswer();
            if (!followUpAnswer.isEmpty()) {
                System.out.println("✅ 已记录追问回答");
            }
        }
    }
}
