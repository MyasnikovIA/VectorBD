# VectorDB - Полная документация

## Оглавление
1. [Введение](#введение)
2. [Архитектура системы](#архитектура-системы)
3. [Быстрый старт](#быстрый-старт)
4. [Детальное описание компонентов](#детальное-описание-компонентов)
5. [Примеры использования](#примеры-использования)
6. [Конфигурация](#конфигурация)
7. [Лучшие практики](#лучшие-практики)
8. [Устранение неполадок](#устранение-неполадок)

## Введение

VectorDB - это Java-библиотека для создания и управления векторными базами данных с поддержкой семантического поиска и Retrieval-Augmented Generation (RAG). Система предназначена для работы с текстовыми данными, их векторным представлением и интеллектуальным поиском.

### Ключевые особенности
- **Семантический поиск** - поиск документов по смысловому сходству
- **Интеллектуальное чанкинг** - автоматическое разбиение текста на смысловые фрагменты
- **Интеграция с LLM** - работа с локальными моделями через Ollama
- **Гибкое хранение** - древовидная структура для сложных данных
- **RAG-готовность** - готовые компоненты для RAG-приложений

## Архитектура системы

### Основные компоненты

```
VectorDB Architecture:
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Knowledge     │    │   Vector Database│    │   Ollama        │
│    Loader       │───▶│     Core         │───▶│   Integration   │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  Text Chunking  │    │  Vector Index    │    │  Prompt Generation│
│  & Embedding    │    │  & Search        │    │  & RAG          │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

### Поток данных
1. **Загрузка текста** → SemanticChunker → Векторные эмбеддинги
2. **Сохранение** → VectorDatabase + TreeNode
3. **Поиск** → VectorIndex → Релевантные документы
4. **Генерация** → PromptGenerator → Ollama → Ответ

## Быстрый старт

### Предварительные требования

```bash
# Установка Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# Загрузка моделей
ollama pull llama3.2
ollama pull all-minilm:22m
ollama pull deepseek-v3.1:671b-cloud

# Проверка установки
ollama list
```

### Базовая настройка проекта

#### Maven зависимость
```xml
<dependencies>
    <dependency>
        <groupId>org.json</groupId>
        <artifactId>json</artifactId>
        <version>20231013</version>
    </dependency>
</dependencies>
```

#### Минимальный рабочий пример
```java
package com.example.vectordb;

import ru.miacomsoft.vectordb.core.SemanticChunker;
import ru.miacomsoft.vectordb.core.VectorDatabase;
import ru.miacomsoft.vectordb.core.VectorSearchResult;
import java.util.List;

public class QuickStart {
    public static void main(String[] args) {
        try {
            // 1. Инициализация компонентов
            SemanticChunker chunker = new SemanticChunker(
                "http://localhost:11434",
                "all-minilm:22m",
                0.8
            );
            
            VectorDatabase vectorDB = new VectorDatabase("./data/mydb", chunker);
            
            // 2. Загрузка данных
            String document = """
                Искусственный интеллект - это область компьютерных наук, 
                занимающаяся созданием машин, способных выполнять задачи, 
                требующие человеческого интеллекта. Машинное обучение является 
                подразделом ИИ и фокусируется на алгоритмах, обучающихся на данных.
                """;
                
            vectorDB.storeTextWithChunking(
                document,
                "ai_intro",
                new Object[]{"knowledge", "artificial_intelligence"}
            );
            
            // 3. Поиск
            List<VectorSearchResult> results = vectorDB.similaritySearch(
                "что такое машинное обучение?", 3
            );
            
            // 4. Вывод результатов
            for (VectorSearchResult result : results) {
                System.out.printf("Схожесть: %.4f%n", result.getSimilarity());
                System.out.println("Текст: " + result.getVectorData().getText());
                System.out.println("---");
            }
            
            vectorDB.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Детальное описание компонентов

### 1. SemanticChunker

Класс для семантического разбиения текста на чанки.

#### Конструктор
```java
SemanticChunker chunker = new SemanticChunker(
    "http://localhost:11434",  // URL Ollama
    "all-minilm:22m",          // модель для эмбеддингов
    0.8                        // порог схожести (0.0-1.0)
);
```

#### Основные методы
```java
// Разбиение текста на семантические чанки
List<SemanticChunker.Chunk> chunks = chunker.semanticChunking(
    "Длинный текст для разбиения...", 
    1000  // максимальный размер чанка
);

// Получение эмбеддинга для текста
float[] embedding = chunker.getEmbedding("Текст для векторизации");

// Расчет косинусной схожести
double similarity = chunker.cosineSimilarity(embedding1, embedding2);

// Настройка порога
chunker.setSimilarityThreshold(0.7);
```

#### Пример работы с чанками
```java
List<SemanticChunker.Chunk> chunks = chunker.semanticChunking(text, 500);

for (SemanticChunker.Chunk chunk : chunks) {
    System.out.println("Позиция: " + chunk.getPosition());
    System.out.println("Длина: " + chunk.getLength());
    System.out.println("Текст: " + chunk.getText());
    System.out.println("Эмбеддинг: " + Arrays.toString(chunk.getEmbedding()));
    System.out.println("---");
}
```

### 2. VectorDatabase

Основной класс для работы с векторной базой данных.

#### Инициализация
```java
VectorDatabase vectorDB = new VectorDatabase(
    "./data/mydatabase",  // путь к данным
    semanticChunker       // экземпляр SemanticChunker
);
```

#### Методы хранения данных
```java
// Базовое сохранение текста с автоматическим чанкингом
vectorDB.storeTextWithChunking(
    "Текст для сохранения...",
    "document_001",           // ID документа
    new Object[]{"category", "subcategory"}  // путь в дереве
);

// Прямое сохранение VectorData
VectorData vectorData = new VectorData(
    "chunk_001",
    embedding,               // float[] эмбеддинг
    originalData,           // исходные данные
    "Текст чанка",          // текст
    "path.to.chunk",        // путь узла
    "doc_001",              // ID документа
    0                       // индекс чанка
);
vectorDB.storeVectorData(vectorData);

// Сохранение TreeNode
TreeNode node = new TreeNode("Значение узла");
vectorDB.storeTreeNode("node_001", node, new Object[]{"tree", "node"});
```

#### Методы поиска
```java
// Семантический поиск по тексту
List<VectorSearchResult> semanticResults = vectorDB.similaritySearch(
    "поисковый запрос", 
    10  // лимит результатов
);

// Семантический поиск по вектору
float[] queryVector = chunker.getEmbedding("запрос");
List<VectorSearchResult> vectorResults = vectorDB.similaritySearch(
    queryVector, 
    10
);

// Точный текстовый поиск
List<VectorData> exactResults = vectorDB.exactSearch("ключевое слово");

// Поиск по пути
List<VectorData> pathResults = vectorDB.searchByPath("category.subcategory");
```

#### Управление данными
```java
// Получение данных
VectorData data = vectorDB.getVectorData("chunk_001");
TreeNode node = vectorDB.getTreeNode("node_001");

// Удаление данных
vectorDB.removeVectorData("chunk_001");
vectorDB.removeTreeNode("node_001");

// Статистика
int vectorCount = vectorDB.getVectorCount();
int nodeCount = vectorDB.getTreeNodeCount();

// Сохранение базы
vectorDB.saveDatabase();
vectorDB.close();  // автоматическое сохранение при закрытии
```

### 3. TreeNode

Гибкая древовидная структура для хранения данных.

#### Создание и базовые операции
```java
// Создание дерева
TreeNode root = new TreeNode();

// Установка значений по пути
root.setNode(new Object[]{"users", "john", "profile", "name"}, "John Doe");
root.setNode(new Object[]{"users", "john", "profile", "age"}, 30);
root.setNode(new Object[]{"users", "john", "settings", "theme"}, "dark");

// Получение значений
String name = (String) root.getNode(new Object[]{"users", "john", "profile", "name"});
Integer age = (Integer) root.getNode(new Object[]{"users", "john", "profile", "age"});

// Удаление узлов
root.removeNode(new Object[]{"users", "john", "settings", "theme"});
```

#### Поиск и запросы
```java
// Поиск с глубиной
List<TreeNode.QueryResult> results = root.query(
    new Object[]{"users", "john"},  // путь
    2                              // глубина поиска
);

for (TreeNode.QueryResult result : results) {
    System.out.println("Путь: " + result.getPathString());
    System.out.println("Значение: " + result.getValue());
}

// Поиск значений
List<Map.Entry<List<Object>, Object>> foundValues = root.findValues("John Doe");

// Поиск по шаблону
List<Map.Entry<List<Object>, Object>> patternResults = 
    root.findValuesByPattern("john");
```

#### Расширенные возможности
```java
// Метаданные
root.setMetadata("created", "2024-01-01");
root.setMetadata("version", "1.0");
String created = (String) root.getMetadata("created");

// Сериализация в JSON
String json = root.toJsonString();
TreeNode restored = TreeNode.fromJsonString(json);

// Статистика дерева
int totalNodes = root.countNodes();
int depth = root.getDepth();
int width = root.getWidth();
List<Object> leafValues = root.getLeafValues();

// Визуализация
System.out.println(root.toTreeString());
```

### 4. KnowledgeLoader

Загрузчик текстовых знаний в векторную базу.

#### Инициализация
```java
KnowledgeConfig config = new KnowledgeConfig(
    "http://localhost:11434",
    "llama3.2",
    0.8,
    true,
    true
);

KnowledgeLoader loader = new KnowledgeLoader(vectorDB, config);
```

#### Загрузка данных
```java
// Загрузка текстовой строки
int chunksCount = loader.loadText(
    "Длинный текст с знаниями...",
    "knowledge_base",
    new Object[]{"domain", "topic"},
    500,      // максимальный размер чанка
    "source"  // имя источника
);

// Загрузка файла
int fileChunks = loader.loadTextFile(
    "/path/to/document.txt",
    "document_001",
    new Object[]{"documents", "ai"},
    500
);

// Загрузка директории
String[] extensions = {".txt", ".md", ".json"};
int totalChunks = loader.loadTextDirectory(
    "/path/to/documents/",
    "docs",
    new Object[]{"library"},
    500,
    extensions
);
```

#### Управление загрузкой
```java
// Настройка порога схожести
loader.setSimilarityThreshold(0.9);
double currentThreshold = loader.getCurrentSimilarityThreshold();

// Статистика
loader.printKnowledgeStats();

// Конфигурация
Map<String, Object> config = loader.getKnowledgeConfig();
String chunkerConfig = loader.getSemanticChunkerConfig();
```

### 5. OllamaKnowledgeClient

Клиент для работы с Ollama и RAG-функциональностью.

#### Инициализация
```java
OllamaKnowledgeClient client = new OllamaKnowledgeClient(vectorDB, config);

// Кастомная настройка
client.setDefaultModel("deepseek-v3.1:671b-cloud");
client.setSimilarityThreshold(0.7);
client.setMaxContextResults(5);
```

#### Основные методы
```java
// Поиск релевантных фактов
List<String> facts = client.findRelevantFacts("запрос", 5);

// Базовая генерация ответа
String response = client.generateResponse("Вопрос без контекста");

// Генерация с использованием знаний (RAG)
String ragResponse = client.generateResponseWithKnowledge(
    "Что такое машинное обучение?"
);

// Потоковая генерация
Iterator<String> stream = client.generateResponseStream("запрос");
while (stream.hasNext()) {
    System.out.print(stream.next());
}
```

#### Интерактивный режим
```java
// Запуск интерактивного чата
client.startInteractiveChat();

// Пример сессии:
// You: Что такое искусственный интеллект?
// AI: [генерирует ответ на основе знаний из базы]
// You: Какие есть типы машинного обучения?
// AI: [использует найденные релевантные документы]
```

### 6. PromptGenerator

Генератор промптов для AI с использованием семантического поиска.

#### Использование
```java
PromptGenerator promptGenerator = new PromptGenerator(vectorDB, config);

// Контекстный промпт
String contextPrompt = promptGenerator.createContextPrompt(
    "Вопрос пользователя",
    3,    // максимальное количество результатов на чанк
    0.7   // порог схожести
);

// Генерация вопросов
String questionPrompt = promptGenerator.createQuestionGenerationPrompt(
    "тема для вопросов",
    5,    // количество вопросов
    0.6   // порог схожести
);

// Суммаризация
String summaryPrompt = promptGenerator.createSummarizationPrompt(
    "тема для суммаризации",
    4,    // максимальное количество контекстных элементов
    0.65  // порог схожести
);
```

## Примеры использования

### Пример 1: Построение базы знаний компании

```java
public class CompanyKnowledgeBase {
    private VectorDatabase vectorDB;
    private KnowledgeLoader loader;
    
    public CompanyKnowledgeBase() throws Exception {
        KnowledgeConfig config = new KnowledgeConfig(
            "http://localhost:11434", "llama3.2", 0.8, true, true
        );
        
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.8
        );
        
        this.vectorDB = new VectorDatabase("./data/company_kb", chunker);
        this.loader = new KnowledgeLoader(vectorDB, config);
    }
    
    public void loadCompanyDocuments() throws Exception {
        // Документация по продуктам
        String productDocs = """
            Наш продукт AI Assistant помогает автоматизировать 
            обработку клиентских запросов. Основные функции:
            - Анализ естественного языка
            - Интеграция с CRM системами
            - Автоматическая классификация запросов
            - Генерация персонализированных ответов
            
            Технические требования:
            - Java 11 или выше
            - 4GB RAM минимум
            - Поддержка Docker
            """;
            
        loader.loadText(productDocs, "product_docs", 
            new Object[]{"docs", "products", "ai_assistant"}, 500, "internal");
        
        // Процедуры поддержки
        String supportProcedures = """
            Процедура эскалации критических инцидентов:
            1. Первичная оценка приоритета - 15 минут
            2. Уведомление тимлида - 5 минут
            3. Создание задачи в JIRA - немедленно
            4. Коммуникация с клиентом - каждые 2 часа
            
            SLA для различных приоритетов:
            - P0: 1 час response time
            - P1: 4 часа response time  
            - P2: 1 рабочий день
            - P3: 3 рабочих дня
            """;
            
        loader.loadText(supportProcedures, "support_procedures",
            new Object[]{"docs", "support", "procedures"}, 400, "internal");
    }
    
    public String askQuestion(String question) throws Exception {
        OllamaKnowledgeClient client = new OllamaKnowledgeClient(vectorDB, 
            new KnowledgeConfig("http://localhost:11434", "llama3.2", 0.8, true, true));
        
        return client.generateResponseWithKnowledge(question);
    }
    
    public static void main(String[] args) throws Exception {
        CompanyKnowledgeBase kb = new CompanyKnowledgeBase();
        kb.loadCompanyDocuments();
        
        // Примеры вопросов
        System.out.println(kb.askQuestion("Какие SLA для приоритета P1?"));
        System.out.println(kb.askQuestion("Какие функции у AI Assistant?"));
        System.out.println(kb.askQuestion("Как происходит эскалация инцидентов?"));
        
        kb.vectorDB.close();
    }
}
```

### Пример 2: Образовательная платформа

```java
public class EducationalPlatform {
    private VectorDatabase vectorDB;
    private PromptGenerator promptGenerator;
    
    public EducationalPlatform() throws Exception {
        KnowledgeConfig config = new KnowledgeConfig(
            "http://localhost:11434", "llama3.2", 0.7, true, true
        );
        
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.7
        );
        
        this.vectorDB = new VectorDatabase("./data/education", chunker);
        this.promptGenerator = new PromptGenerator(vectorDB, config);
        
        loadEducationalContent();
    }
    
    private void loadEducationalContent() throws Exception {
        KnowledgeLoader loader = new KnowledgeLoader(vectorDB, 
            new KnowledgeConfig("http://localhost:11434", "llama3.2", 0.7, true, true));
        
        // Материалы по программированию
        String programmingContent = """
            Объектно-ориентированное программирование (ООП) - это парадигма программирования, 
            в которой программа представляется в виде совокупности объектов, 
            каждый из которых является экземпляром определенного класса.
            
            Основные принципы ООП:
            1. Инкапсуляция - сокрытие внутренней реализации
            2. Наследование - создание новых классов на основе существующих
            3. Полиморфизм - возможность объектов с одинаковой спецификацией 
               иметь различную реализацию
            4. Абстракция - выделение существенных характеристик объекта
            
            Класс - это шаблон для создания объектов, определяющий их структуру и поведение.
            Объект - экземпляр класса, содержащий данные и методы для работы с ними.
            """;
            
        loader.loadText(programmingContent, "oop_basics",
            new Object[]{"courses", "programming", "oop"}, 300, "educational");
            
        // Материалы по алгоритмам
        String algorithmsContent = """
            Алгоритмы сортировки - методы упорядочивания элементов в списке.
            
            Быстрая сортировка (QuickSort):
            - Сложность: O(n log n) в среднем случае, O(n²) в худшем
            - Стратегия: "разделяй и властвуй"
            - Выбор опорного элемента критически важен для производительности
            
            Сортировка слиянием (MergeSort):
            - Сложность: O(n log n) в любом случае
            - Требует дополнительной памяти O(n)
            - Стабильная сортировка
            """;
            
        loader.loadText(algorithmsContent, "sorting_algorithms",
            new Object[]{"courses", "algorithms", "sorting"}, 350, "educational");
    }
    
    public void generateStudyQuestions(String topic, int count) throws Exception {
        String prompt = promptGenerator.createQuestionGenerationPrompt(
            topic, count, 0.6
        );
        
        System.out.println("=== ВОПРОСЫ ДЛЯ ИЗУЧЕНИЯ ===");
        System.out.println("Тема: " + topic);
        System.out.println("Количество: " + count);
        System.out.println("\n" + prompt);
    }
    
    public void createStudyGuide(String topic) throws Exception {
        String prompt = promptGenerator.createSummarizationPrompt(
            topic, 5, 0.65
        );
        
        System.out.println("=== УЧЕБНОЕ ПОСОБИЕ ===");
        System.out.println("Тема: " + topic);
        System.out.println("\n" + prompt);
    }
    
    public static void main(String[] args) throws Exception {
        EducationalPlatform platform = new EducationalPlatform();
        
        // Генерация вопросов для самопроверки
        platform.generateStudyQuestions("ООП принципы", 5);
        System.out.println("\n" + "=" .repeat(50) + "\n");
        
        // Создание учебного пособия
        platform.createStudyGuide("алгоритмы сортировки");
        
        platform.vectorDB.close();
    }
}
```

### Пример 3: Техническая поддержка с RAG

```java
public class TechnicalSupportBot {
    private OllamaKnowledgeClient client;
    private VectorDatabase vectorDB;
    
    public TechnicalSupportBot() throws Exception {
        KnowledgeConfig config = new KnowledgeConfig(
            "http://localhost:11434", "deepseek-v3.1:671b-cloud", 0.75, true, true
        );
        
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.75
        );
        
        this.vectorDB = new VectorDatabase("./data/support_bot", chunker);
        this.client = new OllamaKnowledgeClient(vectorDB, config);
        
        loadKnowledgeBase();
    }
    
    private void loadKnowledgeBase() throws Exception {
        KnowledgeLoader loader = new KnowledgeLoader(vectorDB,
            new KnowledgeConfig("http://localhost:11434", "deepseek-v3.1:671b-cloud", 0.75, true, true));
        
        // База знаний по устранению неполадок
        String troubleshootingKB = """
            Распространенные проблемы и решения:
            
            Проблема: Приложение не запускается
            Решение: 
            1. Проверить установку Java: java -version
            2. Убедиться в наличии необходимых прав
            3. Проверить файл конфигурации на ошибки
            4. Просмотреть логи приложения
            
            Проблема: Высокая загрузка памяти
            Решение:
            1. Проверить настройки JVM (-Xmx, -Xms)
            2. Проанализировать дамп памяти
            3. Проверить наличие утечек памяти
            4. Оптимизировать использование кэша
            
            Проблема: Медленная работа базы данных
            Решение:
            1. Проверить индексы таблиц
            2. Оптимизировать запросы
            3. Проверить настройки соединения
            4. Мониторить системные ресурсы
            """;
            
        loader.loadText(troubleshootingKB, "troubleshooting_guide",
            new Object[]{"support", "troubleshooting"}, 400, "support_kb");
            
        // Информация о продукте
        String productInfo = """
            Наш продукт SuperApp версии 2.1.0
            
            Системные требования:
            - Операционная система: Windows 10+, Linux, MacOS
            - Java: версия 11 или выше
            - Память: минимум 2GB RAM, рекомендуется 4GB
            - Диск: 500MB свободного места
            
            Поддерживаемые базы данных:
            - PostgreSQL 12+
            - MySQL 8.0+
            - MongoDB 4.4+
            
            Логирование:
            - Основные логи: /var/log/superapp/app.log
            - Логи ошибок: /var/log/superapp/error.log
            - Уровень логирования настраивается в application.properties
            """;
            
        loader.loadText(productInfo, "product_info",
            new Object[]{"product", "information"}, 500, "product_docs");
    }
    
    public void handleUserQuery(String userQuery) {
        try {
            System.out.println("👤 Пользователь: " + userQuery);
            System.out.print("🤖 Поддержка: ");
            
            // Потоковый ответ для лучшего UX
            Iterator<String> responseStream = client.generateResponseStream(userQuery);
            
            while (responseStream.hasNext()) {
                System.out.print(responseStream.next());
            }
            System.out.println("\n");
            
        } catch (Exception e) {
            System.out.println("❌ Извините, произошла ошибка: " + e.getMessage());
        }
    }
    
    public void startSupportSession() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("🎯 Техническая поддержка готова к работе!");
        System.out.println("Введите ваш вопрос (или 'выход' для завершения):\n");
        
        while (true) {
            System.out.print("> ");
            String query = scanner.nextLine().trim();
            
            if (query.equalsIgnoreCase("выход") || 
                query.equalsIgnoreCase("exit") || 
                query.equalsIgnoreCase("quit")) {
                break;
            }
            
            if (!query.isEmpty()) {
                handleUserQuery(query);
            }
        }
        
        scanner.close();
        System.out.println("✅ Сессия поддержки завершена.");
    }
    
    public static void main(String[] args) throws Exception {
        TechnicalSupportBot bot = new TechnicalSupportBot();
        bot.startSupportSession();
        bot.vectorDB.close();
    }
}
```

## Конфигурация

### Оптимальные настройки для различных сценариев

#### Сценарий 1: Высокая точность (техническая документация)
```java
KnowledgeConfig highPrecisionConfig = new KnowledgeConfig(
    "http://localhost:11434",
    "llama3.2",      // точная модель
    0.85,            // высокий порог схожести
    true,
    true
);

SemanticChunker highPrecisionChunker = new SemanticChunker(
    "http://localhost:11434",
    "all-minilm:22m",
    0.85
);
```

#### Сценарий 2: Баланс скорости и качества (чат-бот)
```java
KnowledgeConfig balancedConfig = new KnowledgeConfig(
    "http://localhost:11434", 
    "deepseek-v3.1:671b-cloud",  // сбалансированная модель
    0.7,                         // средний порог
    true,
    true
);

SemanticChunker balancedChunker = new SemanticChunker(
    "http://localhost:11434",
    "all-minilm:22m", 
    0.7
);
```

#### Сценарий 3: Максимальная скорость (реальное время)
```java
KnowledgeConfig speedConfig = new KnowledgeConfig(
    "http://localhost:11434",
    "llama3.2",      // быстрая модель
    0.6,             // низкий порог для большего охвата
    false,           // без сохранения истории
    true
);

SemanticChunker speedChunker = new SemanticChunker(
    "http://localhost:11434",
    "all-minilm:22m",
    0.6
);
```

### Настройка размера чанков

```java
// Маленькие чанки (для точного поиска)
int smallChunkSize = 200;  // символов

// Средние чанки (универсальные)
int mediumChunkSize = 500; // символов

// Большие чанки (для контекстных ответов)
int largeChunkSize = 1000; // символов
```

## Лучшие практики

### 1. Оптимизация производительности

```java
// Используйте пулы соединений для HTTP клиентов
HttpClient httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .executor(Executors.newFixedThreadPool(5))
    .build();

// Настройте размеры чанков в зависимости от использования
public class OptimizedVectorDB {
    private static final int SEARCH_CHUNK_SIZE = 300;
    private static final int STORAGE_CHUNK_SIZE = 800;
    
    public void optimizeForSearch() {
        semanticChunker.setSimilarityThreshold(0.8);
    }
    
    public void optimizeForStorage() {
        semanticChunker.setSimilarityThreshold(0.6);
    }
}
```

### 2. Управление памятью

```java
// Регулярная очистка кэшей
Runtime.getRuntime().gc();

// Мониторинг использования памяти
public class MemoryMonitor {
    public static void printMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        
        System.out.printf("Использование памяти: %.2fMB / %.2fMB%n",
            usedMemory / (1024.0 * 1024.0),
            maxMemory / (1024.0 * 1024.0));
    }
}
```

### 3. Обработка ошибок

```java
public class RobustVectorDB {
    private VectorDatabase vectorDB;
    
    public void safeStoreText(String text, String docId, Object[] path) {
        try {
            vectorDB.storeTextWithChunking(text, docId, path);
        } catch (Exception e) {
            System.err.println("Ошибка сохранения документа " + docId + ": " + e.getMessage());
            // Логирование и восстановление
            logError(e);
            attemptRecovery(docId, path);
        }
    }
    
    public List<VectorSearchResult> safeSearch(String query, int limit) {
        try {
            return vectorDB.similaritySearch(query, limit);
        } catch (Exception e) {
            System.err.println("Ошибка поиска: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
```

## Устранение неполадок

### Общие проблемы и решения

#### Проблема 1: Ollama недоступен
**Симптомы**:
- `ConnectException` при создании SemanticChunker
- Таймауты при получении эмбеддингов

**Решение**:
```bash
# Проверка статуса Ollama
curl http://localhost:11434/api/tags

# Перезапуск Ollama
ollama serve
# или
sudo systemctl restart ollama
```

#### Проблема 2: Нехватка памяти
**Симптомы**:
- `OutOfMemoryError`
- Медленная работа при больших объемах данных

**Решение**:
```java
// Увеличьте heap size
// java -Xmx4G -jar your-app.jar

// Оптимизируйте размеры чанков
chunker.setSimilarityThreshold(0.9);  // меньше чанков
vectorDB.saveDatabase();  // регулярное сохранение
```

#### Проблема 3: Низкая точность поиска
**Симптомы**:
- Нерелевантные результаты поиска
- Высокий порог схожести, но плохие результаты

**Решение**:
```java
// Настройте порог схожести
chunker.setSimilarityThreshold(0.7);  // экспериментируйте

// Используйте другую модель эмбеддингов
SemanticChunker newChunker = new SemanticChunker(
    "http://localhost:11434",
    "nomic-embed-text",  // альтернативная модель
    0.7
);

// Увеличьте размер чанков
int largerChunkSize = 800;
```

#### Проблема 4: Медленная генерация ответов
**Симптомы**:
- Долгая обработка запросов
- Таймауты при работе с Ollama

**Решение**:
```java
// Используйте более быстрые модели
client.setDefaultModel("llama3.2");  // вместо больших моделей

// Уменьшите количество контекстных результатов
client.setMaxContextResults(2);

// Используйте потоковую генерацию
Iterator<String> stream = client.generateResponseStream(query);
```

### Диагностика и отладка

```java
public class Diagnostics {
    public static void checkSystemHealth(VectorDatabase vectorDB, 
                                       SemanticChunker chunker) {
        try {
            // Проверка базы данных
            System.out.println("Векторов в базе: " + vectorDB.getVectorCount());
            System.out.println("Узлов в дереве: " + vectorDB.getTreeNodeCount());
            
            // Проверка чанкера
            System.out.println("Конфигурация чанкера: " + chunker.getConfigInfo());
            
            // Проверка эмбеддингов
            float[] testEmbedding = chunker.getEmbedding("тест");
            System.out.println("Размерность эмбеддинга: " + testEmbedding.length);
            
            // Проверка поиска
            List<VectorSearchResult> results = vectorDB.similaritySearch("тест", 1);
            System.out.println("Поиск работает: " + !results.isEmpty());
            
        } catch (Exception e) {
            System.err.println("Ошибка диагностики: " + e.getMessage());
        }
    }
}
```

### Мониторинг производительности

```java
public class PerformanceMonitor {
    private long startTime;
    
    public void startTiming() {
        startTime = System.currentTimeMillis();
    }
    
    public void logOperation(String operation) {
        long duration = System.currentTimeMillis() - startTime;
        System.out.printf("Операция '%s' заняла: %d мс%n", operation, duration);
    }
    
    public static void monitorSearch(VectorDatabase vectorDB, String query) {
        PerformanceMonitor monitor = new PerformanceMonitor();
        monitor.startTiming();
        
        List<VectorSearchResult> results = vectorDB.similaritySearch(query, 5);
        
        monitor.logOperation("семантический поиск");
        System.out.println("Найдено результатов: " + results.size());
    }
}
```

Эта документация покрывает все основные аспекты работы с VectorDB системой. Для дополнительной помощи обращайтесь к демонстрационным примерам в пакете `ru.miacomsoft.vectordb.demo`.