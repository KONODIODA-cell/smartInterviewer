# SmartInterviewer 🎯

> 基于 AI 的模拟面试系统 - 使用 RAG 技术构建个性化面试官人格

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)](https://maven.apache.org/)
[![GraalVM](https://img.shields.io/badge/GraalVM-Native-green.svg)](https://www.graalvm.org/)

## 📖 简介

SmartInterviewer 是一个智能化的模拟面试练习系统，能够：

- 📄 **自动扫描** Markdown 面经文档，建立向量索引
- 🤖 **生成多个人格** 的 AI 面试官（基于你的面经内容）
- 💬 **交互式面试** 体验，支持自定义难度和题目数量
- 🎯 **RAG 增强** 题目生成和答案评估，确保专业性
- 📊 **智能评估** 回答质量，提供详细反馈和追问

## ✨ 功能特性

| 功能 | 描述 |
|------|------|
| **知识库初始化** | 扫描指定目录的 Markdown 面经，自动分块、向量化存储 |
| **人格化面试官** | 基于面经内容分析，生成具有不同专业领域和风格的面试官 |
| **RAG 题目生成** | 基于向量检索的真实面经生成面试题目 |
| **智能答案评估** | 多维度评分（准确性/完整性/深度）+ 详细反馈 |
| **深入追问** | 根据回答质量自动进行技术深度追问 |
| **离线向量库** | 使用 DuckDB 本地存储，无需额外服务 |

## 🛠️ 技术栈

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

## 📦 安装与构建

### 前置要求

- JDK 21+
- Maven 3.8+
- GraalVM（可选，用于构建原生镜像）

### 克隆项目

```bash
git clone <repository-url>
cd smartInterviewer
```

### 编译构建

```bash
# 标准编译
mvn clean package

# 构建 GraalVM 原生镜像（可选）
mvn -Pnative native:compile
```

## ⚙️ 配置

### 1. 环境变量配置

复制 `.env.example` 为 `.env` 并填写配置：

```bash
cp .env.example .env
```

编辑 `.env` 文件：

```ini
# ==================AI 服务商地址================
AI_SERVICE_URL=your_ai_service_path

# ==================API Key=====================
AI_API_KEY=your_api_key

# ==================模型名称====================
AI_MODEL_NAME=your_model_name

# ==================MD 文档目标路径===============
MD_PATH=/path/to/your/markdown/files

# ==================数据库路径==================
DB_PATH=/path/to/interviewer.db
```

### 配置说明

| 变量 | 说明 | 示例 |
|------|------|------|
| `AI_SERVICE_URL` | AI 服务 API 地址 | `https://api.openai.com/v1` |
| `AI_API_KEY` | API 认证密钥 | `sk-xxxxx` |
| `AI_MODEL_NAME` | 使用的模型名称 | `gpt-4`, `gpt-3.5-turbo` |
| `MD_PATH` | Markdown 面经文件目录 | `/home/user/interviews` |
| `DB_PATH` | DuckDB 数据库文件路径 | `/home/user/data/interviewer.db` |

## 🚀 使用指南

### 命令结构

```
ai-interviewer (版本：1.0.0)
│
├── init                      # 初始化知识库
│
└── start [选项]              # 开始模拟面试
    ├── -d, --difficulty      # 题目难度：BASIC/INTERMEDIATE/ADVANCED
    └── -c, --count           # 面试题数量
```

### 使用流程

#### 1️⃣ 初始化知识库

扫描 Markdown 文件并建立向量索引：

```bash
# 如果使用原生镜像
./ai-interviewer init

# 如果使用 JAR 运行
java -jar target/smartInterviewer-0.0.1.jar init
```

**执行过程：**
```
[1/4] 扫描 Markdown 文件...
[2/4] 解析并分块文档...
[3/4] 生成向量嵌入...
[4/4] 生成面试官人格...
✅ 知识库初始化完成！
```

#### 2️⃣ 开始模拟面试

```bash
# 默认配置（进阶难度，5 道题）
./ai-interviewer start

# 自定义难度和题目数量
./ai-interviewer start -d ADVANCED -c 10
```

**难度选项：**
| 难度 | 描述 |
|------|------|
| `BASIC` | 基础概念和简单问题 |
| `INTERMEDIATE` | 进阶技术和场景题（默认） |
| `ADVANCED` | 深度技术和系统设计题 |

### 面试交互示例

```
📋 可用的面试官人格：
1. [后端专家 - 张工] 专注分布式系统、高并发架构，风格严谨
2. [全栈工程师 - 李工] 关注前后端协同，风格温和
3. [算法专家 - 王工] 重视数据结构，喜欢追问复杂度
请选择面试官 (输入编号或名称): 1

🎯 正在生成面试主题...

【第 1 题】(难度：进阶)
请谈谈你对分布式事务的理解，以及常见的解决方案有哪些？

请输入你的回答（输入 END 提交）:
> 分布式事务是指在分布式系统中...
> ...
> END

📊 评估结果：
├─ 总分：78/100
├─ 准确性：85/100
├─ 完整性：70/100
├─ 深度：75/100
├─ 亮点：概念清晰，举例恰当
├─ 不足：缺少具体场景分析
└─ 追问：在跨服务调用中，如何保证最终一致性？
```

## 📁 项目结构

```
smartInterviewer/
├── src/main/java/org/hane/
│   ├── cli/                        # CLI 命令层
│   │   ├── InterViewerCli.java     # 主入口
│   │   ├── InitCommand.java        # init 命令
│   │   └── PracticeCommand.java    # start 命令
│   ├── model/                      # 数据模型层
│   │   ├── InterviewerPersona.java # 面试官人格
│   │   ├── InterviewQuestion.java  # 面试问题
│   │   ├── EvaluationResult.java   # 评估结果
│   │   └── InterviewReference.java # 知识库引用
│   ├── service/                    # 业务服务层
│   │   ├── initService/            # 初始化服务
│   │   │   ├── KnowledgeBaseService.java
│   │   │   ├── DocumentProcessor.java
│   │   │   ├── InterviewerPersonaGenerator.java
│   │   │   └── PersonaAiService.java
│   │   └── practiceService/        # 面试练习服务
│   │       ├── InterviewSessionService.java
│   │       ├── PersonaService.java
│   │       ├── InterviewAiService.java
│   │       └── InterviewKnowledgeBase.java
│   └── utils/                      # 工具类层
│       ├── AppConfig.java          # 应用配置
│       ├── DotenvLoader.java       # 环境变量加载
│       └── DuckDb.java             # DuckDB 工具
├── src/main/resources/
├── docs/                           # 文档目录
├── .env.example                    # 环境变量示例
├── pom.xml                         # Maven 配置
└── Readme.md
```

## 🏗️ 架构设计

### 核心流程

#### 知识库初始化流程
```
Markdown 文件扫描
    ↓
文档解析 (MarkdownParser)
    ↓
文本分块 (DocumentSplitters, 500 字符/块)
    ↓
向量化 (BgeSmallZhEmbeddingModel, 批量处理)
    ↓
存储 (DuckDB 向量库)
    ↓
人格生成 (AI 分析文件结构)
    ↓
保存人格 (interviewer_personas 表)
```

#### 模拟面试流程
```
选择面试官人格
    ↓
生成面试主题 (AI)
    ↓
循环：
  ├─ 检索相关面经 (向量检索)
  ├─ 生成题目 (RAG 模式)
  ├─ 用户回答
  ├─ 评估回答 (多维度评分)
  └─ 智能追问 (分数<9 时触发)
    ↓
面试总结
```

### DuckDB 表结构

```sql
-- 向量嵌入表
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

## ⚠️ 注意事项

1. **首次运行**：必须先执行 `init` 命令初始化知识库，否则无法开始面试
2. **Markdown 格式**：确保面经文件为标准 Markdown 格式，标题层级清晰
3. **API 配额**：人格生成和面试过程会消耗 AI Token，注意 API 配额限制
4. **数据库锁定**：避免同时运行多个实例，DuckDB 不支持并发写入
5. **Embedding 模型**：首次使用会下载 BGE 模型文件，请确保网络畅通

## ❓ FAQ

**Q: 如何更换 AI 服务商？**
A: 修改 `.env` 中的 `AI_SERVICE_URL` 为兼容 OpenAI 接口的服务商地址即可。

**Q: 支持哪些 Embedding 模型？**
A: 当前使用 `langchain4j-embeddings-bge-small-zh`，专为中文优化。可在 `pom.xml` 中更换。

**Q: 如何重置知识库？**
A: 删除 `DB_PATH` 指定的数据库文件，重新运行 `init` 命令。

**Q: 可以自定义面试官人格吗？**
A: 当前人格由 AI 自动生成。如需手动添加，可直接向 `interviewer_personas` 表插入数据。

**Q: 原生镜像构建失败怎么办？**
A: 确保安装了 GraalVM 21+，并设置 `GRAALVM_HOME` 环境变量。DuckDB 需要 JNI 支持，已配置在 `pom.xml` 中。

## 📄 许可证

本项目采用 MIT 许可证。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

**Happy Interviewing! 🎉**
