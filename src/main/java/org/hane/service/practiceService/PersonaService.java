package org.hane.service.practiceService;

import org.hane.model.InterviewerPersona;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import static org.hane.utils.AppConfig.dbPath;

public class PersonaService {
  static {
    // 赋予面试官一个默认的人格
    var persona = new InterviewerPersona();
    persona.setId("default-general-interviewer");
    persona.setName("通用技术面试官");
    persona.setDescription("一位专业、友善的技术面试官，注重考察候选人的基础知识和解决问题的能力");
    persona.setSystemPrompt("""
      你是一位经验丰富的技术面试官，正在进行一场模拟面试。
      
      你的特点：
      - 专业且友善，营造轻松的面试氛围
      - 注重考察基础知识的理解深度，而非死记硬背
      - 当候选人回答不完整时，给予适当的提示和引导
      - 追问时由浅入深，帮助候选人展现真实水平
      
      面试行为准则：
      1. 每次只问一个问题，避免连环追问
      2. 问题要具体、开放，避免"是/否"能回答的
      3. 对候选人的回答给予及时、具体的反馈
      4. 如遇知识盲区，耐心解释并记录
      5. 保持鼓励态度，即使回答不佳也不打击信心
      
      语言风格：
      - 简洁清晰，避免冗长
      - 使用标准技术术语
      - 适当使用例子帮助理解
      """);
    persona.setFilePatterns("通用，Java，后端，基础");
    persona.setExpertise("Java 后端开发");
    persona.setStyle("亲和引导型");
    persona.setDifficultyBias("INTERMEDIATE");
    persona.setPriority(1);
    currentPersona = persona;
  }

  private static InterviewerPersona currentPersona;

  public static InterviewerPersona getCurrentPersona() {
    return currentPersona;
  }

  public static void setCurrentPersona(InterviewerPersona currentPersona) {
    PersonaService.currentPersona = currentPersona;
  }

  /**
   * 获取所有可用的面试官人格
   *
   * @return 人格列表
   */
  public List<InterviewerPersona> getAllPersonas() {
    List<InterviewerPersona> personas = new ArrayList<>();

    // 从 interviewer_personas 表查询所有人格
    String sql = """
        SELECT id, name, description, system_prompt, file_patterns, expertise, style, difficulty_bias, priority
        FROM interviewer_personas
        ORDER BY priority DESC
        """;

    try (Connection conn = DriverManager.getConnection("jdbc:duckdb:" + dbPath.toString());
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

      while (rs.next()) {
        InterviewerPersona persona = new InterviewerPersona();
        persona.setId(rs.getString("id"));
        persona.setName(rs.getString("name"));
        persona.setDescription(rs.getString("description"));
        persona.setSystemPrompt(rs.getString("system_prompt"));
        persona.setFilePatterns(rs.getString("file_patterns"));
        persona.setExpertise(rs.getString("expertise"));
        persona.setStyle(rs.getString("style"));
        persona.setDifficultyBias(rs.getString("difficulty_bias"));
        persona.setPriority(rs.getInt("priority"));
        personas.add(persona);
      }
    } catch (SQLException e) {
      System.err.println("查询人格数据失败：" + e.getMessage());
    }

    return personas;
  }
}
