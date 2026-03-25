package org.hane.cli.practice;

import org.hane.model.EvaluationResult;
import org.hane.model.InterviewQuestion;

/**
 * 面试流程控制器
 * 负责处理单道题目的完整流程：展示题目、获取回答、评估、追问、显示结果
 */
public class QuestionFlowController {
    
    private final InterviewSession session;
    
    public QuestionFlowController(InterviewSession session) {
        this.session = session;
    }
    
    /**
     * 执行单道题目的完整流程
     * @param question 当前题目
     * @param userAnswer 用户回答
     * @return 是否进行了追问
     */
    public boolean processQuestion(InterviewQuestion question, String userAnswer) {
        // 评估回答
        EvaluationResult result = session.evaluateAnswer(userAnswer, question);
        
        // 智能追问（基于评估结果）
        boolean hasFollowUp = false;
        if (result.score() < 9) {
            String followUpQuestion = session.generateFollowUp(result, question);
            displayFollowUp(followUpQuestion);
            hasFollowUp = true;
        }
        
        // 显示评估结果
        displayEvaluationResult(result);
        
        return hasFollowUp;
    }
    
    /**
     * 显示题目信息
     */
    public void displayQuestion(InterviewQuestion question, int questionIndex, int totalQuestions) {
        System.out.printf("\n========== 第 %d/%d 题 ==========%n", questionIndex, totalQuestions);
        System.out.println("\n📝 " + question.content());
        System.out.println("\n考察点：" + String.join(",", question.keyPoints()));
        System.out.println("难度：" + question.difficulty());
        if (question.source() != null) {
            System.out.println("来源：" + question.source());
        }
    }
    
    /**
     * 显示追问
     */
    private void displayFollowUp(String followUpQuestion) {
        System.out.println("\n🔍 AI 追问：" + followUpQuestion);
        System.out.println("\n💬 请回答追问（输入空行跳过）：");
    }
    
    /**
     * 显示评估结果
     */
    private void displayEvaluationResult(EvaluationResult result) {
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
    }
}
