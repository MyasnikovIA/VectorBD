На основе анализа проекта я создал comprehensive README.md файл:

```markdown
# VectorDB - Векторная база данных с семантическим поиском и RAG

Java-библиотека для работы с векторными базами данных, поддерживающая семантический поиск, чанкинг текста и интеграцию с Ollama для RAG (Retrieval-Augmented Generation) приложений.

# Документация с примерами использования
- [DOCUMENT.md](DOCUMENT.md)

# Пример
- [EXAMPLE.md](EXAMPLE.md)

## 🚀 Основные возможности

- **Векторное хранение данных** - эффективное хранение и поиск по векторным эмбеддингам
- **Семантический чанкинг** - интеллектуальное разбиение текста на смысловые фрагменты
- **Древовидная структура данных** - гибкое хранение иерархических данных
- **Интеграция с Ollama** - работа с локальными LLM моделями
- **RAG-функциональность** - генерация ответов с использованием контекста из базы знаний
- **Поиск по схожести** - косинусная схожесть для семантического поиска

## 📁 Структура проекта

### Основные модули

#### Core (`ru.miacomsoft.vectordb.core`)
- `VectorDatabase` - основная база данных векторов
- `SemanticChunker` - семантическое разбиение текста на чанки
- `TreeNode` - древовидная структура для хранения данных
- `VectorData` - модель данных для векторных записей
- `VectorIndex` - индекс для быстрого векторного поиска

#### Knowledge (`ru.miacomsoft.vectordb.knowledge`)
- `KnowledgeLoader` - загрузчик текстовых знаний в базу
- `OllamaKnowledgeClient` - клиент для работы с Ollama
- `PromptGenerator` - генератор промптов для AI
- `KnowledgeConfig` - конфигурация системы знаний

#### Demo (`ru.miacomsoft.vectordb.demo`)
- Примеры использования всех компонентов системы

## 🛠️ Быстрый старт

### Предварительные требования

1. **Java 11+**
2. **Ollama** (для работы с LLM)
3. **Maven/Gradle** для сборки

### Установка Ollama

```bash
# Установка Ollama (Linux/Mac)
curl -fsSL https://ollama.ai/install.sh | sh

# Загрузка модели
ollama pull llama3.2
ollama pull all-minilm:22m
```

### Базовая настройка

```java
// Инициализация SemanticChunker
SemanticChunker semanticChunker = new SemanticChunker(
    "http://localhost:11434",
    "all-minilm:22m",
    0.8  // порог схожести
);

// Создание векторной базы данных
VectorDatabase vectorDB = new VectorDatabase("./data/vectordb", semanticChunker);

// Загрузка текста с семантическим чанкингом
vectorDB.storeTextWithChunking(
    "Ваш текст для индексации...",
    "doc_001", 
    new Object[]{"documents", "category"}
);

// Семантический поиск
List<VectorSearchResult> results = vectorDB.similaritySearch(
    "поисковый запрос", 5
);
```

## 📚 Примеры использования

### 1. Семантический поиск

```java
// Поиск похожих документов
List<VectorSearchResult> results = vectorDB.similaritySearch(
    "машинное обучение", 3
);

for (VectorSearchResult result : results) {
    System.out.printf("Схожесть: %.4f - %s%n", 
        result.getSimilarity(), 
        result.getVectorData().getText()
    );
}
```

### 2. Загрузка знаний

```java
KnowledgeConfig config = new KnowledgeConfig(
    "http://localhost:11434",
    "llama3.2",
    0.8,
    true,
    true
);

KnowledgeLoader loader = new KnowledgeLoader(vectorDB, config);

// Загрузка текста
int chunks = loader.loadText(
    "Текст с знаниями...",
    "knowledge_base",
    new Object[]{"knowledge", "ai"},
    500,
    "source_name"
);
```

### 3. Генерация ответов с RAG

```java
OllamaKnowledgeClient client = new OllamaKnowledgeClient(vectorDB, config);

