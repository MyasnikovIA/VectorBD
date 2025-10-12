# VectorDB - Полная документация 

## Оглавление
1. [Введение](#введение)
2. [Архитектура системы](#архитектура-системы)
3. [Быстрый старт](#быстрый-старт)
4. [Детальное описание компонентов](#детальное-описание-компонентов)
5. [Новый функционал](#новый-функционал)
6. [Примеры использования](#примеры-использования)
7. [Конфигурация](#конфигурация)
8. [Лучшие практики](#лучшие-практики)
9. [Устранение неполадок](#устранение-неполадок)

## Новый функционал

### 🌳 BinaryTreeNode - Бинарное дерево

Новый класс для оптимизированного хранения иерархических данных.

#### Создание и использование
```java
BinaryTreeNode binaryTree = new BinaryTreeNode();
// Используется для специализированных операций с бинарными деревьями
// Оптимизировано для быстрого поиска и вставки
```

### 🧩 Расширенный SemanticChunker

#### Улучшенное управление конфигурацией
```java
SemanticChunker chunker = new SemanticChunker(
    "http://localhost:11434",
    "all-minilm:22m", 
    0.8
);

// Получение информации о конфигурации
String configInfo = chunker.getConfigInfo();
System.out.println(configInfo);
// Output: SemanticChunker Config: model=all-minilm:22m, similarityThreshold=0.80, url=http://localhost:11434

// Получение информации о модели
String embeddingModel = chunker.getEmbeddingModel();
String ollamaUrl = chunker.getOllamaBaseUrl();
double currentThreshold = chunker.getSimilarityThreshold();
```

#### Расширенная работа с чанками
```java
List<SemanticChunker.Chunk> chunks = chunker.semanticChunking(text, 1000);

for (SemanticChunker.Chunk chunk : chunks) {
    // Детальная информация о чанке
    System.out.println(chunk.getFullInfo());
    // Output: 
    // === CHUNK INFO ===
    // Position: 0
    // Length: 156 characters
    // Text:
    // Текст чанка...
    
    // Отдельные свойства
    System.out.println("Position: " + chunk.getPosition());
    System.out.println("Length: " + chunk.getLength());
    System.out.println("Text: " + chunk.getText());
    float[] embedding = chunk.getEmbedding();
}
```

### 🗂️ Усовершенствованный TreeNode

#### Расширенная сериализация
```java
TreeNode root = new TreeNode();

// Сериализация в JSON
String jsonString = root.toJsonString();
JSONObject json = root.toJson();

// Восстановление из JSON
TreeNode fromJson = TreeNode.fromJsonString(jsonString);
TreeNode fromJsonObject = TreeNode.fromJson(json);
```

#### Работа с метаданными
```java
// Установка метаданных
root.setMetadata("author", "John Doe");
root.setMetadata("version", "1.0");
root.setMetadata("created", "2024-01-01");

// Получение метаданных
Object author = root.getMetadata("author");
Map<String, Object> allMetadata = root.getMetadata();
boolean hasMetadata = root.hasMetadata("author");

// Удаление метаданных
root.removeMetadata("author");
root.clearMetadata();
```

#### Расширенный поиск и навигация
```java
// Поиск пути к значению
List<Object> path = root.getPathToNode("targetValue");

// Проверка наличия значения
boolean contains = root.containsValue("searchValue");

// Получение всех путей
Map<List<Object>, Object> allPaths = root.getAllPaths();

// Поиск по шаблону
List<Map.Entry<List<Object>, Object>> patternResults = 
    root.findValuesByPattern("searchPattern");
```

#### Анализ структуры дерева
```java
// Основные метрики
int totalNodes = root.countNodes();
int depth = root.getDepth();
int width = root.getWidth();

// Работа с листьями
List<Object> leafValues = root.getLeafValues();

// Поиск поддерева
TreeNode subtree = root.findSubtree(new Object[]{"users", "user1"});
```

#### Операции с деревьями
```java
// Слияние деревьев
TreeNode otherTree = new TreeNode();
root.mergeWith(otherTree);

// Глубокое копирование
TreeNode copy = root.deepCopy();

// Визуальное представление
String treeString = root.toTreeString();
System.out.println(treeString);
// Output:
// └── rootData
//     ├── child1
//     │   └── grandchild1
//     └── child2
```

#### Статические конструкторы
```java
// Создание из Map
Map<String, Object> map = new HashMap<>();
map.put("key1", "value1");
map.put("key2", "value2");
TreeNode fromMap = TreeNode.createFromMap(map);

// Создание из путей
Map<List<Object>, Object> paths = new HashMap<>();
paths.put(Arrays.asList("users", "user1"), "John Doe");
TreeNode fromPaths = TreeNode.createFromPaths(paths);

// Плоское представление
Map<String, Object> flatMap = root.toFlatMap();
```

### 🎯 Продвинутые демонстрационные примеры

#### AdvancedPromptDemo - Реальные сценарии
```java
// Комплексная демонстрация работы с промптами
AdvancedPromptDemo.main(args);

// Включает сценарии:
// - Техническая поддержка с реальными данными
// - Обучение новых разработчиков
// - Подготовка к техническим собеседованиям
// - Оптимизация параметров промптов
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
// - Работа с multiple конфигурациями для разных сценариев
```

### 🔧 Улучшенная работа с VectorData

#### Расширенная сериализация
```java
VectorData vectorData = new VectorData();

// JSON сериализация
JSONObject json = vectorData.toJson();
VectorData fromJson = VectorData.fromJson(json);

// Расширенная отладочная информация
String debugInfo = vectorData.toString();
// Output: VectorData{id='doc_001_chunk_0', text='Текст...', nodePath='[documents, ml]', ...}
```

### 🚀 Полный набор демонстрационных классов

#### Базовые демонстрации
```java
// Основная функциональность
VectorDBExample.main(args);

// Работа с эмбеддингами
EmbeddingDemo.main(args);

// Загрузка и использование знаний
KnowledgeDemo.main(args);
KnowledgeExample.main(args);
```

#### Продвинутые демонстрации
```java
// Реальные сценарии использования промптов
AdvancedPromptDemo.main(args);

// Управление конфигурацией системы
ConfigKnowledgeDemo.main(args);

// Интерактивный чат с RAG функциональностью
OllamaChatDemo.main(args);

// Генерация и оптимизация промптов
PromptGeneratorDemo.main(args);
```

### 📊 Расширенная статистика и мониторинг

#### Мониторинг SemanticChunker
```java
// Детальная информация о конфигурации
String chunkerConfig = chunker.getConfigInfo();
double currentThreshold = chunker.getSimilarityThreshold();

// Статистика использования
System.out.println("Current similarity threshold: " + currentThreshold);
System.out.println("Embedding model: " + chunker.getEmbeddingModel());
System.out.println("Ollama URL: " + chunker.getOllamaBaseUrl());
```

#### Детальная информация о данных
```java
// Полная статистика базы данных
int vectorCount = vectorDB.getVectorCount();
int nodeCount = vectorDB.getTreeNodeCount();

// Детальная информация о чанках
List<VectorData> allVectors = vectorDB.exactSearch("");
for (VectorData vector : allVectors) {
    System.out.println("ID: " + vector.getId());
    System.out.println("Document: " + vector.getDocumentId()); 
    System.out.println("Chunk Index: " + vector.getChunkIndex());
    System.out.println("Node Path: " + vector.getNodePath());
    System.out.println("Timestamp: " + vector.getTimestamp());
    System.out.println("Text: " + vector.getText().substring(0, 50) + "...");
}
```

### 🔄 Расширенные сценарии использования

#### Динамическая адаптация чанкинга
```java
KnowledgeLoader loader = new KnowledgeLoader(vectorDB, knowledgeConfig);

// Мониторинг текущих настроек
double currentThreshold = loader.getCurrentSimilarityThreshold();
String chunkerConfig = loader.getSemanticChunkerConfig();

// Динамическая адаптация под тип контента
loader.setSimilarityThreshold(0.6);  // Более агрессивный чанкинг для технических текстов
loader.setSimilarityThreshold(0.9);  // Более консервативный для юридических документов

// Обновленная статистика после изменений
loader.printKnowledgeStats();
```

#### Многомодельная архитектура
```java
// Специализированные модели для разных задач
SemanticChunker embeddingChunker = new SemanticChunker(
    "http://localhost:11434", 
    "all-minilm:22m",  // Оптимизирована для эмбеддингов
    0.8
);

KnowledgeConfig generationConfig = new KnowledgeConfig(
    "http://localhost:11434",
    "llama3.2",        // Оптимизирована для генерации
    0.8, true, true
);

KnowledgeConfig analysisConfig = new KnowledgeConfig(
    "http://localhost:11434", 
    "deepseek-v3.1:671b-cloud",  // Для сложного анализа
    0.7, true, true
);

// Специализированные клиенты для разных задач
OllamaKnowledgeClient knowledgeClient = new OllamaKnowledgeClient(vectorDB, generationConfig);
OllamaKnowledgeClient analysisClient = new OllamaKnowledgeClient(vectorDB, analysisConfig);
```

### 🛠️ Утилиты и вспомогательные методы

#### Вспомогательные функции TreeNode
```java
// Расширенный поиск
List<Map.Entry<List<Object>, Object>> patternResults = 
    root.findValuesByPattern("searchPattern");

// Плоское представление для экспорта
Map<String, Object> flatMap = root.toFlatMap();

// Глубокое копирование для безопасного изменения
TreeNode copy = root.deepCopy();

// Статические конструкторы для быстрого создания
Map<List<Object>, Object> paths = new HashMap<>();
TreeNode fromPaths = TreeNode.createFromPaths(paths);
```

#### Управление версиями и метаданными
```java
// Комплексная работа с метаданными
root.setMetadata("version", "1.0.0");
root.setMetadata("author", "Development Team");
root.setMetadata("created", "2024-01-01");
root.setMetadata("lastModified", "2024-01-15");

// Валидация структуры данных
if (root.hasMetadata("version")) {
    String version = (String) root.getMetadata("version");
    System.out.println("Working with version: " + version);
}
```

## Примеры использования нового функционала

### Пример 4: Система управления конфигурацией с расширенным TreeNode

```java
public class AdvancedConfigurationManager {
    private TreeNode configTree;
    
    public AdvancedConfigurationManager() {
        this.configTree = new TreeNode();
        initializeDefaultConfig();
    }
    
    private void initializeDefaultConfig() {
        // Базовая конфигурация с метаданными
        configTree.setNode(new Object[]{"database", "url"}, "jdbc:postgresql://localhost:5432/mydb");
        configTree.setNode(new Object[]{"database", "username"}, "admin");
        configTree.setNode(new Object[]{"database", "password"}, "secret");
        configTree.setNode(new Object[]{"server", "port"}, 8080);
        configTree.setNode(new Object[]{"server", "host"}, "localhost");
        
        // Метаданные конфигурации
        configTree.setMetadata("configVersion", "2.1.0");
        configTree.setMetadata("createdBy", "System Administrator");
        configTree.setMetadata("environment", "production");
    }
    
    public void saveConfigToFile(String filePath) throws IOException {
        String jsonConfig = configTree.toJsonString();
        Files.write(Paths.get(filePath), jsonConfig.getBytes());
        System.out.println("Configuration saved to: " + filePath);
    }
    
    public void loadConfigFromFile(String filePath) throws IOException {
        String jsonConfig = new String(Files.readAllBytes(Paths.get(filePath)));
        this.configTree = TreeNode.fromJsonString(jsonConfig);
        System.out.println("Configuration loaded from: " + filePath);
    }
    
    public void printConfigStructure() {
        System.out.println("=== Configuration Structure ===");
        System.out.println(configTree.toTreeString());
        
        System.out.println("=== Metadata ===");
        configTree.getMetadata().forEach((key, value) -> 
            System.out.println(key + ": " + value));
            
        System.out.println("=== Statistics ===");
        System.out.println("Total nodes: " + configTree.countNodes());
        System.out.println("Tree depth: " + configTree.getDepth());
        System.out.println("Tree width: " + configTree.getWidth());
    }
    
    public void findConfigValues(String pattern) {
        System.out.println("=== Searching for: " + pattern + " ===");
        List<Map.Entry<List<Object>, Object>> results = 
            configTree.findValuesByPattern(pattern);
            
        for (Map.Entry<List<Object>, Object> result : results) {
            System.out.println("Path: " + result.getKey() + " -> Value: " + result.getValue());
        }
    }
    
    public static void main(String[] args) throws Exception {
        AdvancedConfigurationManager configManager = new AdvancedConfigurationManager();
        
        // Демонстрация возможностей
        configManager.printConfigStructure();
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // Поиск конфигурационных значений
        configManager.findConfigValues("localhost");
        configManager.findConfigValues("admin");
        
        // Сохранение и загрузка
        configManager.saveConfigToFile("config.json");
        
        // Создание новой конфигурации и загрузка
        AdvancedConfigurationManager newManager = new AdvancedConfigurationManager();
        newManager.loadConfigFromFile("config.json");
        newManager.printConfigStructure();
    }
}
```

### Пример 5: Продвинутая система семантического поиска

```java
public class AdvancedSemanticSearch {
    private VectorDatabase vectorDB;
    private SemanticChunker chunker;
    
    public AdvancedSemanticSearch() throws Exception {
        this.chunker = new SemanticChunker(
            "http://localhost:11434",
            "all-minilm:22m",
            0.8
        );
        
        this.vectorDB = new VectorDatabase("./data/advanced_search", chunker);
        loadSampleData();
    }
    
    private void loadSampleData() throws Exception {
        // Загрузка разнообразных данных для демонстрации
        String[] documents = {
            "Машинное обучение и искусственный интеллект преобразуют современные технологии",
            "Глубокое обучение использует нейронные сети для решения сложных задач",
            "Векторные базы данных обеспечивают эффективный семантический поиск",
            "Java programming language is widely used for enterprise applications",
            "Spring Framework simplifies development of Java applications",
            "Базы данных хранят и управляют структурированной информацией"
        };
        
        for (int i = 0; i < documents.length; i++) {
            vectorDB.storeTextWithChunking(
                documents[i],
                "doc_" + i,
                new Object[]{"documents", "category_" + (i % 3)}
            );
        }
    }
    
    public void demonstrateAdvancedSearch() throws Exception {
        System.out.println("=== Advanced Semantic Search Demo ===");
        
        // Демонстрация различных порогов схожести
        double[] thresholds = {0.5, 0.7, 0.9};
        String query = "машинное обучение и нейронные сети";
        
        for (double threshold : thresholds) {
            System.out.println("\n--- Similarity Threshold: " + threshold + " ---");
            
            chunker.setSimilarityThreshold(threshold);
            List<VectorSearchResult> results = vectorDB.similaritySearch(query, 3);
            
            System.out.println("Found " + results.size() + " results:");
            for (VectorSearchResult result : results) {
                System.out.printf("Similarity: %.4f | Text: %s%n",
                    result.getSimilarity(),
                    result.getVectorData().getText());
            }
        }
        
        // Демонстрация информации о конфигурации
        System.out.println("\n=== Chunker Configuration ===");
        System.out.println(chunker.getConfigInfo());
    }
    
    public void analyzeChunking(String text) throws Exception {
        System.out.println("\n=== Text Chunking Analysis ===");
        System.out.println("Original text length: " + text.length() + " characters");
        
        List<SemanticChunker.Chunk> chunks = chunker.semanticChunking(text, 500);
        
        System.out.println("Created " + chunks.size() + " chunks:");
        for (int i = 0; i < chunks.size(); i++) {
            SemanticChunker.Chunk chunk = chunks.get(i);
            System.out.println("\nChunk " + (i + 1) + ":");
            System.out.println("Position: " + chunk.getPosition());
            System.out.println("Length: " + chunk.getLength());
            System.out.println("Text preview: " + 
                chunk.getText().substring(0, Math.min(100, chunk.getText().length())) + "...");
        }
    }
    
    public static void main(String[] args) throws Exception {
        AdvancedSemanticSearch search = new AdvancedSemanticSearch();
        
        // Демонстрация расширенного поиска
        search.demonstrateAdvancedSearch();
        
        // Анализ чанкинга
        String testText = """
            Искусственный интеллект - это область компьютерных наук, которая занимается 
            созданием интеллектуальных машин, способных выполнять задачи, требующие 
            человеческого интеллекта. Машинное обучение является подразделом 
            искусственного интеллекта и focuses на разработке алгоритмов, которые 
            могут обучаться на данных и делать прогнозы или принимать решения без 
            явного программирования. Глубокое обучение, в свою очередь, является 
            подразделом машинного обучения и использует нейронные сети с множеством слоев.
            """;
            
        search.analyzeChunking(testText);
        
        search.vectorDB.close();
    }
}
```

### Пример 6: Комплексная система управления знаниями с расширенным функционалом

```java
public class ComprehensiveKnowledgeManager {
    private VectorDatabase vectorDB;
    private KnowledgeLoader knowledgeLoader;
    private OllamaKnowledgeClient knowledgeClient;
    private TreeNode knowledgeMetadata;
    
    public ComprehensiveKnowledgeManager() throws Exception {
        KnowledgeConfig config = new KnowledgeConfig(
            "http://localhost:11434",
            "llama3.2", 
            0.8, true, true
        );
        
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434",
            "all-minilm:22m",
            0.8
        );
        
        this.vectorDB = new VectorDatabase("./data/comprehensive_kb", chunker);
        this.knowledgeLoader = new KnowledgeLoader(vectorDB, config);
        this.knowledgeClient = new OllamaKnowledgeClient(vectorDB, config);
        this.knowledgeMetadata = new TreeNode();
        
        initializeKnowledgeBase();
    }
    
    private void initializeKnowledgeBase() throws Exception {
        // Загрузка различных типов знаний
        loadTechnicalDocumentation();
        loadBusinessKnowledge();
        loadProceduralGuidelines();
        
        // Инициализация метаданных
        knowledgeMetadata.setNode(new Object[]{"statistics", "loadedDocuments"}, 3);
        knowledgeMetadata.setNode(new Object[]{"statistics", "totalChunks"}, 0); // Будет обновлено
        knowledgeMetadata.setMetadata("knowledgeBaseVersion", "1.0.0");
        knowledgeMetadata.setMetadata("lastUpdated", new Date().toString());
    }
    
    private void loadTechnicalDocumentation() throws Exception {
        String techDocs = """
            Архитектура микросервисов: 
            Микросервисная архитектура - это подход к разработке приложений 
            как набора небольших сервисов, каждый из которых работает в своем процессе 
            и взаимодействует с другими через легковесные механизмы.
            
            Преимущества:
            - Независимое развертывание сервисов
            - Технологическое разнообразие
            - Устойчивость к отказам
            - Масштабируемость
            
            Контейнеризация с Docker:
            Docker позволяет упаковывать приложения и их зависимости в контейнеры,
            которые могут работать в любой среде с Docker.
            """;
            
        int chunks = knowledgeLoader.loadText(techDocs, "technical_docs",
            new Object[]{"knowledge", "technical", "architecture"}, 400, "tech_docs");
            
        knowledgeMetadata.setNode(new Object[]{"documents", "technical", "chunks"}, chunks);
    }
    
    private void loadBusinessKnowledge() throws Exception {
        String businessKnowledge = """
            Бизнес-процессы компании:
            Основной процесс продаж включает этапы: лидогенерация, квалификация,
            презентация, заключение сделки и постпродажное обслуживание.
            
            Метрики успеха:
            - Конверсия лидов: 15-20%
            - Среднее время сделки: 30 дней
            - Удовлетворенность клиентов: 95%
            
            Стратегия роста:
            Фокус на upsell существующим клиентам и экспансия в новые регионы.
            """;
            
        int chunks = knowledgeLoader.loadText(businessKnowledge, "business_knowledge",
            new Object[]{"knowledge", "business", "processes"}, 350, "business_kb");
            
        knowledgeMetadata.setNode(new Object[]{"documents", "business", "chunks"}, chunks);
    }
    
    private void loadProceduralGuidelines() throws Exception {
        String procedures = """
            Процедура код-ревью:
            1. Проверка кода на соответствие стандартам
            2. Анализ архитектурных решений
            3. Проверка тестового покрытия
            4. Оценка производительности
            5. Проверка безопасности
            
            Стандарты разработки:
            - Использование Java Code Conventions
            - Минимальное покрытие тестами 80%
            - Документирование публичных API
            - Регулярное рефакторинг кода
            """;
            
        int chunks = knowledgeLoader.loadText(procedures, "procedural_guidelines",
            new Object[]{"knowledge", "procedures", "development"}, 300, "procedures");
            
        knowledgeMetadata.setNode(new Object[]{"documents", "procedures", "chunks"}, chunks);
    }
    
    public void printComprehensiveStats() {
        System.out.println("=== Comprehensive Knowledge Base Statistics ===");
        
        // Статистика векторной базы
        System.out.println("Vector database statistics:");
        System.out.println("  Total vectors: " + vectorDB.getVectorCount());
        System.out.println("  Total tree nodes: " + vectorDB.getTreeNodeCount());
        
        // Статистика загрузчика знаний
        System.out.println("Knowledge loader statistics:");
        System.out.println("  Current similarity threshold: " + 
            knowledgeLoader.getCurrentSimilarityThreshold());
        System.out.println("  Chunker config: " + 
            knowledgeLoader.getSemanticChunkerConfig());
        
        // Метаданные знаний
        System.out.println("Knowledge metadata:");
        System.out.println(knowledgeMetadata.toTreeString());
        
        // Конфигурация Knowledge
        Map<String, Object> config = knowledgeLoader.getKnowledgeConfig();
        System.out.println("Knowledge configuration:");
        config.forEach((key, value) -> System.out.println("  " + key + ": " + value));
    }
    
    public void interactiveKnowledgeQuery() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n🎓 Interactive Knowledge Query Session");
        System.out.println("Type your questions about technology, business, or procedures");
        System.out.println("Type 'exit' to end the session\n");
        
        while (true) {
            System.out.print("❓ Your question: ");
            String question = scanner.nextLine().trim();
            
            if (question.equalsIgnoreCase("exit")) {
                break;
            }
            
            if (question.isEmpty()) {
                continue;
            }
            
            try {
                // Автоматическая настройка порога в зависимости от типа вопроса
                if (question.toLowerCase().contains("техн") || 
                    question.toLowerCase().contains("архитектур")) {
                    knowledgeLoader.setSimilarityThreshold(0.8); // Высокая точность для тех. вопросов
                } else if (question.toLowerCase().contains("бизнес") || 
                          question.toLowerCase().contains("процесс")) {
                    knowledgeLoader.setSimilarityThreshold(0.7); // Средняя точность для бизнес-вопросов
                } else {
                    knowledgeLoader.setSimilarityThreshold(0.6); // Широкий охват для общих вопросов
                }
                
                System.out.print("💡 Answer: ");
                String response = knowledgeClient.generateResponseWithKnowledge(question);
                System.out.println(response);
                System.out.println("(Used similarity threshold: " + 
                    knowledgeLoader.getCurrentSimilarityThreshold() + ")\n");
                
            } catch (Exception e) {
                System.out.println("❌ Error: " + e.getMessage() + "\n");
            }
        }
        
        scanner.close();
        System.out.println("✅ Knowledge query session completed.");
    }
    
    public static void main(String[] args) throws Exception {
        ComprehensiveKnowledgeManager knowledgeManager = new ComprehensiveKnowledgeManager();
        
        // Комплексная статистика
        knowledgeManager.printComprehensiveStats();
        
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        // Интерактивная сессия запросов
        knowledgeManager.interactiveKnowledgeQuery();
        
        knowledgeManager.vectorDB.close();
    }
}
```

## Лучшие практики для нового функционала

### 1. Оптимальное использование расширенного TreeNode

```java
public class TreeNodeBestPractices {
    
    // Использование метаданных для управления версиями
    public static void setupVersionedTree(TreeNode root) {
        root.setMetadata("version", "1.0.0");
        root.setMetadata("created", new Date().toString());
        root.setMetadata("author", "System");
        
        // Структура данных с версионированием
        root.setNode(new Object[]{"data", "v1", "users"}, "userListV1");
        root.setNode(new Object[]{"data", "v2", "users"}, "userListV2");
    }
    
    // Эффективный поиск в больших деревьях
    public static void efficientTreeSearch(TreeNode root, String pattern) {
        // Используйте findValuesByPattern для сложных поисков
        List<Map.Entry<List<Object>, Object>> results = 
            root.findValuesByPattern(pattern);
        
        // Используйте getPathToNode для точного поиска
        List<Object> path = root.getPathToNode("specificValue");
    }
    
    // Безопасные операции с деревьями
    public static TreeNode safeTreeOperations(TreeNode original) {
        // Всегда используйте deepCopy для модификаций
        TreeNode workingCopy = original.deepCopy();
        
        // Выполняйте операции на копии
        workingCopy.setNode(new Object[]{"new", "path"}, "newValue");
        
        return workingCopy;
    }
}
```

### 2. Управление конфигурацией SemanticChunker

```java
public class ChunkerOptimization {
    
    public static void optimizeForContentType(String contentType, SemanticChunker chunker) {
        switch (contentType.toLowerCase()) {
            case "technical":
                chunker.setSimilarityThreshold(0.8); // Высокая точность
                break;
            case "legal":
                chunker.setSimilarityThreshold(0.9); // Очень высокая точность
                break;
            case "creative":
                chunker.setSimilarityThreshold(0.6); // Широкий охват
                break;
            default:
                chunker.setSimilarityThreshold(0.7); // Баланс
        }
    }
    
    public static void monitorChunkerPerformance(SemanticChunker chunker) {
        System.out.println("=== Chunker Performance Monitor ===");
        System.out.println("Configuration: " + chunker.getConfigInfo());
        System.out.println("Current threshold: " + chunker.getSimilarityThreshold());
        System.out.println("Embedding model: " + chunker.getEmbeddingModel());
    }
}
```

Эти дополнения к документации полностью охватывают новый функционал, предоставляя подробные инструкции, примеры использования и лучшие практики для работы с расширенными возможностями VectorDB.