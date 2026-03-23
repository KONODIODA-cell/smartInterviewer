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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hane.model.InterviewQuestion.*;

/**
 * 面试会话服务
 * 负责管理面试流程、AI 交互、人格切换等
 */
public class InterviewSessionService {
  private static int questionCount;
  private static Difficulty queestionDifficulty;
  private static InterviewerPersona currentPersona;
  private InterviewAiService aiService;
  private final InterviewKnowledgeBase knowledgeBase;
  private final ChatMemory chatMemory;


  public InterviewSessionService(InterviewerPersona persona, int count, Difficulty difficulty) {
    // 初始化 ChatModel（所有 AI 服务共用）
    var chatModel = OpenAiChatModel.builder()
        .baseUrl(AppConfig.aiServiceUrl)
        .apiKey(AppConfig.aiApiKey)
        .modelName(AppConfig.aiModelName)
        .temperature(1.0)
        .build();

    // 初始化知识库
    this.knowledgeBase = new InterviewKnowledgeBase(Path.of(AppConfig.dbPath));

    // 初始化对话历史
    this.chatMemory = MessageWindowChatMemory.withMaxMessages(20);

    currentPersona = persona;
    questionCount = count + 20;
    queestionDifficulty = difficulty;
  }


  /**
   * 生成下一题（RAG 增强）
   *
   * @return 面试题
   */
  public InterviewQuestion nextQuestion() {
    // 1. 从 DuckDB 检索相关面经（Top 3）
    List<InterviewReference> refs = knowledgeBase.search(topic, 3);

    if (refs.isEmpty()) {
      throw new RuntimeException("未找到主题 '" + topic + "' 的相关面经资料，请先执行 init 命令初始化知识库");
    }

    // 2. 拼接上下文
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
        queestionDifficulty,
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
   * @param question   面试题
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
   * @param result   评估结果
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
}
