package org.hane.service.practiceService;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.hane.model.EvaluationResult;
import org.hane.model.InterviewQuestion;
import org.hane.model.InterviewReference;
import org.hane.model.InterviewerPersona;
import org.hane.utils.AppConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 面试会话服务
 * 负责管理面试流程、AI 交互、人格切换等
 */
public class InterviewSessionService {

    private InterviewAiService aiService;
    private final InterviewKnowledgeBase knowledgeBase;
    private final ChatMemory chatMemory;
    private final ChatModel chatModel;
    
    private InterviewerPersona currentPersona;
    private String currentTopic;

    public InterviewSessionService() {
        // 初始化 ChatModel（所有 AI 服务共用）
        this.chatModel = OpenAiChatModel.builder()
                .baseUrl(AppConfig.aiServiceUrl)
                .apiKey(AppConfig.aiApiKey)
                .modelName(AppConfig.aiModelName)
                .temperature(1.0)
                .build();

        // 初始化知识库
        this.knowledgeBase = new InterviewKnowledgeBase(Path.of(AppConfig.dbPath));
        
        // 初始化对话历史
        this.chatMemory = MessageWindowChatMemory.withMaxMessages(20);
        
        // 默认加载通用人格
        this.currentPersona = knowledgeBase.getDefaultPersona();
        this.currentTopic = "通用技术";
        
        // 根据人格创建 AI 服务
        rebuildAiService();
        
        System.out.println("🎭 当前面试官人格：" + currentPersona.name());
        System.out.println("   专业领域：" + currentPersona.expertise());
        System.out.println("   面试风格：" + currentPersona.style());
    }

    /**
     * 根据当前人格重建 AI 服务
     */
    private void rebuildAiService() {
        this.aiService = AiServices.builder(InterviewAiService.class)
                .chatModel(chatModel)
                .systemMessageProvider(chatMemory -> currentPersona.systemPrompt())
                .build();
    }

    /**
     * 切换面试官人格
     * 
     * @param topic 面试主题，用于匹配最合适的人格
     * @return 是否成功切换
     */
    public boolean switchPersona(String topic) {
        Optional<InterviewerPersona> personaOpt = knowledgeBase.findBestPersona(topic);
        
        if (personaOpt.isPresent()) {
            InterviewerPersona newPersona = personaOpt.get();
            if (!newPersona.id().equals(currentPersona.id())) {
                this.currentPersona = newPersona;
                this.currentTopic = topic;
                rebuildAiService();
                
                // 清空历史对话，开始新的面试
                chatMemory.clear();
                
                System.out.println("🎭 已切换面试官人格：" + currentPersona.name());
                System.out.println("   专业领域：" + currentPersona.expertise());
                System.out.println("   面试风格：" + currentPersona.style());
                return true;
            }
        } else {
            System.out.println("⚠️ 未找到匹配 '" + topic + "' 的专用人格，使用当前人格");
        }
        
        return false;
    }

    /**
     * 获取当前面试官人格
     */
    public InterviewerPersona getCurrentPersona() {
        return currentPersona;
    }

    /**
     * 获取当前面试主题
     */
    public String getCurrentTopic() {
        return currentTopic;
    }

    /**
     * 生成下一题（RAG 增强）
     * 
     * @param topic 面试主题
     * @param difficulty 难度级别
     * @return 面试题
     */
    public InterviewQuestion nextQuestion(String topic, InterviewQuestion.Difficulty difficulty) {
        // 如果主题变化，尝试切换人格
        if (!topic.equals(currentTopic)) {
            switchPersona(topic);
        }
        
        // 1. 从 DuckDB 检索相关面经（Top 3）
        List<InterviewReference> refs = knowledgeBase.search(topic, null, 3);
        
        if (refs.isEmpty()) {
            throw new RuntimeException("未找到主题 '" + topic + "' 的相关面经资料，请先执行 init 命令初始化知识库");
        }

        // 2. 拼接上下文（公司信息 + 面经片段）
        String context = refs.stream()
                .map(r -> String.format("[%s, 相似度%.2f]: %s",
                        r.metadata().getString("file_name"), r.score(), r.content()))
                .collect(Collectors.joining("\n---\n"));

        // 3. AI 基于真实面经生成题目
        InterviewQuestion rawQuestion = aiService.generateQuestion(context);

        // 4. 补充元数据
        String source = refs.getFirst().metadata().getString("file_name");
        return new InterviewQuestion(
                UUID.randomUUID().toString(),
                rawQuestion.content(),
                rawQuestion.keyPoints(),
                difficulty != null ? difficulty : rawQuestion.difficulty(),
                source,
                topic,
                refs.getFirst().content()
        );
    }

    /**
     * 评估回答（对比向量库中的标准答案）
     * 
     * @param questionId 问题ID
     * @param userAnswer 用户回答
     * @param question 面试题
     * @return 评估结果
     */
    public EvaluationResult evaluate(String questionId, String userAnswer, InterviewQuestion question) {
        // 向量检索该题的标准答案要点
        String reference = question.suggestedAnswer();

        // AI 评估
        EvaluationResult result = aiService.evaluateAnswer(reference, userAnswer);

        // 保存到会话历史
        chatMemory.add(UserMessage.from("问题：" + question.content() + "\n回答：" + userAnswer));
        chatMemory.add(AiMessage.from(result.suggestion()));

        return result;
    }

    /**
     * 智能追问（基于评估结果）
     * 
     * @param result 评估结果
     * @param question 面试题
     * @return 追问内容
     */
    public String generateFollowUp(EvaluationResult result, InterviewQuestion question) {
        String previousQa = String.format("问题：%s\n回答评估：%s\n建议追问方向：%s",
                question.content(),
                result.suggestion(),
                result.followUp());
        
        return aiService.generateFollowUp(previousQa);
    }

    /**
     * 开始面试会话
     * 
     * @param topic 面试主题
     * @return 欢迎语和第一题
     */
    public String startInterview(String topic) {
        // 切换到合适的人格
        switchPersona(topic);
        
        // 构建欢迎语
        String welcome = String.format("""
            👋 欢迎开始模拟面试！
            
            🎭 当前面试官：%s
            📋 专业领域：%s
            💡 面试风格：%s
            
            我们将围绕 "%s" 进行面试。请准备好后回答第一道题目。
            """,
            currentPersona.name(),
            currentPersona.expertise(),
            currentPersona.style(),
            topic
        );
        
        return welcome;
    }

    /**
     * 获取知识库统计
     */
    public InterviewKnowledgeBase.KnowledgeStats getKnowledgeStats() {
        return knowledgeBase.getStats();
    }
    
    /**
     * 获取所有可用的面试官人格
     * 
     * @return 人格列表
     */
    public List<InterviewerPersona> getAllPersonas() {
        return knowledgeBase.getAllPersonas();
    }
    
    /**
     * 设置指定的面试官人格
     * 
     * @param persona 要设置的人格
     */
    public void setPersona(InterviewerPersona persona) {
        if (persona != null && !persona.id().equals(currentPersona.id())) {
            this.currentPersona = persona;
            this.currentTopic = persona.expertise() != null ? persona.expertise() : "通用技术";
            rebuildAiService();
            
            // 清空历史对话，开始新的面试
            chatMemory.clear();
            
            System.out.println("🎭 已切换面试官人格：" + currentPersona.name());
            System.out.println("   专业领域：" + currentPersona.expertise());
            System.out.println("   面试风格：" + currentPersona.style());
        }
    }
}
