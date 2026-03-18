package org.hane.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DotenvLoader {
    
    private final Map<String, String> env = new HashMap<>();
    private final Path envPath;
    
    public DotenvLoader(Path workDir) {
        this.envPath = findEnvFile(workDir);
        if (envPath != null) {
            load();
        }
    }

    // 读取当前目录下的.env文件
    private Path findEnvFile(Path workDir) {
        Path local = workDir.resolve(".env");
        if (Files.exists(local)) return local;
        return null;
    }
    
    private void load() {
        try {
            List<String> lines = Files.readAllLines(envPath, StandardCharsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                
                int eq = line.indexOf('=');
                if (eq > 0) {
                    String key = line.substring(0, eq).trim();
                    String value = line.substring(eq + 1).trim();
                    // 去除可能的引号
                    value = value.replaceAll("^[\"']|[\"']$", "");
                    env.put(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("⚠️ 无法读取 .env: " + e.getMessage());
        }
    }
    
    public String get(String key) {
        return env.get(key);
    }
    
    public String getOrDefault(String key, String defaultValue) {
        return env.getOrDefault(key, defaultValue);
    }
}