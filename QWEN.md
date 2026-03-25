# SmartInterviewer 项目上下文

## 项目概述

**SmartInterviewer** 是一个基于 AI 的模拟面试系统，使用 RAG（检索增强生成）技术构建个性化面试官人格。项目采用 Java 21 开发，通过 CLI 命令行界面提供交互式面试体验。

### 核心功能

- **知识库初始化**：扫描 Markdown 面经文档，自动分块、向量化存储到 DuckDB
- **人格化面试官**：基于面经内容分析，生成具有不同专业领域和风格的面试官
- **RAG 题目生成**：基于向量检索的真实面经生成面试题目
- **智能答案评估**：多维度评分（准确性/完整性/深度）+ 详细反馈
- **深入追问**：根据回答质量自动进行技术深度追问
- **离线向量库**：使用 DuckDB 本地存储，无需额外服务

### 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| **语言** | Java | 21 |
| **CLI 框架** | Picocli | 4.7.6 |
| **AI 框架** | LangChain4j | 1.12.2 |
| **向量数据库** | DuckDB | - |
| **Embedding 模型** | BGE Small Zh | - |
| **LLM** | OpenAI 兼容接口 | 可配置 |
| **构建工具** | Maven | - |
| **原生编译** | GraalVM Native Image | - |

---

## 项目结构

```
smartInterviewer/
├── src/main/java/org/hane/
│   ├── cli/                        # CLI 命令层
│   │   ├── InterViewerCli.java     # 主入口（Picocli）
│   │   ├── InitCommand.java        # init 命令 - 初始化知识库
│   │   └── PracticeCommand.java    # start 命令 - 开始模拟面试
│   ├── model/                      # 数据模型层
│   │   ├── InterviewerPersona.java # 面试官人格模型
│   │   ├── InterviewQuestion.java  # 面试问题模型
│   │   ├── EvaluationResult.java   # 评估结果模型
│   │   └── InterviewReference.java # 知识库引用模型
│   ├── service/                    # 业务服务层
│   │   ├── initService/            # 初始化服务
│   │   │   ├── KnowledgeBaseService.java    # 知识库初始化主服务
│   │   │   ├── DocumentProcessor.java       # 文档扫描与处理
│   │   │   ├── InterviewerPersonaGenerator.java  # 人格生成器
│   │   │   ├── PersonaAiService.java        # 人格 AI 服务
│   │   │   └── PersonaListWrapper.java      # 人格列表包装类
│   │   └── practiceService/        # 面试练习服务
│   │       ├── InterviewSessionService.java   # 面试会话管理
│   │       ├── PersonaService.java            # 人格选择服务
│   │       ├── InterviewAiService.java        # 面试 AI 服务
│   │       └── InterviewKnowledgeBase.java    # 面试知识库检索
│   └── utils/                      # 工具类层
│       ├── AppConfig.java          # 应用配置（单例）
│       ├── DotenvLoader.java       # .env 环境变量加载器
│       └── DuckDb.java             # DuckDB 连接工具
├── src/test/java/org/hane/
│   └── DebugDb.java                # 数据库调试工具
├── src/main/resources/
├── docs/                           # 文档目录
├── bin/docker/                     # Docker 相关配置
├── .env.example                    # 环境变量示例
├── pom.xml                         # Maven 配置
└── Readme.md                       # 项目文档
```

---

## 构建与运行

### 前置要求

- JDK 21+
- Maven 3.8+
- GraalVM（可选，用于构建原生镜像）

### 编译构建

```bash
# 标准编译打包
mvn clean package

# 构建 GraalVM 原生镜像
mvn -Pnative native:compile
```

### 运行方式

```bash
# 方式 1：直接运行 JAR
java -jar target/smartInterviewer-0.0.1.jar <command>

# 方式 2：使用 Maven
mvn exec:java -Dexec.mainClass="org.hane.cli.InterViewerCli" -Dexec.args="<command>"

# 方式 3：原生镜像（构建后）
./ai-interviewer <command>
```

### 可用命令

```
ai-interviewer (版本：1.0.0)
│
├── init                      # 初始化知识库
│
└── start [选项]              # 开始模拟面试
    ├── -d, --difficulty      # 题目难度：BASIC/INTERMEDIATE/ADVANCED
    └── -c, --count           # 面试题数量
```

---

## 配置说明

### 环境变量配置

复制 `.env.example` 为 `.env` 并填写配置：

```bash
cp .env.example .env
```

### 必需的环境变量

| 变量 | 说明 | 示例 |
|------|------|------|
| `AI_SERVICE_URL` | AI 服务 API 地址 | `https://api.openai.com/v1` |
| `AI_API_KEY` | API 认证密钥 | `sk-xxxxx` |
| `AI_MODEL_NAME` | 使用的模型名称 | `gpt-4`, `gpt-3.5-turbo` |
| `MD_PATH` | Markdown 面经文件目录 | `/home/user/interviews` |
| `DB_PATH` | DuckDB 数据库文件路径 | `/home/user/data/interviewer.db` |