// Генерация ответа с использованием знаний
String response = client.generateResponseWithKnowledge(
    "Что такое машинное обучение?"
);

// Интерактивный чат
client.startInteractiveChat();
```

### 4. Работа с древовидными структурами

```java
TreeNode root = new TreeNode();

// Установка значений по пути
root.setNode(new Object[]{"users", "user1", "name"}, "Иван Иванов");
root.setNode(new Object[]{"users", "user1", "age"}, 30);

// Получение значений
String name = (String) root.getNode(new Object[]{"users", "user1", "name"});

// Поиск по дереву
List<TreeNode.QueryResult> results = root.query(
    new Object[]{"users"}, 
    2  // глубина поиска
);
```

## 🔧 Конфигурация

### Настройка SemanticChunker

```java
SemanticChunker chunker = new SemanticChunker(
    "http://localhost:11434",  // URL Ollama
    "all-minilm:22m",          // модель для эмбеддингов
    0.8                        // порог схожести (0.0-1.0)
);

// Настройка порога
chunker.setSimilarityThreshold(0.7);
```

### Конфигурация Knowledge системы

```java
KnowledgeConfig config = new KnowledgeConfig(
    "http://localhost:11434",  // Ollama URL
    "llama3.2",               // модель для генерации
    0.8,                      // порог схожести
    true,                     // сохранять историю
    true                      // включить функциональность
);
```

## 🎯 Демонстрационные примеры

Проект включает comprehensive демо-классы:

- `VectorDBExample` - базовое использование векторной БД
- `EmbeddingDemo` - работа с эмбеддингами
- `KnowledgeDemo` - загрузка и использование знаний
- `OllamaChatDemo` - интерактивный чат с RAG
- `PromptGeneratorDemo` - генерация промптов
- `AdvancedPromptDemo` - продвинутые сценарии

Запуск демо:
```bash
java -cp vectordb.jar ru.miacomsoft.vectordb.demo.VectorDBExample
```

## 📊 Метрики и производительность

- **Косинусная схожесть** для семантического поиска
- **Автоматическое чанкинг** с настраиваемым порогом
- **Эффективное индексирование** для быстрого поиска
- **Поддержка больших объемов** данных через древовидные структуры

## 🔮 Расширенные возможности

### Кастомные стратегии чанкинга

```java
// Настройка семантического чанкинга
chunker.setSimilarityThreshold(0.6);  // более агрессивное разбиение
List<Chunk> chunks = chunker.semanticChunking(text, 1000);
```

### Многомодельная работа

```java
// Разные модели для эмбеддингов и генерации
SemanticChunker chunker = new SemanticChunker(
    "http://localhost:11434", 
    "all-minilm:22m",  // для эмбеддингов
    0.8
);

KnowledgeConfig config = new KnowledgeConfig(
    "http://localhost:11434",
    "llama3.2",        // для генерации
    0.8, true, true
);
```

## 🐛 Устранение неполадок

### Общие проблемы

1. **Ollama недоступен**
   ```bash
   # Проверка статуса
   curl http://localhost:11434/api/tags
   ```

2. **Модели не загружены**
   ```bash
   # Загрузка необходимых моделей
   ollama pull llama3.2
   ollama pull all-minilm:22m
   ```

3. **Проблемы с памятью**
    - Увеличьте heap size: `-Xmx4G`
    - Настройте размер чанков

### Логирование и отладка

```java
// Включение debug информации
System.out.println("Database stats: " + vectorDB.getVectorCount());
System.out.println("Chunker config: " + chunker.getConfigInfo());
```

## 📄 Лицензия

[Указать лицензию проекта]

## 🤝 Вклад в проект

Приветствуются issues и pull requests!

## 📞 Поддержка

Для вопросов и предложений:
- Создайте issue в репозитории
- Опишите проблему с примерами кода и логами

---

**Примечание**: Для полной функциональности требуется запущенный Ollama сервер с установленными моделями.
На основе анализа нового функционала, я дополню README.md следующими разделами:

## 🔍 Дополнения к README.md

Добавьте следующие разделы в существующий README.md файл:

### 🌳 Новые структуры данных

#### BinaryTreeNode
```java
// Бинарное дерево для специализированных операций
BinaryTreeNode binaryTree = new BinaryTreeNode();
// Используется для оптимизированного хранения и поиска в иерархических структурах
```

### 🧩 Расширенный Semantic Chunking

#### Улучшенный семантический чанкинг
```java
SemanticChunker chunker = new SemanticChunker(
    "http://localhost:11434",
    "all-minilm:22m", 
    0.8
);

