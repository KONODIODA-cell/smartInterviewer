package org.hane.service.initService;

import org.hane.model.InterviewerPersona;

import java.util.List;

/**
 * AI 人格列表包装类
 * 用于解决 LangChain4j 无法直接解析 List<InterviewerPersona> 的问题
 */
public class PersonaListWrapper {
    
    private List<InterviewerPersona> personas;
    
    public PersonaListWrapper() {
    }
    
    public List<InterviewerPersona> getPersonas() {
        return personas;
    }
    
    public void setPersonas(List<InterviewerPersona> personas) {
        this.personas = personas;
    }
}
