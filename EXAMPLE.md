# VectorDB - Примеры использования

## Оглавление
1. [Быстрый старт](#быстрый-старт)
2. [Базовые операции](#базовые-операции)
3. [Работа с семантическим поиском](#работа-с-семантическим-поиском)
4. [Знания и RAG](#знания-и-rag)
5. [Продвинутые сценарии](#продвинутые-сценарии)
6. [Интеграционные примеры](#интеграционные-примеры)
7. [Утилиты и инструменты](#утилиты-и-инструменты)
8. [Новый функционал](#новый-функционал)

## Быстрый старт

### Минимальный рабочий пример

```java
import ru.miacomsoft.vectordb.core.*;
import ru.miacomsoft.vectordb.knowledge.*;

public class QuickStartExample {
    public static void main(String[] args) {
        try {
            // 1. Инициализация компонентов
            SemanticChunker chunker = new SemanticChunker(
                "http://localhost:11434",
                "all-minilm:22m",
                0.8
            );
            
            BinaryVectorDatabase vectorDB = new BinaryVectorDatabase("./data/quickstart", chunker);
            
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
                new Object[]{"knowledge", "ai"}
            );
            
            // 3. Семантический поиск
            List<VectorSearchResult> results = vectorDB.similaritySearch(
                "что такое машинное обучение?", 3
            );
            
            // 4. Вывод результатов
            System.out.println("=== Результаты поиска ===");
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

### Настройка окружения

```java
public class EnvironmentSetup {
    public static void main(String[] args) {
        // Проверка доступности Ollama
        OllamaStreamClient ollamaClient = new OllamaStreamClient("http://localhost:11434");
        
        if (ollamaClient.isServerAvailable()) {
            System.out.println("✅ Ollama сервер доступен");
            
            // Получение списка моделей
            List<String> models = ollamaClient.getAvailableModels();
            System.out.println("Доступные модели:");
            for (String model : models) {
                System.out.println("  - " + model);
            }
        } else {
            System.out.println("❌ Ollama сервер недоступен");
            System.out.println("Запустите: ollama serve");
        }
    }
}
```

## Базовые операции

### Работа с BinaryVectorDatabase

```java
public class BinaryVectorDBOperations {
    public static void main(String[] args) throws Exception {
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.8
        );
        
        // Создание базы с 1 GB памяти
        BinaryVectorDatabase vectorDB = new BinaryVectorDatabase(
            "./data/operations", chunker, 1024 * 1024 * 1024
        );
        
        // Сохранение различных типов данных
        String[] documents = {
            "Java - объектно-ориентированный язык программирования",
            "Python популярен для машинного обучения и анализа данных",
            "Базы данных хранят структурированную информацию",
            "Векторный поиск находит семантически похожие документы"
        };
        
        for (int i = 0; i < documents.length; i++) {
            vectorDB.storeTextWithChunking(
                documents[i],
                "doc_" + i,
                new Object[]{"lang", "doc_" + i}
            );
        }
        
        // Различные типы поиска
        System.out.println("=== Семантический поиск ===");
        List<VectorSearchResult> semanticResults = vectorDB.similaritySearch(
            "программирование и данные", 2
        );
        semanticResults.forEach(r -> 
            System.out.printf("(%.3f) %s%n", r.getSimilarity(), r.getVectorData().getText())
        );
        
        System.out.println("\n=== Точный поиск ===");
        List<BinaryVectorData> exactResults = vectorDB.exactSearch("Java");
        exactResults.forEach(data -> 
            System.out.println("Найдено: " + data.getText())
        );
        
        System.out.println("\n=== Поиск по пути ===");
        List<BinaryVectorData> pathResults = vectorDB.searchByPath("lang");
        pathResults.forEach(data -> 
            System.out.println("Путь: " + data.getNodePath() + " - " + data.getText())
        );
        
        // Статистика
        System.out.println("\n=== Статистика ===");
        System.out.println("Векторов: " + vectorDB.getVectorCount());
        System.out.println("Узлов: " + vectorDB.getTreeNodeCount());
        
        // Статистика памяти
        Map<String, Object> memoryStats = vectorDB.getMemoryStats();
        System.out.println("Использование памяти: " + memoryStats.get("estimatedUsageMB") + " MB");
        
        vectorDB.close();
    }
}
```

### Работа с BinaryTreeNode

```java
public class BinaryTreeNodeExamples {
    public static void main(String[] args) {
        BinaryTreeNode root = new BinaryTreeNode("root");
        BinaryTreeNode left = new BinaryTreeNode("left child");
        BinaryTreeNode right = new BinaryTreeNode("right child");
        
        // Построение дерева
        root.setLeft(left);
        root.setRight(right);
        
        // Установка метаданных
        root.setMetadata("author", "System");
        root.setMetadata("version", "1.0");
        left.setMetadata("type", "branch");
        right.setMetadata("type", "branch");
        
        // Сериализация дерева
        try {
            byte[] serializedData = root.serialize();
            System.out.println("Дерево сериализовано, размер: " + serializedData.length + " bytes");
            
            // Десериализация
            BinaryTreeNode restoredRoot = BinaryTreeNode.deserialize(serializedData);
            System.out.println("Дерево восстановлено: " + restoredRoot.getContent());
            System.out.println("Метаданные: " + restoredRoot.getMetadata());
            
        } catch (IOException e) {
            System.err.println("Ошибка сериализации: " + e.getMessage());
        }
        
        // Работа с бинарной структурой
        System.out.println("\n=== Обход дерева ===");
        traverseTree(root, 0);
    }
    
    private static void traverseTree(BinaryTreeNode node, int level) {
        if (node == null) return;
        
        String indent = "  ".repeat(level);
        System.out.println(indent + "└── " + node.getContent());
        
        if (node.getLeft() != null) {
            System.out.println(indent + "    ├── LEFT: " + node.getLeft().getContent());
            traverseTree(node.getLeft(), level + 2);
        }
        
        if (node.getRight() != null) {
            System.out.println(indent + "    └── RIGHT: " + node.getRight().getContent());
            traverseTree(node.getRight(), level + 2);
        }
    }
}
```

### Работа с BinaryVectorData

```java
public class BinaryVectorDataExamples {
    public static void main(String[] args) throws IOException {
        // Создание векторных данных
        float[] embedding = new float[384]; // пример эмбеддинга
        Arrays.fill(embedding, 0.1f); // заполнение тестовыми значениями
        
        BinaryVectorData vectorData = new BinaryVectorData(
            "doc_chunk_0",
            embedding,
            "Текст чанка документа о машинном обучении",
            "{\"category\": \"ai\", \"source\": \"internal\"}",
            "[documents, ai]",
            "ai_document",
            0
        );
        
        // Сериализация
        byte[] serialized = vectorData.serialize();
        System.out.println("Данные сериализованы, размер: " + serialized.length + " bytes");
        
        // Десериализация
        BinaryVectorData restored = BinaryVectorData.deserialize(serialized);
        System.out.println("Данные восстановлены:");
        System.out.println("ID: " + restored.getId());
        System.out.println("Текст: " + restored.getText());
        System.out.println("Chunk Index: " + restored.getChunkIndex());
        System.out.println("Размерность вектора: " + restored.getVector().length);
        
        // Автоматическое извлечение chunkIndex из ID
        BinaryVectorData autoChunkData = new BinaryVectorData();
        autoChunkData.setId("document_chunk_5");
        System.out.println("Автоопределенный chunkIndex: " + autoChunkData.getChunkIndex());
    }
}
```

## Работа с семантическим поиском

### Расширенный SemanticChunker

```java
public class SemanticChunkerExamples {
    public static void main(String[] args) throws Exception {
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.8
        );
        
        // Тестирование различных порогов схожести
        String text = """
            Машинное обучение - это область искусственного интеллекта. 
            Глубокое обучение использует нейронные сети. 
            Обработка естественного языка позволяет компьютерам понимать текст.
            Векторные базы данных эффективны для семантического поиска.
            """;
        
        double[] thresholds = {0.5, 0.7, 0.9};
        for (double threshold : thresholds) {
            System.out.println("\n=== Порог схожести: " + threshold + " ===");
            chunker.setSimilarityThreshold(threshold);
            
            List<SemanticChunker.Chunk> chunks = chunker.semanticChunking(text, 200);
            System.out.println("Создано чанков: " + chunks.size());
            
            for (SemanticChunker.Chunk chunk : chunks) {
                System.out.printf("Чанк [позиция: %d, длина: %d]: %s%n",
                    chunk.getPosition(), chunk.getLength(),
                    chunk.getText().substring(0, Math.min(50, chunk.getText().length())) + "...");
            }
        }
        
        // Работа с эмбеддингами
        System.out.println("\n=== Работа с эмбеддингами ===");
        String text1 = "искусственный интеллект";
        String text2 = "машинное обучение";
        String text3 = "программирование на Java";
        
        float[] embedding1 = chunker.getEmbedding(text1);
        float[] embedding2 = chunker.getEmbedding(text2);
        float[] embedding3 = chunker.getEmbedding(text3);
        
        double sim1 = chunker.cosineSimilarity(embedding1, embedding2);
        double sim2 = chunker.cosineSimilarity(embedding1, embedding3);
        
        System.out.printf("Схожесть '%s' и '%s': %.4f%n", text1, text2, sim1);
        System.out.printf("Схожесть '%s' и '%s': %.4f%n", text1, text3, sim2);
        
        // Информация о конфигурации
        System.out.println("\n=== Конфигурация чанкера ===");
        System.out.println(chunker.getConfigInfo());
    }
}
```

### Поиск с различными стратегиями

```java
public class SearchStrategies {
    public static void main(String[] args) throws Exception {
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.7
        );
        
        // Создание базы с управлением памятью
        BinaryVectorDatabase vectorDB = new BinaryVectorDatabase(
            "./data/search_demo", chunker, 512 * 1024 * 1024 // 512 MB
        );
        
        // Загрузка тестовых данных
        String[] techTopics = {
            "Java programming language with object-oriented features",
            "Python for data science and machine learning",
            "Relational databases like PostgreSQL and MySQL",
            "NoSQL databases including MongoDB and Cassandra",
            "Vector databases for semantic search applications",
            "Docker containerization technology",
            "Kubernetes for container orchestration",
            "Cloud computing with AWS and Azure"
        };
        
        for (int i = 0; i < techTopics.length; i++) {
            vectorDB.storeTextWithChunking(
                techTopics[i],
                "tech_" + i,
                new Object[]{"technology", "topic_" + (i % 3)}
            );
        }
        
        // Различные стратегии поиска
        String[] queries = {
            "programming languages",
            "databases systems",
            "cloud technologies"
        };
        
        for (String query : queries) {
            System.out.println("\n=== Запрос: '" + query + "' ===");
            
            // Поиск с разным количеством результатов
            for (int limit : new int[]{1, 3, 5}) {
                List<VectorSearchResult> results = vectorDB.similaritySearch(query, limit);
                System.out.printf("Топ-%d результатов:%n", limit);
                
                for (VectorSearchResult result : results) {
                    System.out.printf("  [%.4f] %s%n", 
                        result.getSimilarity(),
                        result.getVectorData().getText());
                }
            }
        }
        
        // Статистика памяти после поиска
        Map<String, Object> memoryStats = vectorDB.getMemoryStats();
        System.out.println("\n=== Статистика памяти ===");
        System.out.println("Использовано: " + memoryStats.get("estimatedUsageMB") + " MB");
        System.out.println("Лимит: " + memoryStats.get("maxMemoryMB") + " MB");
        
        vectorDB.close();
    }
}
```

## Знания и RAG

### KnowledgeLoader в действии

```java
public class KnowledgeLoaderExamples {
    public static void main(String[] args) throws Exception {
        KnowledgeConfig config = new KnowledgeConfig(
            "http://localhost:11434", "llama3.2", 0.8, true, true
        );
        
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.8
        );
        
        BinaryVectorDatabase vectorDB = new BinaryVectorDatabase(
            "./data/knowledge_demo", chunker, 1024 * 1024 * 1024 // 1 GB
        );
        
        KnowledgeLoader loader = new KnowledgeLoader(vectorDB, config);
        
        // Загрузка из строки
        String programmingKnowledge = """
            Объектно-ориентированное программирование (ООП) - парадигма программирования.
            Основные принципы ООП: инкапсуляция, наследование, полиморфизм.
            Класс - шаблон для создания объектов, определяющий их структуру и поведение.
            Объект - экземпляр класса, содержащий данные и методы.
            """;
            
        int chunks1 = loader.loadText(
            programmingKnowledge,
            "oop_basics",
            new Object[]{"programming", "oop"},
            300,
            "educational"
        );
        
        // Динамическая настройка порога
        System.out.println("Текущий порог: " + loader.getCurrentSimilarityThreshold());
        loader.setSimilarityThreshold(0.9);
        System.out.println("Новый порог: " + loader.getCurrentSimilarityThreshold());
        
        // Статистика
        loader.printKnowledgeStats();
        
        // Конфигурация
        Map<String, Object> loaderConfig = loader.getKnowledgeConfig();
        System.out.println("\n=== Конфигурация загрузчика ===");
        loaderConfig.forEach((k, v) -> System.out.println(k + ": " + v));
        
        vectorDB.close();
    }
}
```

### RAG с OllamaKnowledgeClient

```java
public class RAGExamples {
    public static void main(String[] args) throws Exception {
        // Настройка
        KnowledgeConfig config = new KnowledgeConfig(
            "http://localhost:11434", "llama3.2", 0.7, true, true
        );
        
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.7
        );
        
        BinaryVectorDatabase vectorDB = new BinaryVectorDatabase(
            "./data/rag_demo", chunker, 512 * 1024 * 1024 // 512 MB
        );
        
        // Загрузка знаний
        KnowledgeLoader loader = new KnowledgeLoader(vectorDB, config);
        String knowledge = """
            Компания TechCorp основана в 2010 году.
            Основные продукты: AI Assistant, Cloud Platform, Data Analytics.
            AI Assistant помогает автоматизировать поддержку клиентов.
            Cloud Platform предоставляет облачную инфраструктуру.
            Data Analytics позволяет анализировать большие данные.
            """;
            
        loader.loadText(knowledge, "company_info", 
            new Object[]{"company", "info"}, 400, "internal");
        
        // Создание клиента
        OllamaKnowledgeClient client = new OllamaKnowledgeClient(vectorDB, config);
        
        // Тестирование различных запросов
        String[] questions = {
            "Какие продукты у TechCorp?",
            "Когда основана компания?",
            "Что делает AI Assistant?",
            "Какие услуги предоставляет Cloud Platform?"
        };
        
        for (String question : questions) {
            System.out.println("\n👤 Вопрос: " + question);
            
            // Поиск релевантных фактов
            List<String> facts = client.findRelevantFacts(question, 2);
            System.out.println("📚 Релевантные факты:");
            facts.forEach(fact -> System.out.println("  - " + fact));
            
            // Генерация ответа
            String answer = client.generateResponseWithKnowledge(question);
            System.out.println("🤖 Ответ: " + answer);
        }
        
        vectorDB.close();
    }
}
```

### Интерактивный чат

```java
public class InteractiveChatExample {
    public static void main(String[] args) throws Exception {
        KnowledgeConfig config = new KnowledgeConfig(
            "http://localhost:11434", "llama3.2", 0.7, true, true
        );
        
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.7
        );
        
        BinaryVectorDatabase vectorDB = new BinaryVectorDatabase(
            "./data/chat_demo", chunker, 1024 * 1024 * 1024 // 1 GB
        );
        
        // Загрузка знаний для чата
        KnowledgeLoader loader = new KnowledgeLoader(vectorDB, config);
        String chatKnowledge = """
            Технологии искусственного интеллекта развиваются быстро.
            Машинное обучение используется для прогнозирования и классификации.
            Нейронные сети имитируют работу человеческого мозга.
            Обработка естественного языка позволяет компьютерам понимать текст.
            """;
            
        loader.loadText(chatKnowledge, "ai_knowledge",
            new Object[]{"ai", "knowledge"}, 300, "educational");
        
        OllamaKnowledgeClient client = new OllamaKnowledgeClient(vectorDB, config);
        
        System.out.println("🚀 Запуск интерактивного чата...");
        System.out.println("Введите ваш вопрос (или 'выход' для завершения):\n");
        
        // Запуск чата
        client.startInteractiveChat();
        
        vectorDB.close();
    }
}
```

## Продвинутые сценарии

### PromptGenerator для различных задач

```java
public class PromptGeneratorExamples {
    public static void main(String[] args) throws Exception {
        KnowledgeConfig config = new KnowledgeConfig(
            "http://localhost:11434", "llama3.2", 0.7, true, true
        );
        
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.7
        );
        
        BinaryVectorDatabase vectorDB = new BinaryVectorDatabase(
            "./data/prompt_demo", chunker, 512 * 1024 * 1024 // 512 MB
        );
        
        // Загрузка данных для промптов
        KnowledgeLoader loader = new KnowledgeLoader(vectorDB, config);
        String data = """
            Java - строго типизированный объектно-ориентированный язык.
            Spring Framework - популярный фреймворк для Java приложений.
            Hibernate - ORM для работы с базами данных в Java.
            Maven - инструмент для сборки и управления зависимостями.
            """;
            
        loader.loadText(data, "java_tech", 
            new Object[]{"technology", "java"}, 350, "tech_docs");
        
        PromptGenerator promptGenerator = new PromptGenerator(vectorDB, config);
        
        // Генерация различных типов промптов
        System.out.println("=== Контекстный промпт ===");
        String contextPrompt = promptGenerator.createContextPrompt(
            "Какие технологии используются в Java разработке?", 3, 0.7
        );
        System.out.println(contextPrompt);
        
        System.out.println("\n=== Промпт для генерации вопросов ===");
        String questionPrompt = promptGenerator.createQuestionGenerationPrompt(
            "Java технологии", 5, 0.6
        );
        System.out.println(questionPrompt);
        
        System.out.println("\n=== Промпт для суммаризации ===");
        String summaryPrompt = promptGenerator.createSummarizationPrompt(
            "Java frameworks", 3, 0.65
        );
        System.out.println(summaryPrompt);
        
        vectorDB.close();
    }
}
```

## Новый функционал

### Управление памятью в BinaryVectorDatabase

```java
public class MemoryManagementExample {
    public static void main(String[] args) throws Exception {
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.7
        );
        
        // Создание базы с ограничением памяти
        BinaryVectorDatabase vectorDB = new BinaryVectorDatabase(
            "./data/memory_managed", chunker, 500 * 1024 * 1024 // 500 MB
        );
        
        KnowledgeLoader loader = new KnowledgeLoader(vectorDB, 
            new KnowledgeConfig("http://localhost:11434", "deepseek-v3.1:671b-cloud", 0.7, true, true));
        
        // Загрузка данных с мониторингом памяти
        loadDataWithMemoryMonitoring(loader, vectorDB);
        
        // Проверка использования памяти
        Map<String, Object> memoryStats = vectorDB.getMemoryStats();
        System.out.println("=== Memory Statistics ===");
        memoryStats.forEach((key, value) -> System.out.println(key + ": " + value));
        
        vectorDB.close();
    }
    
    private static void loadDataWithMemoryMonitoring(KnowledgeLoader loader, BinaryVectorDatabase vectorDB) throws Exception {
        // Загрузка данных порциями с проверкой памяти
        String[] documents = {
            "Документ о машинном обучении и искусственном интеллекте...",
            "Документ о базах данных и системах хранения информации...",
            "Документ о веб-разработке и современных фреймворках...",
            "Документ о мобильной разработке и платформах...",
            "Документ о DevOps и практиках непрерывной интеграции...",
            // ... больше документов
        };
        
        for (int i = 0; i < documents.length; i++) {
            loader.loadText(documents[i], "doc_" + i,
                new Object[]{"documents", "batch1"}, 500, "document_" + i);
            
            // Проверка памяти каждые 2 документа
            if (i % 2 == 0) {
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

### Работа с индексами

```java
public class IndexManagementExample {
    public static void main(String[] args) throws Exception {
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.7
        );
        
        BinaryVectorDatabase vectorDB = new BinaryVectorDatabase(
            "./data/index_demo", chunker, 1024 * 1024 * 1024 // 1 GB
        );
        
        // Загрузка тестовых данных
        KnowledgeLoader loader = new KnowledgeLoader(vectorDB,
            new KnowledgeConfig("http://localhost:11434", "llama3.2", 0.7, true, true));
        
        String[] documents = {
            "Java programming language for enterprise applications",
            "Python for data science and machine learning projects", 
            "JavaScript for web development and frontend applications",
            "Go language for concurrent programming and microservices",
            "Rust for systems programming and memory safety"
        };
        
        for (int i = 0; i < documents.length; i++) {
            loader.loadText(documents[i], "lang_doc_" + i,
                new Object[]{"languages", "doc_" + i}, 400, "programming");
        }
        
        // Создание индексов
        System.out.println("=== Создание индексов ===");
        vectorDB.createIndex("content_index", "content");
        vectorDB.createIndex("language_index", "metadata.category");
        vectorDB.createIndex("document_index", "metadata.documentId");
        
        // Информация об индексах
        Map<String, Object> indexesInfo = vectorDB.getIndexesInfo();
        System.out.println("\n=== Информация об индексах ===");
        indexesInfo.forEach((indexName, info) -> {
            Map<String, Object> indexInfo = (Map<String, Object>) info;
            System.out.printf("%s: %d записей, %d KB памяти%n",
                indexName, indexInfo.get("size"), indexInfo.get("estimatedMemoryKB"));
        });
        
        // Поиск по индексу
        System.out.println("\n=== Поиск по индексу ===");
        List<String> searchResults = vectorDB.searchByIndex("content_index", "programming");
        System.out.println("Найдено документов: " + searchResults.size());
        searchResults.forEach(id -> System.out.println("  - " + id));
        
        // Удаление индекса
        System.out.println("\n=== Удаление индекса ===");
        vectorDB.dropIndex("document_index");
        
        // Обновленная информация об индексах
        Map<String, Object> updatedIndexesInfo = vectorDB.getIndexesInfo();
        System.out.println("Оставшиеся индексы: " + updatedIndexesInfo.keySet());
        
        vectorDB.close();
    }
}
```

### JSON интеграция с BinaryVectorDBJsonManager

```java
public class JsonIntegrationExample {
    public static void main(String[] args) throws Exception {
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.7
        );
        
        BinaryVectorDatabase vectorDB = new BinaryVectorDatabase(
            "./data/json_demo", chunker, 512 * 1024 * 1024 // 512 MB
        );
        
        BinaryVectorDBJsonManager jsonManager = new BinaryVectorDBJsonManager(vectorDB);
        
        // Загрузка тестовых данных
        KnowledgeLoader loader = new KnowledgeLoader(vectorDB,
            new KnowledgeConfig("http://localhost:11434", "llama3.2", 0.7, true, true));
        
        String data = """
            Искусственный интеллект преобразует современные технологии.
            Машинное обучение позволяет компьютерам обучаться на данных.
            Глубокое обучение использует нейронные сети для сложных задач.
            """;
            
        loader.loadText(data, "ai_data",
            new Object[]{"ai", "knowledge"}, 300, "educational");
        
        // Экспорт в JSON
        System.out.println("=== Экспорт данных в JSON ===");
        JSONArray allVectors = jsonManager.exportAllVectorDataToJson();
        System.out.println("Экспортировано векторов: " + allVectors.length());
        
        JSONArray allNodes = jsonManager.exportAllTreeNodesToJson();
        System.out.println("Экспортировано узлов: " + allNodes.length());
        
        // Экспорт статистики
        JSONObject stats = jsonManager.exportDatabaseStatsToJson();
        System.out.println("\n=== Статистика базы ===");
        System.out.println(stats.toString(2));
        
        // Поиск и экспорт
        System.out.println("\n=== Поиск и экспорт ===");
        JSONArray searchResults = jsonManager.searchAndExportToJson("искусственный интеллект", 3);
        System.out.println("Результатов поиска: " + searchResults.length());
        
        // Сохранение в файлы
        System.out.println("\n=== Сохранение в файлы ===");
        jsonManager.saveDatabaseToJsonFiles("./json_export");
        System.out.println("Данные сохранены в папку ./json_export");
        
        // Работа с отдельными объектами
        System.out.println("\n=== Работа с отдельными объектами ===");
        List<BinaryVectorData> allData = vectorDB.findAllVectorData();
        if (!allData.isEmpty()) {
            BinaryVectorData sampleData = allData.get(0);
            
            // Конвертация в JSON строку
            String jsonString = jsonManager.vectorDataToJsonString(sampleData);
            System.out.println("JSON строка (первые 200 символов): " + 
                jsonString.substring(0, Math.min(200, jsonString.length())) + "...");
            
            // Восстановление из JSON строки
            BinaryVectorData restoredData = jsonManager.vectorDataFromJsonString(jsonString);
            System.out.println("Данные восстановлены: " + restoredData.getId());
        }
        
        // Пакетный экспорт
        System.out.println("\n=== Пакетный экспорт ===");
        jsonManager.batchExportToJsonFile("./full_export.json");
        System.out.println("Пакетный экспорт завершен");
        
        vectorDB.close();
    }
}
```

### SQL-подобный интерфейс

```java
public class SQLInterfaceExample {
    public static void main(String[] args) throws Exception {
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.7
        );
        
        BinaryVectorDatabase vectorDB = new BinaryVectorDatabase(
            "./data/sql_demo", chunker, 512 * 1024 * 1024 // 512 MB
        );
        
        SQLParser sqlParser = new SQLParser(vectorDB);
        
        // Загрузка тестовых данных
        KnowledgeLoader loader = new KnowledgeLoader(vectorDB,
            new KnowledgeConfig("http://localhost:11434", "llama3.2", 0.7, true, true));
        
        String[] documents = {
            "Java programming for enterprise applications",
            "Python for data science and AI",
            "JavaScript for web development",
            "Database management with SQL"
        };
        
        for (int i = 0; i < documents.length; i++) {
            loader.loadText(documents[i], "doc_" + i,
                new Object[]{"technology", "lang_" + i}, 300, "tech");
        }
        
        // Выполнение SQL-подобных запросов
        System.out.println("=== SQL-подобные запросы ===");
        
        // SELECT запрос
        System.out.println("\n--- SELECT запрос ---");
        List<JSONObject> selectResults = sqlParser.execute(
            "SELECT * FROM documents WHERE text LIKE '%programming%' LIMIT 2"
        );
        System.out.println("Найдено записей: " + selectResults.size());
        selectResults.forEach(json -> System.out.println("  - " + json.toString()));
        
        // Семантический поиск через SQL интерфейс
        System.out.println("\n--- Семантический поиск ---");
        List<JSONObject> semanticResults = sqlParser.semanticSearch("data science", 3);
        System.out.println("Семантических результатов: " + semanticResults.size());
        
        // Гибридный поиск
        System.out.println("\n--- Гибридный поиск ---");
        List<JSONObject> hybridResults = sqlParser.hybridSearch("web development programming", 5);
        System.out.println("Гибридных результатов: " + hybridResults.size());
        
        vectorDB.close();
    }
}
```

### Комплексный пример с новым функционалом

```java
public class ComprehensiveExample {
    public static void main(String[] args) throws Exception {
        // Инициализация с расширенными настройками
        KnowledgeConfig config = new KnowledgeConfig(
            "http://localhost:11434", "deepseek-v3.1:671b-cloud", 0.7, true, true
        );
        
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.7
        );
        
        // Создание базы с 2 GB памяти
        BinaryVectorDatabase vectorDB = new BinaryVectorDatabase(
            "./data/comprehensive", chunker, 2L * 1024 * 1024 * 1024
        );
        
        KnowledgeLoader loader = new KnowledgeLoader(vectorDB, config);
        OllamaKnowledgeClient client = new OllamaKnowledgeClient(vectorDB, config);
        BinaryVectorDBJsonManager jsonManager = new BinaryVectorDBJsonManager(vectorDB);
        
        // Загрузка разнообразных данных
        System.out.println("=== Загрузка данных ===");
        Map<String, String> documents = new HashMap<>();
        documents.put("ai_basics", "Искусственный интеллект и машинное обучение...");
        documents.put("web_dev", "Веб-разработка с современными фреймворками...");
        documents.put("databases", "Реляционные и NoSQL базы данных...");
        documents.put("cloud", "Облачные вычисления и микросервисы...");
        
        Map<String, Integer> loadResults = loader.loadTextBatch(documents, "tech_docs",
            new Object[]{"technology"}, 500);
        
        System.out.println("Загружено документов:");
        loadResults.forEach((name, chunks) -> 
            System.out.println("  " + name + ": " + chunks + " чанков")
        );
        
        // Создание индексов для оптимизации
        System.out.println("\n=== Создание индексов ===");
        vectorDB.createIndex("content_index", "content");
        vectorDB.createIndex("tech_index", "metadata.category");
        
        // Мониторинг памяти
        System.out.println("\n=== Мониторинг памяти ===");
        Map<String, Object> memoryStats = vectorDB.getMemoryStats();
        memoryStats.forEach((key, value) -> System.out.println(key + ": " + value));
        
        // Информация об индексах
        System.out.println("\n=== Информация об индексах ===");
        Map<String, Object> indexesInfo = vectorDB.getIndexesInfo();
        indexesInfo.forEach((name, info) -> 
            System.out.println(name + ": " + info)
        );
        
        // Экспорт в JSON
        System.out.println("\n=== Экспорт в JSON ===");
        jsonManager.saveDatabaseToJsonFiles("./comprehensive_export");
        System.out.println("Данные экспортированы в JSON");
        
        // Демонстрация поиска
        System.out.println("\n=== Демонстрация поиска ===");
        String[] testQueries = {"искусственный интеллект", "базы данных", "веб-разработка"};
        
        for (String query : testQueries) {
            System.out.println("\nЗапрос: " + query);
            
            // Поиск по индексу
            List<String> indexResults = vectorDB.searchByIndex("content_index", query);
            System.out.println("Результаты по индексу: " + indexResults.size());
            
            // Семантический поиск
            List<VectorSearchResult> semanticResults = vectorDB.similaritySearch(query, 2);
            System.out.println("Семантические результаты: " + semanticResults.size());
            
            // Генерация ответа с использованием знаний
            String answer = client.generateResponseWithKnowledge(query);
            System.out.println("Ответ ИИ: " + answer.substring(0, Math.min(100, answer.length())) + "...");
        }
        
        // Грациозное завершение
        System.out.println("\n=== Завершение работы ===");
        vectorDB.close();
        System.out.println("База данных закрыта, все данные сохранены");
    }
}
```

## Интеграционные примеры

### Веб-сервис с BinaryVectorDatabase

```java
// Пример простого HTTP сервера с BinaryVectorDatabase
public class BinaryVectorDBWebService {
    private BinaryVectorDatabase vectorDB;
    private OllamaKnowledgeClient knowledgeClient;
    private BinaryVectorDBJsonManager jsonManager;
    
    public BinaryVectorDBWebService() throws Exception {
        KnowledgeConfig config = new KnowledgeConfig(
            "http://localhost:11434", "llama3.2", 0.7, true, true
        );
        
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.7
        );
        
        this.vectorDB = new BinaryVectorDatabase("./data/webservice", chunker, 1024 * 1024 * 1024);
        this.knowledgeClient = new OllamaKnowledgeClient(vectorDB, config);
        this.jsonManager = new BinaryVectorDBJsonManager(vectorDB);
        
        // Создание индексов для веб-сервиса
        vectorDB.createIndex("web_content_index", "content");
    }
    
    public String handleSearch(String query, int limit) {
        try {
            List<VectorSearchResult> results = vectorDB.similaritySearch(query, limit);
            
            StringBuilder response = new StringBuilder();
            response.append("Результаты поиска для: ").append(query).append("\n\n");
            
            for (VectorSearchResult result : results) {
                response.append(String.format("Схожесть: %.4f%n", result.getSimilarity()));
                response.append("Текст: ").append(result.getVectorData().getText()).append("\n");
                response.append("---\n");
            }
            
            return response.toString();
        } catch (Exception e) {
            return "Ошибка поиска: " + e.getMessage();
        }
    }
    
    public String handleQuestion(String question) {
        try {
            return knowledgeClient.generateResponseWithKnowledge(question);
        } catch (Exception e) {
            return "Ошибка генерации ответа: " + e.getMessage();
        }
    }
    
    public JSONObject getStats() {
        return jsonManager.exportDatabaseStatsToJson();
    }
    
    public static void main(String[] args) throws Exception {
        BinaryVectorDBWebService service = new BinaryVectorDBWebService();
        
        // Пример использования
        System.out.println("=== Пример веб-сервиса с BinaryVectorDatabase ===");
        
        String searchResults = service.handleSearch("искусственный интеллект", 3);
        System.out.println(searchResults);
        
        String answer = service.handleQuestion("Что такое машинное обучение?");
        System.out.println("Ответ на вопрос: " + answer);
        
        JSONObject stats = service.getStats();
        System.out.println("Статистика: " + stats.toString(2));
        
        service.vectorDB.close();
    }
}
```

## Утилиты и инструменты

### Мониторинг производительности с новым функционалом

```java
public class AdvancedPerformanceMonitor {
    
    public static void monitorWithMemoryTracking(BinaryVectorDatabase vectorDB, String query, int iterations) {
        System.out.println("=== Расширенный мониторинг производительности ===");
        System.out.println("Запрос: " + query);
        System.out.println("Итераций: " + iterations);
        
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.currentTimeMillis();
            
            try {
                List<VectorSearchResult> results = vectorDB.similaritySearch(query, 5);
                long duration = System.currentTimeMillis() - startTime;
                totalTime += duration;
                minTime = Math.min(minTime, duration);
                maxTime = Math.max(maxTime, duration);
                
                System.out.printf("Итерация %d: %d мс, найдено: %d результатов%n",
                    i + 1, duration, results.size());
                    
                // Проверка памяти каждые 5 итераций
                if (i % 5 == 0) {
                    Map<String, Object> memoryStats = vectorDB.getMemoryStats();
                    System.out.printf("  Память: %d/%d MB%n",
                        memoryStats.get("estimatedUsageMB"), memoryStats.get("maxMemoryMB"));
                }
                    
            } catch (Exception e) {
                System.out.println("Ошибка в итерации " + (i + 1) + ": " + e.getMessage());
            }
        }
        
        System.out.printf("\nИтоговая статистика:%n");
        System.out.printf("Среднее время: %.2f мс%n", (double) totalTime / iterations);
        System.out.printf("Минимальное время: %d мс%n", minTime);
        System.out.printf("Максимальное время: %d мс%n", maxTime);
        
        // Информация об индексах
        Map<String, Object> indexesInfo = vectorDB.getIndexesInfo();
        System.out.println("Активные индексы: " + indexesInfo.keySet());
    }
    
    public static void main(String[] args) throws Exception {
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.8
        );
        
        BinaryVectorDatabase vectorDB = new BinaryVectorDatabase(
            "./data/performance", chunker, 512 * 1024 * 1024
        );
        
        // Создание индексов для тестирования
        vectorDB.createIndex("perf_content_index", "content");
        
        // Загрузка тестовых данных
        for (int i = 0; i < 100; i++) {
            vectorDB.storeTextWithChunking(
                "Тестовый документ номер " + i + " с некоторым содержанием для тестирования производительности",
                "test_doc_" + i,
                new Object[]{"test", "performance"}
            );
        }
        
        // Тестирование производительности
        monitorWithMemoryTracking(vectorDB, "тестовый документ", 10);
        
        vectorDB.close();
    }
}
```

### Утилиты для отладки нового функционала

```java
public class AdvancedDebugUtilities {
    
    public static void printBinaryVectorDBInfo(BinaryVectorDatabase vectorDB) {
        System.out.println("=== Расширенная информация о BinaryVectorDatabase ===");
        System.out.println("Количество векторов: " + vectorDB.getVectorCount());
        System.out.println("Количество узлов: " + vectorDB.getTreeNodeCount());
        
        // Статистика памяти
        Map<String, Object> memoryStats = vectorDB.getMemoryStats();
        System.out.println("\n=== Статистика памяти ===");
        memoryStats.forEach((key, value) -> System.out.println(key + ": " + value));
        
        // Информация об индексах
        Map<String, Object> indexesInfo = vectorDB.getIndexesInfo();
        System.out.println("\n=== Информация об индексах ===");
        if (indexesInfo.isEmpty()) {
            System.out.println("Индексы не созданы");
        } else {
            indexesInfo.forEach((name, info) -> {
                Map<String, Object> indexInfo = (Map<String, Object>) info;
                System.out.printf("%s: %d записей, %d KB%n",
                    name, indexInfo.get("size"), indexInfo.get("estimatedMemoryKB"));
            });
        }
        
        // Пример информации о нескольких векторах
        List<BinaryVectorData> sampleVectors = vectorDB.findAllVectorData();
        int sampleSize = Math.min(3, sampleVectors.size());
        
        System.out.println("\nПримеры векторов (" + sampleSize + " из " + sampleVectors.size() + "):");
        for (int i = 0; i < sampleSize; i++) {
            BinaryVectorData vector = sampleVectors.get(i);
            System.out.printf("  %d. ID: %s%n", i + 1, vector.getId());
            System.out.printf("     Текст: %s%n", 
                vector.getText().substring(0, Math.min(50, vector.getText().length())) + "...");
            System.out.printf("     Chunk Index: %d%n", vector.getChunkIndex());
            System.out.printf("     Путь: %s%n", vector.getNodePath());
        }
    }
    
    public static void testBinarySerialization() throws IOException {
        System.out.println("\n=== Тест бинарной сериализации ===");
        
        // Создание тестовых данных
        BinaryVectorData vectorData = new BinaryVectorData(
            "test_binary_1",
            new float[]{0.1f, 0.2f, 0.3f},
            "Тестовый текст для бинарной сериализации",
            "{\"test\": true}",
            "[test, binary]",
            "test_doc",
            0
        );
        
        // Сериализация
        byte[] serialized = vectorData.serialize();
        System.out.println("Размер сериализованных данных: " + serialized.length + " bytes");
        
        // Десериализация
        BinaryVectorData restored = BinaryVectorData.deserialize(serialized);
        System.out.println("Данные восстановлены: " + restored.getId());
        System.out.println("Вектор восстановлен: " + (restored.getVector() != null ? "да" : "нет"));
    }
    
    public static void main(String[] args) throws Exception {
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.8
        );
        
        BinaryVectorDatabase vectorDB = new BinaryVectorDatabase(
            "./data/debug_demo", chunker, 256 * 1024 * 1024
        );
        
        // Создание индексов для тестирования
        vectorDB.createIndex("debug_content_index", "content");
        
        // Загрузка демо данных
        vectorDB.storeTextWithChunking(
            "Тестовый текст для отладки и демонстрации возможностей системы с бинарной сериализацией",
            "debug_doc",
            new Object[]{"debug", "test"}
        );
        
        // Запуск утилит
        printBinaryVectorDBInfo(vectorDB);
        testBinarySerialization();
        
        vectorDB.close();
    }
}
```

## Заключение

Эти примеры демонстрируют полный спектр возможностей VectorDB, включая новый функционал с бинарной сериализацией, управлением памятью, системой индексов и JSON интеграцией.

### Ключевые особенности нового функционала:

1. **BinaryVectorDatabase** - улучшенная производительность с бинарной сериализацией
2. **Управление памятью** - контроль использования оперативной памяти
3. **Система индексов** - ускорение поисковых запросов
4. **BinaryVectorDBJsonManager** - полная поддержка JSON формата
5. **BinaryTreeNode/BinaryVectorData** - оптимизированные структуры данных

### Рекомендации по использованию:

1. Начните с примеров из раздела "Быстрый старт"
2. Изучите управление памятью для оптимизации производительности
3. Используйте индексы для ускорения часто выполняемых запросов
4. Применяйте JSON интеграцию для экспорта/импорта данных
5. Тестируйте производительность с различными настройками памяти

Все примеры готовы к запуску и требуют только наличия запущенного Ollama сервера с соответствующими моделями.