// Расширенное управление чанкингом
chunker.setSimilarityThreshold(0.7); // Динамическое изменение порога

// Детальная информация о конфигурации
String configInfo = chunker.getConfigInfo();
System.out.println(configInfo);
// Output: SemanticChunker Config: model=all-minilm:22m, similarityThreshold=0.70, url=http://localhost:11434

// Получение информации о модели
String embeddingModel = chunker.getEmbeddingModel();
String ollamaUrl = chunker.getOllamaBaseUrl();
```

#### Работа с чанками
```java
List<SemanticChunker.Chunk> chunks = chunker.semanticChunking(text, 1000);

for (SemanticChunker.Chunk chunk : chunks) {
    System.out.println(chunk.getFullInfo());
    // Output: 
    // === CHUNK INFO ===
    // Position: 0
    // Length: 156 characters
    // Text:
    // Текст чанка...
    
    System.out.println("Position: " + chunk.getPosition());
    System.out.println("Length: " + chunk.getLength());
    System.out.println("Text: " + chunk.getText());
    float[] embedding = chunk.getEmbedding();
}
```

### 🗂️ Усовершенствованная работа с TreeNode

#### Расширенные возможности TreeNode
```java
TreeNode root = new TreeNode();

// Сериализация в JSON
String jsonString = root.toJsonString();
JSONObject json = root.toJson();

// Восстановление из JSON
TreeNode fromJson = TreeNode.fromJsonString(jsonString);
TreeNode fromJsonObject = TreeNode.fromJson(json);

// Работа с метаданными
root.setMetadata("author", "John Doe");
root.setMetadata("version", "1.0");
Object author = root.getMetadata("author");
Map<String, Object> allMetadata = root.getMetadata();

// Поиск путей к значениям
List<Object> path = root.getPathToNode("targetValue");
boolean contains = root.containsValue("searchValue");

// Глубина и ширина дерева
int depth = root.getDepth();
int width = root.getWidth();

// Получение листовых значений
List<Object> leafValues = root.getLeafValues();

// Поиск поддерева
TreeNode subtree = root.findSubtree(new Object[]{"users", "user1"});

// Слияние деревьев
TreeNode otherTree = new TreeNode();
root.mergeWith(otherTree);

// Визуальное представление
String treeString = root.toTreeString();
System.out.println(treeString);
// Output:
// └── rootData
//     ├── child1
//     │   └── grandchild1
//     └── child2

// Создание из различных источников
Map<String, Object> flatMap = root.toFlatMap();
TreeNode fromMap = TreeNode.createFromMap(flatMap);
```

### 🎯 Продвинутые демонстрационные примеры

#### AdvancedPromptDemo - Реальные сценарии использования
```java
// Комплексная демонстрация PromptGenerator
AdvancedPromptDemo.main(args);

// Сценарии включают:
// - Техническая поддержка
// - Обучение разработчиков  
// - Подготовка к собеседованиям
// - Оптимизация промптов
// - Интеграция знаний из multiple источников
```

#### ConfigKnowledgeDemo - Управление конфигурацией
```java
// Расширенное управление конфигурацией Knowledge системы
ConfigKnowledgeDemo.main(args);

