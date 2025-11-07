# VectorDB - Полная документация

## Оглавление
1. [Введение](#введение)
2. [Архитектура системы](#архитектура-системы)
3. [Быстрый старт](#быстрый-старт)
4. [Детальное описание компонентов](#детальное-описание-компонентов)
5. [Расширенный функционал](#расширенный-функционал)
6. [Управление памятью и индексами](#управление-памятью-и-индексами)
7. [JSON интеграция](#json-интеграция)
8. [Примеры использования](#примеры-использования)
9. [Конфигурация](#конфигурация)
10. [Лучшие практики](#лучшие-практики)
11. [Устранение неполадок](#устранение-неполадок)

## Введение

VectorDB - это высокопроизводительная векторная база данных на Java с поддержкой семантического поиска, RAG (Retrieval-Augmented Generation) и бинарной сериализации. Система предназначена для эффективного хранения и поиска векторных представлений текстовых данных с использованием современных подходов к обработке естественного языка.

## Архитектура системы

### Основные компоненты

- **BinaryVectorDatabase** - основная база данных с бинарной сериализацией и управлением памятью
- **BinaryTreeNode** - оптимизированная структура для хранения иерархических данных
- **BinaryVectorData** - контейнер для векторных данных с расширенной сериализацией
- **SemanticChunker** - интеллектуальное разбиение текста на семантические чанки
- **VectorIndex** - индекс для быстрого векторного поиска
- **KnowledgeLoader** - загрузчик знаний с поддержкой различных форматов
- **OllamaKnowledgeClient** - клиент для работы с моделями Ollama
- **SQLParser** - SQL-подобный интерфейс для запросов
- **BinaryVectorDBJsonManager** - менеджер для работы с JSON форматом

## Быстрый старт

### Базовая настройка

```java
// Создание конфигурации Knowledge
KnowledgeConfig config = new KnowledgeConfig(
    "http://localhost:11434",
    "deepseek-v3.1:671b-cloud",
    0.7,
    true,
    true
);

// Инициализация SemanticChunker
SemanticChunker chunker = new SemanticChunker(
    "http://localhost:11434",
    "all-minilm:22m",
    0.7
);

// Создание векторной базы данных (500 MB по умолчанию)
BinaryVectorDatabase vectorDB = new BinaryVectorDatabase("./data", chunker);

// Создание базы данных с 2 GB памяти
BinaryVectorDatabase largeVectorDB = new BinaryVectorDatabase("./data", chunker, 2L * 1024 * 1024 * 1024);

// Создание KnowledgeLoader
KnowledgeLoader loader = new KnowledgeLoader(vectorDB, config);

// Создание клиента для работы с Ollama
OllamaKnowledgeClient knowledgeClient = new OllamaKnowledgeClient(vectorDB, config);

// Создание JSON менеджера
BinaryVectorDBJsonManager jsonManager = new BinaryVectorDBJsonManager(vectorDB);
```

### Загрузка и поиск данных

```java
// Загрузка текстового файла
int chunksCount = loader.loadTextFile("document.txt", "doc1", 
    new Object[]{"documents", "category1"}, 1000);

// Семантический поиск
List<VectorSearchResult> results = vectorDB.similaritySearch("запрос для поиска", 5);

// Генерация ответа с использованием знаний
String response = knowledgeClient.generateResponseWithKnowledge("Ваш вопрос");

// Экспорт в JSON
JSONArray jsonData = jsonManager.exportAllVectorDataToJson();
```

## Детальное описание компонентов

### BinaryVectorDatabase

Основной класс для работы с векторной базой данных с бинарной сериализацией и управлением памятью.

#### Конструкторы:
```java
// Конструктор по умолчанию (500 MB)
BinaryVectorDatabase(String databasePath, SemanticChunker semanticChunker)

// Конструктор с настройкой памяти
BinaryVectorDatabase(String databasePath, SemanticChunker semanticChunker, long maxMemoryBytes)
```

#### Основные методы:
```java
// Хранение данных
void storeTextWithChunking(String text, String documentId, Object[] path)
void storeVectorData(BinaryVectorData vectorData)
void storeTreeNode(String nodeId, BinaryTreeNode node, Object[] path)

// Поиск
List<VectorSearchResult> similaritySearch(String query, int limit)
List<VectorSearchResult> similaritySearch(float[] queryVector, int limit)
List<BinaryVectorData> searchByPath(String pathPattern)
List<BinaryVectorData> exactSearch(String searchText)

// Управление
void saveDatabase()
void loadDatabase()
void removeVectorData(String vectorId)
void removeTreeNode(String nodeId)
void close() // Грациозное завершение
```

### BinaryVectorData

Контейнер для векторных данных с расширенной бинарной сериализацией и версионированием.

```java
BinaryVectorData vectorData = new BinaryVectorData(
    "doc1_chunk_0",
    embedding,
    "Текст чанка",
    "Метаданные",
    "[documents, category1]",
    "doc1",
    0  // chunkIndex
);

// Сериализация в бинарный формат
byte[] binaryData = vectorData.serialize();

// Десериализация
BinaryVectorData restored = BinaryVectorData.deserialize(binaryData);

// Автоматическое извлечение chunkIndex из ID
vectorData.setId("document_chunk_5");
int chunkIndex = vectorData.getChunkIndex(); // Вернет 5
```

### BinaryTreeNode

Оптимизированная структура для хранения иерархических данных с бинарной сериализацией.

```java
BinaryTreeNode node = new BinaryTreeNode("content");
node.setMetadata("key", "value");
node.setLeft(leftNode);
node.setRight(rightNode);

// Сериализация
byte[] data = node.serialize();
BinaryTreeNode restored = BinaryTreeNode.deserialize(data);
```

### SemanticChunker

Интеллектуальное разбиение текста на семантические чанки с использованием эмбеддингов.

```java
List<SemanticChunker.Chunk> chunks = semanticChunker.semanticChunking(text, 1000);

for (Chunk chunk : chunks) {
    System.out.println("Text: " + chunk.getText());
    System.out.println("Position: " + chunk.getPosition());
    System.out.println("Length: " + chunk.getLength());
    float[] embedding = chunk.getEmbedding();
    
    // Детальная информация
    System.out.println(chunk.getFullInfo());
}
```

### VectorIndex

Высокопроизводительный индекс для векторного поиска.

```java
VectorIndex index = new VectorIndex();
index.addVector("vector1", embedding);
List<VectorIndex.SearchResult> results = index.search(queryVector, 10, "cosine");
```

## Расширенный функционал

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

// Динамическое изменение порога
chunker.setSimilarityThreshold(0.9);

// Получение информации о модели
String embeddingModel = chunker.getEmbeddingModel();
String ollamaUrl = chunker.getOllamaBaseUrl();
double currentThreshold = chunker.getSimilarityThreshold();
```

### 🗂️ KnowledgeConfig - Централизованное управление конфигурацией

```java
KnowledgeConfig config = new KnowledgeConfig(
    "http://localhost:11434",
    "deepseek-v3.1:671b-cloud",
    0.7,
    true,  // saveHistory
    true   // enabled
);

// Проверка валидности
if (config.isValid()) {
    System.out.println("Configuration is valid");
}

// Получение всех настроек
Map<String, Object> settings = config.getAllSettings();

// Вывод конфигурации
config.printConfig();
```

### 📚 KnowledgeLoader - Интеллектуальная загрузка знаний

#### Расширенная загрузка различных источников
```java
KnowledgeLoader loader = new KnowledgeLoader(vectorDB, knowledgeConfig);

// Загрузка текстового файла
int chunks = loader.loadTextFile("document.txt", "doc1", 
    new Object[]{"documents", "tech"}, 1000);

// Загрузка текстовой строки
int chunks2 = loader.loadText("Текст для обработки", "doc2",
    new Object[]{"knowledge", "business"}, 800, "business_kb");

// Пакетная загрузка из директории
int totalChunks = loader.loadTextDirectory("./docs", "base_doc",
    new Object[]{"documents"}, 1000, new String[]{".txt", ".md"});

// Пакетная загрузка из Map
Map<String, String> documents = new HashMap<>();
documents.put("doc1", "Текст документа 1");
documents.put("doc2", "Текст документа 2");
Map<String, Integer> results = loader.loadTextBatch(documents, "batch_docs",
    new Object[]{"documents"}, 600);
```

#### Управление параметрами чанкинга
```java
// Динамическая настройка порога схожести
loader.setSimilarityThreshold(0.6);  // Более агрессивный чанкинг
loader.setSimilarityThreshold(0.9);  // Более консервативный

// Мониторинг текущих настроек
double currentThreshold = loader.getCurrentSimilarityThreshold();
String chunkerConfig = loader.getSemanticChunkerConfig();

// Статистика базы знаний
loader.printKnowledgeStats();
Map<String, Object> stats = loader.exportStats();

// Оптимизация базы
loader.optimizeKnowledgeBase();
```

### 🤖 OllamaKnowledgeClient - Продвинутая работа с AI моделями

#### Многоуровневая генерация ответов
```java
OllamaKnowledgeClient client = new OllamaKnowledgeClient(vectorDB, config);

// Базовая генерация
String response = client.generateResponse("Простой вопрос");

// Генерация с использованием знаний из базы
String knowledgeResponse = client.generateResponseWithKnowledge("Сложный вопрос");

// Потоковая генерация
Iterator<String> stream = client.generateResponseStream("Вопрос для потоковой обработки");
while (stream.hasNext()) {
    System.out.print(stream.next());
}

// Расширенная генерация со статистикой
Map<String, Object> detailedResponse = client.generateResponseWithStats("Вопрос");

// Генерация с контекстом
String contextAwareResponse = client.generateResponseWithContext("Вопрос", "Дополнительный контекст");
```

#### Интерактивный режим
```java
// Запуск интерактивного чата
client.startInteractiveChat();

// Тестирование подключений
Map<String, Boolean> testResults = client.testConnections();
client.printConnectionTest();
```

### 🔍 PromptGenerator - Интеллектуальная генерация промптов

```java
PromptGenerator generator = new PromptGenerator(vectorDB, knowledgeConfig);

// Создание контекстного промпта
String contextPrompt = generator.createContextPrompt("Запрос пользователя", 5, 0.7);

// Генерация вопросов
String questionPrompt = generator.createQuestionGenerationPrompt("Тема", 10, 0.8);

// Суммаризация
String summarizationPrompt = generator.createSummarizationPrompt("Тема для суммаризации", 5, 0.7);

// Сравнение концепций
String comparisonPrompt = generator.createComparisonPrompt("Концепция1", "Концепция2", 0.7);

// Объяснение сложных тем
String explanationPrompt = generator.createExplanationPrompt("Сложная тема", 0.8);
```

### 🗃️ SQLParser - SQL-подобный интерфейс

```java
SQLParser parser = new SQLParser(vectorDB);

// SELECT запросы
List<JSONObject> results = parser.execute(
    "SELECT * FROM documents WHERE text LIKE '%машинное обучение%' LIMIT 10"
);

// INSERT запросы
List<JSONObject> insertResult = parser.execute(
    "INSERT INTO documents (id, content, category) VALUES ('doc1', 'Текст документа', 'технический')"
);

// Семантический поиск через SQL-интерфейс
List<JSONObject> semanticResults = parser.semanticSearch("запрос семантического поиска", 5);

// Гибридный поиск
List<JSONObject> hybridResults = parser.hybridSearch("комплексный запрос", 10);
```

## Управление памятью и индексами

### 🚀 Управление памятью в BinaryVectorDatabase

```java
// Создание базы с ограничением памяти
BinaryVectorDatabase db = new BinaryVectorDatabase("./data", chunker, 2L * 1024 * 1024 * 1024);

// Мониторинг использования памяти
Map<String, Object> memoryStats = db.getMemoryStats();
System.out.println("Memory usage: " + memoryStats.get("estimatedUsageMB") + " MB");
System.out.println("Max memory: " + memoryStats.get("maxMemoryMB") + " MB");

// Грациозное завершение с сохранением данных
db.close();
```

### 📊 Система индексов

#### Создание и управление индексами
```java
// Создание индексов для ускорения поиска
db.createIndex("content_index", "content");
db.createIndex("author_index", "metadata.author");
db.createIndex("timestamp_index", "metadata.created");

// Поиск по индексу
List<String> results = db.searchByIndex("content_index", "машинное обучение");

// Получение информации об индексах
Map<String, Object> indexInfo = db.getIndexesInfo();
System.out.println("Indexes: " + indexInfo);

// Удаление индекса
db.dropIndex("author_index");
```

#### Статистика индексов
```java
Map<String, Object> indexesInfo = db.getIndexesInfo();
for (Map.Entry<String, Object> entry : indexesInfo.entrySet()) {
    String indexName = entry.getKey();
    Map<String, Object> info = (Map<String, Object>) entry.getValue();
    System.out.println(indexName + ": " + info.get("size") + " entries, " + 
        info.get("estimatedMemoryKB") + " KB");
}
```

## JSON интеграция

### 🔄 BinaryVectorDBJsonManager - Полная поддержка JSON

#### Экспорт данных
```java
BinaryVectorDBJsonManager jsonManager = new BinaryVectorDBJsonManager(vectorDB);

// Экспорт всех данных
JSONArray allVectors = jsonManager.exportAllVectorDataToJson();
JSONArray allNodes = jsonManager.exportAllTreeNodesToJson();

// Экспорт отдельных объектов
JSONObject vectorJson = jsonManager.exportVectorDataToJson("doc1_chunk_0");
JSONObject nodeJson = jsonManager.exportTreeNodeToJson("node123");

// Поиск и экспорт
JSONArray searchResults = jsonManager.searchAndExportToJson("машинное обучение", 5);
JSONArray pathResults = jsonManager.searchByPathAndExportToJson("documents/tech");

// Экспорт статистики
JSONObject stats = jsonManager.exportDatabaseStatsToJson();
```

#### Импорт данных
```java
// Импорт из JSON массива
JSONArray vectorsArray = new JSONArray("[...]");
int importedVectors = jsonManager.importVectorDataFromJson(vectorsArray);

JSONArray nodesArray = new JSONArray("[...]");
int importedNodes = jsonManager.importTreeNodesFromJson(nodesArray);

// Пакетный импорт
Map<String, Integer> batchResults = jsonManager.batchImportFromJsonFile("import_data.json");
```

#### Работа с файлами
```java
// Сохранение всей базы в JSON файлы
jsonManager.saveDatabaseToJsonFiles("./json_export");

// Загрузка базы из JSON файлов
jsonManager.loadDatabaseFromJsonFiles("./json_export");

// Пакетный экспорт в один файл
jsonManager.batchExportToJsonFile("./full_export.json");
```

#### Сериализация отдельных объектов
```java
// VectorData в JSON
BinaryVectorData vectorData = database.findVectorData("some_id");
JSONObject jsonObject = jsonManager.vectorDataToJsonObject(vectorData);
String jsonString = jsonManager.vectorDataToJsonString(vectorData);

// Восстановление из JSON
BinaryVectorData restoredVector = jsonManager.vectorDataFromJsonString(jsonString);

// TreeNode в JSON
BinaryTreeNode treeNode = database.getTreeNode("node123");
JSONObject nodeJson = jsonManager.treeNodeToJsonObject(treeNode, "node123");
String nodeJsonString = jsonManager.treeNodeToJsonString(treeNode, "node123");

// Восстановление TreeNode
BinaryTreeNode restoredNode = jsonManager.treeNodeFromJsonString(nodeJsonString);
```

#### Фильтрация и экспорт
```java
// Фильтрация по критериям
Map<String, Object> filters = new HashMap<>();
filters.put("documentId", "tech_docs");
filters.put("minTimestamp", 1672531200000L); // 2023-01-01
filters.put("textContains", "искусственный интеллект");

JSONArray filteredData = jsonManager.filterAndExportVectorData(filters);
```

## Примеры использования

### Пример 1: Базовая работа с векторной базой

```java
public class BasicVectorDBExample {
    public static void main(String[] args) throws Exception {
        // Инициализация
        KnowledgeConfig config = new KnowledgeConfig(
            "http://localhost:11434",
            "deepseek-v3.1:671b-cloud",
            0.7, true, true
        );
        
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434",
            "all-minilm:22m",
            0.7
        );
        
        BinaryVectorDatabase vectorDB = new BinaryVectorDatabase("./data/basic_example", chunker);
        KnowledgeLoader loader = new KnowledgeLoader(vectorDB, config);
        BinaryVectorDBJsonManager jsonManager = new BinaryVectorDBJsonManager(vectorDB);
        
        // Загрузка данных
        String text = "Машинное обучение - это область искусственного интеллекта, " +
                     "которая позволяет компьютерам обучаться на данных без явного программирования.";
        
        loader.loadText(text, "ml_intro", 
            new Object[]{"knowledge", "ai", "machine_learning"}, 500, "ml_knowledge");
        
        // Создание индексов
        vectorDB.createIndex("content_index", "content");
        vectorDB.createIndex("category_index", "metadata.category");
        
        // Поиск
        List<VectorSearchResult> results = vectorDB.similaritySearch("обучение на данных", 3);
        
        for (VectorSearchResult result : results) {
            System.out.printf("Similarity: %.4f | Text: %s%n",
                result.getSimilarity(),
                result.getVectorData().getText());
        }
        
        // Экспорт в JSON
        JSONArray jsonExport = jsonManager.exportAllVectorDataToJson();
        System.out.println("Exported " + jsonExport.length() + " vectors to JSON");
        
        // Статистика памяти
        Map<String, Object> memoryStats = vectorDB.getMemoryStats();
        System.out.println("Memory usage: " + memoryStats.get("estimatedUsageMB") + " MB");
        
        vectorDB.close();
    }
}
```

### Пример 2: Интерактивный чат с знаниями и индексами

```java
public class InteractiveKnowledgeChat {
    public static void main(String[] args) throws Exception {
        KnowledgeConfig config = new KnowledgeConfig(
            "http://localhost:11434",
            "deepseek-v3.1:671b-cloud",
            0.7, true, true
        );
        
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", 
            "all-minilm:22m",
            0.7
        );
        
        // Создание базы с 1 GB памяти
        BinaryVectorDatabase vectorDB = new BinaryVectorDatabase("./data/chat", chunker, 1024 * 1024 * 1024);
        OllamaKnowledgeClient client = new OllamaKnowledgeClient(vectorDB, config);
        
        // Создание индексов для ускорения поиска
        vectorDB.createIndex("content_index", "content");
        vectorDB.createIndex("topic_index", "metadata.topic");
        
        // Запуск интерактивного чата
        client.startInteractiveChat();
        
        // Сохранение статистики перед закрытием
        Map<String, Object> stats = vectorDB.getMemoryStats();
        System.out.println("Final memory stats: " + stats);
        
        vectorDB.close();
    }
}
```

### Пример 3: Продвинутая система с JSON экспортом

```java
public class AdvancedJsonExportSystem {
    public static void main(String[] args) throws Exception {
        KnowledgeConfig config = new KnowledgeConfig(
            "http://localhost:11434",
            "deepseek-v3.1:671b-cloud", 
            0.7, true, true
        );
        
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434",
            "all-minilm:22m",
            0.7
        );
        
        // База данных с 2 GB памяти
        BinaryVectorDatabase vectorDB = new BinaryVectorDatabase("./data/advanced_system", chunker, 2L * 1024 * 1024 * 1024);
        KnowledgeLoader loader = new KnowledgeLoader(vectorDB, config);
        BinaryVectorDBJsonManager jsonManager = new BinaryVectorDBJsonManager(vectorDB);
        OllamaKnowledgeClient client = new OllamaKnowledgeClient(vectorDB, config);
        
        // Пакетная загрузка документов
        Map<String, String> documents = new HashMap<>();
        documents.put("ai_basics", "Искусственный интеллект - это...");
        documents.put("ml_fundamentals", "Машинное обучение включает...");
        documents.put("deep_learning", "Глубокое обучение использует нейронные сети...");
        
        Map<String, Integer> results = loader.loadTextBatch(documents, "tech_docs",
            new Object[]{"knowledge", "technology"}, 600);
        
        // Создание индексов
        vectorDB.createIndex("tech_content_index", "content");
        vectorDB.createIndex("doc_type_index", "metadata.documentType");
        
        // Экспорт в JSON
        jsonManager.saveDatabaseToJsonFiles("./json_export");
        
        // Поиск и экспорт результатов
        JSONArray searchResults = jsonManager.searchAndExportToJson("нейронные сети", 10);
        System.out.println("Search results exported: " + searchResults.length() + " items");
        
        // Экспорт статистики
        JSONObject dbStats = jsonManager.exportDatabaseStatsToJson();
        System.out.println("Database statistics: " + dbStats.toString(2));
        
        // Информация об индексах
        Map<String, Object> indexesInfo = vectorDB.getIndexesInfo();
        System.out.println("Indexes info: " + indexesInfo);
        
        // Оптимизация базы знаний
        loader.optimizeKnowledgeBase();
        
        vectorDB.close();
    }
}
```

### Пример 4: Мониторинг и управление памятью

```java
public class MemoryManagementExample {
    public static void main(String[] args) throws Exception {
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434",
            "all-minilm:22m",
            0.7
        );
        
        // Создание базы с ограничением памяти
        BinaryVectorDatabase vectorDB = new BinaryVectorDatabase("./data/memory_managed", chunker, 500 * 1024 * 1024); // 500 MB
        
        KnowledgeLoader loader = new KnowledgeLoader(vectorDB, 
            new KnowledgeConfig("http://localhost:11434", "deepseek-v3.1:671b-cloud", 0.7, true, true));
        
        // Загрузка данных с мониторингом памяти
        loadDataWithMemoryMonitoring(loader, vectorDB);
        
        // Работа с индексами
        vectorDB.createIndex("content_index", "content");
        
        // Проверка использования памяти
        Map<String, Object> memoryStats = vectorDB.getMemoryStats();
        System.out.println("=== Memory Statistics ===");
        memoryStats.forEach((key, value) -> System.out.println(key + ": " + value));
        
        // Информация об индексах
        Map<String, Object> indexesInfo = vectorDB.getIndexesInfo();
        System.out.println("=== Indexes Information ===");
        indexesInfo.forEach((key, value) -> System.out.println(key + ": " + value));
        
        vectorDB.close();
    }
    
    private static void loadDataWithMemoryMonitoring(KnowledgeLoader loader, BinaryVectorDatabase vectorDB) throws Exception {
        // Загрузка данных порциями с проверкой памяти
        String[] documents = {
            "Документ о машинном обучении...",
            "Документ об искусственном интеллекте...",
            // ... больше документов
        };
        
        for (int i = 0; i < documents.length; i++) {
            loader.loadText(documents[i], "doc_" + i,
                new Object[]{"documents", "batch1"}, 500, "document_" + i);
            
            // Проверка памяти каждые 10 документов
            if (i % 10 == 0) {
                Map<String, Object> stats = vectorDB.getMemoryStats();
                long usedMB = (Long) stats.get("estimatedUsageMB");
                long maxMB = (Long) stats.get("maxMemoryMB");
                System.out.printf("Loaded %d documents, memory: %d/%d MB (%.1f%%)%n",
                    i + 1, usedMB, maxMB, (usedMB * 100.0 / maxMB));
                
                if (usedMB > maxMB * 0.8) {
                    System.out.println("Memory usage high, optimizing...");
                    loader.optimizeKnowledgeBase();
                }
            }
        }
    }
}
```

## Конфигурация

### Настройки SemanticChunker

```java
SemanticChunker chunker = new SemanticChunker(
    ollamaUrl,     // URL сервера Ollama
    model,         // Модель для эмбеддингов
    threshold      // Порог схожести (0.0 - 1.0)
);
```

### Настройки KnowledgeConfig

```java
KnowledgeConfig config = new KnowledgeConfig(
    ollamaUrl,           // URL сервера Ollama
    model,               // Модель для генерации
    similarityThreshold, // Порог схожести
    saveHistory,         // Сохранение истории
    enabled              // Включение функциональности
);
```

### Настройки памяти BinaryVectorDatabase

```java
// Различные варианты создания базы данных
BinaryVectorDatabase db1 = new BinaryVectorDatabase("./data", chunker); // 500 MB по умолчанию
BinaryVectorDatabase db2 = new BinaryVectorDatabase("./data", chunker, 1024 * 1024 * 1024); // 1 GB
BinaryVectorDatabase db3 = new BinaryVectorDatabase("./data", chunker, 2L * 1024 * 1024 * 1024); // 2 GB
```

## Лучшие практики

### 1. Оптимальные параметры чанкинга

```java
public class ChunkingOptimization {
    public static void optimizeChunking(KnowledgeLoader loader, String contentType) {
        switch (contentType) {
            case "technical":
                loader.setSimilarityThreshold(0.8); // Высокая точность
                break;
            case "legal":
                loader.setSimilarityThreshold(0.9); // Максимальная точность  
                break;
            case "creative":
                loader.setSimilarityThreshold(0.6); // Широкий охват
                break;
            default:
                loader.setSimilarityThreshold(0.7); // Баланс
        }
    }
}
```

### 2. Эффективное использование памяти

```java
public class MemoryManagement {
    public static void manageMemory(BinaryVectorDatabase vectorDB) {
        // Регулярное сохранение базы данных
        vectorDB.saveDatabase();
        
        // Мониторинг размера базы
        Map<String, Object> memoryStats = vectorDB.getMemoryStats();
        long usedMB = (Long) memoryStats.get("estimatedUsageMB");
        long maxMB = (Long) memoryStats.get("maxMemoryMB");
        
        if (usedMB > maxMB * 0.8) {
            System.out.println("High memory usage: " + usedMB + "/" + maxMB + " MB");
            // Удаление неиспользуемых индексов
            vectorDB.dropIndex("old_index");
        }
        
        // Регулярная оптимизация
        if (usedMB > maxMB * 0.9) {
            System.out.println("Performing memory optimization...");
            Runtime.getRuntime().gc();
        }
    }
}
```

### 3. Оптимизация поисковых запросов

```java
public class SearchOptimization {
    public static void optimizeSearch(OllamaKnowledgeClient client, BinaryVectorDatabase db, String queryType) {
        if (queryType.equals("technical")) {
            client.setSimilarityThreshold(0.8);
            client.setMaxContextResults(3);
            // Использование специализированного индекса
            db.createIndex("tech_content_index", "content");
        } else if (queryType.equals("general")) {
            client.setSimilarityThreshold(0.6);
            client.setMaxContextResults(5);
        }
    }
}
```

### 4. Работа с индексами

```java
public class IndexManagement {
    public static void setupOptimalIndexes(BinaryVectorDatabase db) {
        // Создание основных индексов
        db.createIndex("content_index", "content");
        db.createIndex("document_index", "metadata.documentId");
        db.createIndex("timestamp_index", "metadata.timestamp");
        
        // Мониторинг эффективности индексов
        Map<String, Object> indexesInfo = db.getIndexesInfo();
        for (Map.Entry<String, Object> entry : indexesInfo.entrySet()) {
            String indexName = entry.getKey();
            Map<String, Object> info = (Map<String, Object>) entry.getValue();
            long size = (Long) info.get("size");
            long memoryKB = (Long) info.get("estimatedMemoryKB");
            
            System.out.printf("Index %s: %d entries, %d KB%n", indexName, size, memoryKB);
            
            // Удаление неэффективных индексов
            if (size < 100 && memoryKB > 1024) {
                System.out.println("Removing inefficient index: " + indexName);
                db.dropIndex(indexName);
            }
        }
    }
}
```

### 5. JSON экспорт и импорт

```java
public class JsonBestPractices {
    public static void exportWithMetadata(BinaryVectorDBJsonManager jsonManager, BinaryVectorDatabase db) {
        // Экспорт с фильтрацией
        Map<String, Object> filters = new HashMap<>();
        filters.put("minTimestamp", System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)); // 30 дней
        filters.put("documentId", "important_docs");
        
        JSONArray filteredData = jsonManager.filterAndExportVectorData(filters);
        
        // Сохранение с метаданными
        JSONObject exportPackage = new JSONObject();
        exportPackage.put("data", filteredData);
        exportPackage.put("metadata", jsonManager.exportDatabaseStatsToJson());
        exportPackage.put("exportInfo", new JSONObject()
            .put("exportedBy", "system")
            .put("exportDate", new Date().toString())
            .put("version", "1.0"));
        
        // Сохранение в файл
        try {
            Files.write(Paths.get("./export/backup.json"), exportPackage.toString(2).getBytes());
        } catch (IOException e) {
            System.err.println("Export failed: " + e.getMessage());
        }
    }
}
```

## Устранение неполадок

### Проверка подключений

```java
OllamaKnowledgeClient client = new OllamaKnowledgeClient(vectorDB, config);
Map<String, Boolean> testResults = client.testConnections();
client.printConnectionTest();
```

### Диагностика проблем с памятью

```java
BinaryVectorDatabase db = new BinaryVectorDatabase("./data", chunker, 1024 * 1024 * 1024);
Map<String, Object> memoryStats = db.getMemoryStats();

System.out.println("=== Memory Diagnostics ===");
System.out.println("Estimated usage: " + memoryStats.get("estimatedUsageMB") + " MB");
System.out.println("Max memory: " + memoryStats.get("maxMemoryMB") + " MB");
System.out.println("JVM used: " + memoryStats.get("jvmUsedMB") + " MB");
System.out.println("JVM free: " + memoryStats.get("jvmFreeMB") + " MB");

if ((Long) memoryStats.get("estimatedUsageMB") > (Long) memoryStats.get("maxMemoryMB") * 0.9) {
    System.out.println("WARNING: Memory usage critically high!");
    System.out.println("Recommended actions:");
    System.out.println("1. Increase memory limit in constructor");
    System.out.println("2. Remove unused indexes");
    System.out.println("3. Archive old data");
    System.out.println("4. Call db.close() and recreate with higher limit");
}
```

### Диагностика индексов

```java
Map<String, Object> indexesInfo = db.getIndexesInfo();
if (indexesInfo.isEmpty()) {
    System.out.println("No indexes found. Consider creating indexes for better performance.");
} else {
    System.out.println("=== Indexes Diagnostics ===");
    for (Map.Entry<String, Object> entry : indexesInfo.entrySet()) {
        String indexName = entry.getKey();
        Map<String, Object> info = (Map<String, Object>) entry.getValue();
        System.out.printf("%s: %d entries, %d KB%n", 
            indexName, info.get("size"), info.get("estimatedMemoryKB"));
    }
}
```

### Обработка ошибок эмбеддингов

```java
try {
    float[] embedding = semanticChunker.getEmbedding(text);
} catch (Exception e) {
    System.err.println("Error getting embedding: " + e.getMessage());
    // Fallback стратегии
    System.out.println("Using fallback embedding strategy...");
    // Можно использовать локальные эмбеддинги или кэшированные значения
}
```

### Восстановление из JSON бэкапа

```java
public class RecoveryExample {
    public static void recoverFromJsonBackup(String backupPath, String newDatabasePath) throws Exception {
        SemanticChunker chunker = new SemanticChunker("http://localhost:11434", "all-minilm:22m", 0.7);
        BinaryVectorDatabase newDb = new BinaryVectorDatabase(newDatabasePath, chunker);
        BinaryVectorDBJsonManager jsonManager = new BinaryVectorDBJsonManager(newDb);
        
        System.out.println("Starting recovery from backup: " + backupPath);
        
        // Загрузка из JSON файлов
        jsonManager.loadDatabaseFromJsonFiles(backupPath);
        
        // Создание индексов для восстановленной базы
        newDb.createIndex("content_index", "content");
        newDb.createIndex("recovery_index", "metadata.recovered");
        
        System.out.println("Recovery completed successfully!");
        System.out.println("Restored vectors: " + newDb.getVectorCount());
        System.out.println("Restored tree nodes: " + newDb.getTreeNodeCount());
        
        newDb.close();
    }
}
```

