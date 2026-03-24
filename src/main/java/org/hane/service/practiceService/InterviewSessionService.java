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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hane.model.InterviewQuestion.*;

/**
 * 面试会话服务
 * 负责管理面试流程、AI 交互、人格切换等
 */
public class InterviewSessionService {
  private static Difficulty queestionDifficulty;
  private final InterviewKnowledgeBase knowledgeBase;
  private final ChatMemory chatMemory;

  // 所有的主题
  private static List<String> topics;

  // ai
  private static ChatModel model;
  private static InterviewAiService aiServices;

  public InterviewSessionService(InterviewerPersona persona, int count, Difficulty difficulty) {
    // 初始化模型
    model = OpenAiChatModel.builder().
        modelName(AppConfig.aiModelName).
        baseUrl(AppConfig.aiServiceUrl).
        apiKey(AppConfig.aiApiKey).
        temperature(0.6).
        build();

    // 初始化知识库
    this.knowledgeBase = new InterviewKnowledgeBase(Path.of(AppConfig.dbPath));

    // 初始化对话历史
    this.chatMemory = MessageWindowChatMemory.withMaxMessages(20);
    queestionDifficulty = difficulty;

    // 生成对应的题目主题
    aiServices = AiServices.create(InterviewAiService.class, model);
    topics = aiServices.genTopics(persona.getName(),
        persona.getExpertise(),
        persona.getStyle(),
        persona.getDifficultyBias(),
        persona.getDescription(),
        count + 20);
  }


  /**
   * 生成下一题（RAG 增强）
   *
   * @return 面试题
   */
  public InterviewQuestion nextQuestion() {
    if (topics.isEmpty()) {
      throw new RuntimeException("topic is empty");
    }

    String topic = "";
    List<InterviewReference> refs = List.of();
    for (int i = 0; i < 3; i++) {
      // 随机选取一个聊天主题
      int index = (int) (Math.random() * topics.size());
      topic = topics.remove(index);
      System.out.println("🔍 尝试搜索主题 [" + (i + 1) + "/3]: " + topic);
      refs = knowledgeBase.search(topic, 3);
      System.out.println("   找到 " + refs.size() + " 条结果");
      if (!refs.isEmpty()) {
        break;
      }
    }

    if (refs.isEmpty()) {
      System.err.println("剩余 topics: " + topics);
      throw new RuntimeException("重试三次仍未找到相关主题");
    }

    // 拼接上下文
    String context = refs.stream()
        .map(r -> String.format("[%s, 相似度%.2f]: %s",
            r.metadata().getString("file_name"), r.score(), r.content()))
        .collect(Collectors.joining("\n---\n"));

    // AI 基于真实面经生成题目
    InterviewQuestion rawQuestion = aiServices.generateQuestion(context);

    // 补充元数据
    String source = refs.getFirst().metadata().getString("file_name");
    return new InterviewQuestion(
        UUID.randomUUID().toString(),
        rawQuestion.content(),
        rawQuestion.keyPoints(),
        queestionDifficulty,
        source,
        topic,
        refs.getFirst().content()
    );
  }

  /**
   * 评估回答（对比向量库中的标准答案）
   *
   * @param userAnswer 用户回答
   * @param question   面试题
   * @return 评估结果
   */
  public EvaluationResult evaluate(String userAnswer, InterviewQuestion question) {
    // 向量检索该题的标准答案要点
    String reference = question.suggestedAnswer();

    // AI 评估
    EvaluationResult result = aiServices.evaluateAnswer(reference, userAnswer);

    // 保存到会话历史
    chatMemory.add(UserMessage.from("问题：" + question.content() + "\n回答：" + userAnswer));
    chatMemory.add(AiMessage.from(result.suggestion()));

    return result;
  }

  /**
   * 智能追问（基于评估结果）
   *
   * @param result   评估结果
   * @param question 面试题
   * @return 追问内容
   */
  public String generateFollowUp(EvaluationResult result, InterviewQuestion question) {
    // 如果评估结果已经包含追问建议，直接使用
    if (result.followUp() != null && !result.followUp().trim().isEmpty()) {
      return result.followUp();
    }

    // 否则基于评估结果生成更深入的追问
    String previousQa = String.format("""
        问题：%s
        考察点：%s
        用户回答评估：%s
        亮点：%s
        不足：%s
        遗漏点：%s
        改进建议：%s
        """,
        question.content(),
        String.join("、", question.keyPoints()),
        result.suggestion(),
        result.strengths().isEmpty() ? "无明显亮点" : String.join("、", result.strengths()),
        result.weaknesses().isEmpty() ? "无明显不足" : String.join("、", result.weaknesses()),
        result.missingPoints().isEmpty() ? "无遗漏点" : String.join("、", result.missingPoints()),
        result.suggestion());

    return aiServices.generateFollowUp(previousQa);
  }
}