### 配置加载逻辑

配置优先级（`AppConfig.java`）：
1. 系统环境变量（最高优先级）
2. `.env` 文件

---

## 核心架构

### 知识库初始化流程

```
Markdown 文件扫描 (DocumentProcessor.scanMarkdownFiles)
    ↓
文档解析 (MarkdownParser)
    ↓
文本分块 (DocumentSplitters, 500 字符/块)
    ↓
向量化 (BgeSmallZhEmbeddingModel, 批量处理)
    ↓
存储 (DuckDB EmbeddingStore)
    ↓
人格生成 (AI 分析文件结构)
    ↓
保存人格 (interviewer_personas 表)
```

### 模拟面试流程

```
选择面试官人格 (PersonaService)
    ↓
生成面试主题 (InterviewAiService)
    ↓
循环：
  ├─ 检索相关面经 (InterviewKnowledgeBase - 向量检索)
  ├─ 生成题目 (RAG 模式)
  ├─ 用户回答
  ├─ 评估回答 (多维度评分)
  └─ 智能追问 (分数<9 时触发)
    ↓
面试总结
```

### DuckDB 表结构

```sql
-- 向量嵌入表 (RAG 知识库)
CREATE TABLE embeddings (
    embedding DOUBLE[],
    text TEXT,
    metadata JSON
);

-- 面试官人格表
CREATE TABLE interviewer_personas (
    id VARCHAR PRIMARY KEY,
    name VARCHAR NOT NULL,
    description VARCHAR,
    system_prompt TEXT,
    expertise VARCHAR,
    style VARCHAR,
    difficulty_bias VARCHAR,
    priority INT
);
```

---

## 开发约定

### 代码风格

- **命名规范**：Java 驼峰命名法，类名大驼峰，方法/变量小驼峰
- **注解使用**：Lombok 简化样板代码（`@ToString`, `@Getter` 等）
- **异常处理**：CLI 层统一捕获并格式化错误信息
- **配置访问**：通过 `AppConfig` 静态字段访问环境变量

### 分层架构

```
CLI 层 (cli/)
    ↓
服务层 (service/)
    ↓
模型层 (model/)
    ↓
工具层 (utils/)
```

### 测试实践

- 测试代码位于 `src/test/java/org/hane/`
- 当前包含 `DebugDb.java` 用于调试数据库内容

---

## 关键类说明

### CLI 入口

| 类 | 职责 |
|------|------|
| `InterViewerCli` | 主入口，配置 Picocli 命令行，注册子命令 |
| `InitCommand` | 执行知识库初始化 |
| `PracticeCommand` | 执行模拟面试会话 |

### 核心服务

| 类 | 职责 |
|------|------|
| `KnowledgeBaseService` | 协调文档处理和人格生成 |
| `DocumentProcessor` | 扫描、解析 Markdown 文件，构建向量索引 |
| `InterviewerPersonaGenerator` | 基于文件结构生成面试官人格 |
| `InterviewSessionService` | 管理面试会话流程 |
| `PersonaService` | 提供人格选择和列表功能 |
| `InterviewAiService` | 与 AI 交互生成题目和评估答案 |
| `InterviewKnowledgeBase` | 向量检索相关面经内容 |

### 工具类

| 类 | 职责 |
|------|------|
| `AppConfig` | 应用配置单例，负责加载和验证环境变量 |
| `DotenvLoader` | 解析 `.env` 文件 |
| `DuckDb` | 提供 DuckDB 连接工具方法 |

---

## 注意事项

1. **首次运行**：必须先执行 `init` 命令初始化知识库，否则无法开始面试
2. **Markdown 格式**：确保面经文件为标准 Markdown 格式，标题层级清晰
3. **API 配额**：人格生成和面试过程会消耗 AI Token，注意 API 配额限制
4. **数据库锁定**：避免同时运行多个实例，DuckDB 不支持并发写入
5. **Embedding 模型**：首次使用会下载 BGE 模型文件，请确保网络畅通

---

## 常用操作

### 调试数据库

```bash
# 设置环境变量后运行调试工具
export DB_PATH=/path/to/interviewer.db
java -cp target/smartInterviewer-0.0.1.jar org.hane.DebugDb
```

### 重置知识库

```bash
# 删除数据库文件
rm $DB_PATH

# 重新初始化
java -jar target/smartInterviewer-0.0.1.jar init
```

### 查看可用命令

```bash
java -jar target/smartInterviewer-0.0.1.jar --help
java -jar target/smartInterviewer-0.0.1.jar start --help
```
