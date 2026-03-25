package org.hane.cli.practice;

import org.hane.model.InterviewQuestion;

/**
 * 面试结果展示器
 * 负责显示面试的最终总结和评价
 */
public class InterviewResultDisplayer {
    
    /**
     * 显示面试结束总结
     * @param averageScore 平均分数
     * @param totalQuestions 总题数
     */
    public void displaySummary(double averageScore, int totalQuestions) {
        System.out.println("\n========== 面试结束 ==========");
        System.out.printf("📊 平均得分：%.1f/10%n", averageScore);
        
        String evaluation = getEvaluation(averageScore);
        System.out.println(evaluation);
    }
    
    /**
     * 根据平均分获取评价语
     */
    private String getEvaluation(double averageScore) {
        if (averageScore >= 9) {
            return "🌟 评价：表现优秀！可以冲击一线大厂";
        } else if (averageScore >= 7) {
            return "👍 评价：基础扎实，部分知识点需要深化";
        } else if (averageScore >= 5) {
            return "📚 评价：需要加强基础知识的学习";
        } else {
            return "💪 评价：建议系统学习相关技术栈";
        }
    }
}