// Возможности:
// - Создание KnowledgeConfig различными способами
// - Интеграция с VectorDatabase
// - Валидация конфигурации
// - Динамическое изменение настроек
// - Работа с multiple конфигурациями
```

### 🔧 Улучшенная работа с VectorData

#### Расширенная сериализация VectorData
```java
VectorData vectorData = new VectorData();

// JSON сериализация
JSONObject json = vectorData.toJson();
VectorData fromJson = VectorData.fromJson(json);

// Расширенная информация
String debugInfo = vectorData.toString();
// Output: VectorData{id='doc_001_chunk_0', text='Текст...', nodePath='[documents, ml]', ...}
```

### 🚀 Новые демо-классы

#### Полный набор демонстраций
```java
// Базовые демо
VectorDBExample.main(args);        // Основная функциональность
EmbeddingDemo.main(args);          // Работа с эмбеддингами
KnowledgeDemo.main(args);          // Загрузка знаний

// Продвинутые демо  
AdvancedPromptDemo.main(args);     // Реальные сценарии промптов
ConfigKnowledgeDemo.main(args);    // Управление конфигурацией
OllamaChatDemo.main(args);         // Интерактивный чат с RAG
PromptGeneratorDemo.main(args);    // Генерация промптов
KnowledgeExample.main(args);       // Комплексный пример знаний
```

### 📊 Расширенная статистика и мониторинг

#### Мониторинг производительности
```java
// Статистика SemanticChunker
String chunkerConfig = chunker.getConfigInfo();
double currentThreshold = chunker.getSimilarityThreshold();

// Статистика базы данных
int vectorCount = vectorDB.getVectorCount();
int nodeCount = vectorDB.getTreeNodeCount();

// Детальная информация о чанках
List<VectorData> allVectors = vectorDB.exactSearch("");
for (VectorData vector : allVectors) {
    System.out.println("ID: " + vector.getId());
    System.out.println("Document: " + vector.getDocumentId()); 
    System.out.println("Chunk Index: " + vector.getChunkIndex());
    System.out.println("Node Path: " + vector.getNodePath());
    System.out.println("Text: " + vector.getText().substring(0, 50) + "...");
}
```

### 🔄 Расширенные сценарии использования

#### Динамическая настройка чанкинга
```java
// Адаптивный чанкинг на основе контента
KnowledgeLoader loader = new KnowledgeLoader(vectorDB, knowledgeConfig);

// Мониторинг текущих настроек
double currentThreshold = loader.getCurrentSimilarityThreshold();
String chunkerConfig = loader.getSemanticChunkerConfig();

// Динамическая адаптация
loader.setSimilarityThreshold(0.6);  // Более агрессивный чанкинг
loader.printKnowledgeStats();        // Обновленная статистика
```

#### Многомодельная архитектура
```java
// Разные модели для разных задач
SemanticChunker embeddingChunker = new SemanticChunker(
    "http://localhost:11434", 
    "all-minilm:22m",  // Для эмбеддингов
    0.8
);

KnowledgeConfig generationConfig = new KnowledgeConfig(
    "http://localhost:11434",
    "llama3.2",        // Для генерации
    0.8, true, true
);

// Специализированные клиенты
OllamaKnowledgeClient knowledgeClient = new OllamaKnowledgeClient(
    vectorDB, generationConfig
);
```

### 🛠️ Утилиты и вспомогательные методы

#### Вспомогательные функции TreeNode
```java
// Поиск по шаблону
List<Map.Entry<List<Object>, Object>> patternResults = 
    root.findValuesByPattern("searchPattern");

// Плоское представление
Map<String, Object> flatMap = root.toFlatMap();

// Глубокое копирование
TreeNode copy = root.deepCopy();

// Статические конструкторы
Map<List<Object>, Object> paths = new HashMap<>();
TreeNode fromPaths = TreeNode.createFromPaths(paths);
```

