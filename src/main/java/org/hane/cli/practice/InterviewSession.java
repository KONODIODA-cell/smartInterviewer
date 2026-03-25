package org.hane.cli.practice;

import org.hane.model.EvaluationResult;
import org.hane.model.InterviewQuestion;
import org.hane.model.InterviewerPersona;
import org.hane.service.practiceService.InterviewSessionService;

/**
 * 面试会话状态管理器
 * 负责维护面试过程中的所有状态信息
 */
public class InterviewSession {
    
    private final InterviewSessionService sessionService;
    private final int totalQuestions;
    private int currentQuestionIndex;
    private int totalScore;
    
    public InterviewSession(InterviewerPersona persona, int questionCount, 
                           InterviewQuestion.Difficulty difficulty) {
        this.sessionService = new InterviewSessionService(persona, questionCount, difficulty);
        this.totalQuestions = questionCount;
        this.currentQuestionIndex = 0;
        this.totalScore = 0;
    }
    
    /**
     * 获取下一道面试题
     */
    public InterviewQuestion nextQuestion() {
        currentQuestionIndex++;
        return sessionService.nextQuestion();
    }
    
    /**
     * 评估用户回答
     */
    public EvaluationResult evaluateAnswer(String userAnswer, InterviewQuestion question) {
        EvaluationResult result = sessionService.evaluate(userAnswer, question);
        totalScore += result.score();
        return result;
    }
    
    /**
     * 生成追问问题
     */
    public String generateFollowUp(EvaluationResult result, InterviewQuestion question) {
        return sessionService.generateFollowUp(result, question);
    }
    
    /**
     * 获取当前题号
     */
    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }
    
    /**
     * 获取总题数
     */
    public int getTotalQuestions() {
        return totalQuestions;
    }
    
    /**
     * 获取累计总分
     */
    public int getTotalScore() {
        return totalScore;
    }
    
    /**
     * 计算平均分
     */
    public double getAverageScore() {
        if (currentQuestionIndex == 0) {
            return 0.0;
        }
        return (double) totalScore / currentQuestionIndex;
    }
    
    /**
     * 是否还有下一题
     */
    public boolean hasMoreQuestions() {
        return currentQuestionIndex < totalQuestions;
    }
}
