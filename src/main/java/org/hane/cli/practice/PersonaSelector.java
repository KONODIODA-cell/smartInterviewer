package org.hane.cli.practice;

import org.hane.model.InterviewerPersona;
import org.hane.service.practiceService.PersonaService;

import java.util.List;

/**
 * 面试官人格选择器
 * 负责展示和选择 AI 面试官人格
 */
public class PersonaSelector {
    
    private final PersonaService personaService;
    
    public PersonaSelector() {
        this.personaService = new PersonaService();
    }
    
    /**
     * 交互式选择面试官人格
     * 
     * @param inputHandler 用户输入处理器
     * @return 选定的人格，如果用户取消则返回 null
     * @throws Exception 查询失败时抛出异常
     */
    public InterviewerPersona selectPersona(UserInputHandler inputHandler) throws Exception {
        List<InterviewerPersona> personas = personaService.getAllPersonas();
        
        if (personas.isEmpty()) {
            throw new RuntimeException("personas is empty");
        }
        
        System.out.println("\n🎭 请选择 AI 面试官人格：\n");
        
        // 显示所有可用人格
        displayPersonas(personas);
        
        // 获取用户选择
        int choice = inputHandler.getNumericChoice(personas.size());
        
        if (choice == 0) {
            return null;
        }
        
        return personas.get(choice - 1);
    }
    
    /**
     * 显示所有可用人格列表
     */
    private void displayPersonas(List<InterviewerPersona> personas) {
        for (int i = 0; i < personas.size(); i++) {
            InterviewerPersona persona = personas.get(i);
            System.out.printf("  [%d] %s\n", i + 1, persona.getName());
            System.out.printf("      专业领域：%s\n", 
                persona.getExpertise() != null ? persona.getExpertise() : "通用");
            System.out.printf("      面试风格：%s\n", 
                persona.getStyle() != null ? persona.getStyle() : "标准");
            if (persona.getDescription() != null && !persona.getDescription().isEmpty()) {
                System.out.printf("      简介：%s\n", persona.getDescription());
            }
            System.out.println();
        }
    }
}
