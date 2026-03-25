package org.hane.cli.practice;

import java.util.Scanner;

/**
 * 用户输入处理器
 * 负责处理所有与用户交互的输入操作
 */
public class UserInputHandler {
    
    private final Scanner scanner;
    
    public UserInputHandler() {
        this.scanner = new Scanner(System.in);
    }
    
    /**
     * 等待用户按 Enter 键继续
     * @param message 提示信息
     */
    public void waitForEnter(String message) {
        System.out.println(message);
        scanner.nextLine();
    }
    
    /**
     * 获取用户的回答（多行输入，空行结束）
     * @return 用户回答内容
     */
    public String getUserAnswer() {
        System.out.println("\n💬 请输入你的回答（输入空行结束）：");
        StringBuilder answerBuilder = new StringBuilder();
        String line;
        while (!(line = scanner.nextLine()).isEmpty()) {
            answerBuilder.append(line).append("\n");
        }
        return answerBuilder.toString().trim();
    }
    
    /**
     * 获取用户对追问的回答
     * @return 追问回答内容
     */
    public String getFollowUpAnswer() {
        StringBuilder followUpBuilder = new StringBuilder();
        String line;
        while (!(line = scanner.nextLine()).isEmpty()) {
            followUpBuilder.append(line).append("\n");
        }
        return followUpBuilder.toString().trim();
    }
    
    /**
     * 获取用户选择的序号
     * @param maxChoice 最大可选值
     * @return 用户选择的序号（0 表示取消）
     */
    public int getNumericChoice(int maxChoice) {
        while (true) {
            System.out.print("请输入序号选择面试官 (1-" + maxChoice + ", 输入 0 取消): ");
            String input = scanner.nextLine().trim();
            
            try {
                int choice = Integer.parseInt(input);
                
                if (choice == 0) {
                    return 0;
                }
                
                if (choice >= 1 && choice <= maxChoice) {
                    return choice;
                }
                
                System.out.println("⚠️ 无效的选择，请输入 1-" + maxChoice + " 之间的数字。");
                
            } catch (NumberFormatException e) {
                System.out.println("⚠️ 请输入有效的数字。");
            }
        }
    }
    
    /**
     * 关闭扫描器
     */
    public void close() {
        scanner.close();
    }
}
