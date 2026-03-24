package org.hane.service.practiceService;

import org.hane.model.InterviewerPersona;
import org.hane.utils.DuckDb;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PersonaService {
  /**
   * 获取所有可用的面试官人格
   *
   * @return 人格列表
   */
  public List<InterviewerPersona> getAllPersonas() throws Exception {
    List<InterviewerPersona> personas = new ArrayList<>();

    // 从 interviewer_personas 表查询所有人格
    String sql = """
        SELECT id, name, description, system_prompt, file_patterns, expertise, style, difficulty_bias, priority
        FROM interviewer_personas
        ORDER BY priority DESC
        """;

    try (Connection conn = DuckDb.getReadOnlyConn();
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
      e.printStackTrace();
      throw new Exception("find all fail");
    }

    return personas;
  }
}
