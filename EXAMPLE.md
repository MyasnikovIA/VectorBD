# VectorDB - Примеры использования

## Оглавление
1. [Быстрый старт](#быстрый-старт)
2. [Базовые операции](#базовые-операции)
3. [Работа с семантическим поиском](#работа-с-семантическим-поиском)
4. [Знания и RAG](#знания-и-rag)
5. [Продвинутые сценарии](#продвинутые-сценарии)
6. [Интеграционные примеры](#интеграционные-примеры)
7. [Утилиты и инструменты](#утилиты-и-инструменты)

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
            
            VectorDatabase vectorDB = new VectorDatabase("./data/quickstart", chunker);
            
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

### Работа с VectorDatabase

```java
public class VectorDBOperations {
    public static void main(String[] args) throws Exception {
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.8
        );
        VectorDatabase vectorDB = new VectorDatabase("./data/operations", chunker);
        
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
        List<VectorData> exactResults = vectorDB.exactSearch("Java");
        exactResults.forEach(data -> 
            System.out.println("Найдено: " + data.getText())
        );
        
        System.out.println("\n=== Поиск по пути ===");
        List<VectorData> pathResults = vectorDB.searchByPath("lang");
        pathResults.forEach(data -> 
            System.out.println("Путь: " + data.getNodePath() + " - " + data.getText())
        );
        
        // Статистика
        System.out.println("\n=== Статистика ===");
        System.out.println("Векторов: " + vectorDB.getVectorCount());
        System.out.println("Узлов: " + vectorDB.getTreeNodeCount());
        
        vectorDB.close();
    }
}
```

### Работа с TreeNode

```java
public class TreeNodeExamples {
    public static void main(String[] args) {
        TreeNode root = new TreeNode();
        
        // Базовые операции
        root.setNode(new Object[]{"users", "alice", "name"}, "Alice Johnson");
        root.setNode(new Object[]{"users", "alice", "age"}, 28);
        root.setNode(new Object[]{"users", "alice", "email"}, "alice@example.com");
        
        root.setNode(new Object[]{"users", "bob", "name"}, "Bob Smith");
        root.setNode(new Object[]{"users", "bob", "age"}, 32);
        
        root.setNode(new Object[]{"config", "database", "url"}, "jdbc:postgresql://localhost/test");
        root.setNode(new Object[]{"config", "database", "username"}, "admin");
        
        // Получение данных
        String name = (String) root.getNode(new Object[]{"users", "alice", "name"});
        Integer age = (Integer) root.getNode(new Object[]{"users", "alice", "age"});
        System.out.println("Пользователь: " + name + ", возраст: " + age);
        
        // Поиск
        System.out.println("\n=== Поиск пользователей ===");
        List<TreeNode.QueryResult> users = root.query(new Object[]{"users"}, 2);
        users.forEach(result -> 
            System.out.println("Путь: " + result.getPathString() + " = " + result.getValue())
        );
        
        // Метаданные
        root.setMetadata("created", "2024-01-01");
        root.setMetadata("version", "1.0");
        
        // JSON сериализация
        String json = root.toJsonString();
        System.out.println("\n=== JSON представление ===");
        System.out.println(json);
        
        // Визуализация дерева
        System.out.println("\n=== Структура дерева ===");
        System.out.println(root.toTreeString());
        
        // Статистика
        System.out.println("\n=== Статистика ===");
        System.out.println("Всего узлов: " + root.countNodes());
        System.out.println("Глубина: " + root.getDepth());
        System.out.println("Ширина: " + root.getWidth());
        System.out.println("Листовые значения: " + root.getLeafValues());
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
        VectorDatabase vectorDB = new VectorDatabase("./data/search_demo", chunker);
        
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
        VectorDatabase vectorDB = new VectorDatabase("./data/knowledge_demo", chunker);
        
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
        
        // Загрузка из файла (пример)
        // int chunks2 = loader.loadTextFile(
        //     "path/to/document.txt",
        //     "file_doc",
        //     new Object[]{"docs", "external"},
        //     500
        // );
        
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
        VectorDatabase vectorDB = new VectorDatabase("./data/rag_demo", chunker);
        
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
        VectorDatabase vectorDB = new VectorDatabase("./data/chat_demo", chunker);
        
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
        VectorDatabase vectorDB = new VectorDatabase("./data/prompt_demo", chunker);
        
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

### Расширенная работа с деревьями

```java
public class AdvancedTreeNodeExamples {
    public static void main(String[] args) {
        TreeNode company = new TreeNode();
        
        // Создание сложной структуры
        company.setNode(new Object[]{"departments", "engineering", "team", "frontend", "members", "alice"}, 
            "Alice Johnson - Senior Frontend Developer");
        company.setNode(new Object[]{"departments", "engineering", "team", "frontend", "members", "bob"}, 
            "Bob Smith - Frontend Developer");
        company.setNode(new Object[]{"departments", "engineering", "team", "backend", "members", "charlie"}, 
            "Charlie Brown - Backend Team Lead");
        
        company.setNode(new Object[]{"departments", "sales", "region", "europe", "manager"}, 
            "Diana Prince - Europe Sales Manager");
        company.setNode(new Object[]{"departments", "sales", "region", "asia", "manager"}, 
            "Eve Wilson - Asia Sales Manager");
        
        // Метаданные
        company.setMetadata("companyName", "TechInnovations Inc.");
        company.setMetadata("founded", "2015");
        company.setMetadata("employees", 150);
        
        // Расширенный поиск
        System.out.println("=== Поиск менеджеров ===");
        List<Map.Entry<List<Object>, Object>> managers = 
            company.findValuesByPattern("Manager");
        managers.forEach(entry -> 
            System.out.println("Путь: " + entry.getKey() + " -> " + entry.getValue())
        );
        
        // Поиск путей
        System.out.println("\n=== Путь к Alice ===");
        List<Object> pathToAlice = company.getPathToNode("Alice Johnson - Senior Frontend Developer");
        System.out.println("Путь: " + pathToAlice);
        
        // Статистика организации
        System.out.println("\n=== Статистика компании ===");
        System.out.println("Всего узлов в структуре: " + company.countNodes());
        System.out.println("Глубина структуры: " + company.getDepth());
        System.out.println("Ширина структуры: " + company.getWidth());
        
        // Визуализация
        System.out.println("\n=== Организационная структура ===");
        System.out.println(company.toTreeString());
        
        // JSON экспорт
        System.out.println("\n=== JSON экспорт ===");
        String json = company.toJsonString();
        System.out.println(json.substring(0, Math.min(500, json.length())) + "...");
        
        // Создание из JSON
        TreeNode importedCompany = TreeNode.fromJsonString(json);
        System.out.println("\nИмпортировано узлов: " + importedCompany.countNodes());
    }
}
```

### Управление конфигурацией

```java
public class ConfigurationManagement {
    public static void main(String[] args) {
        // Различные конфигурации для разных сценариев
        
        // Высокая точность
        KnowledgeConfig highPrecisionConfig = new KnowledgeConfig(
            "http://localhost:11434", "llama3.2", 0.9, true, true
        );
        
        // Баланс скорости и качества
        KnowledgeConfig balancedConfig = new KnowledgeConfig(
            "http://localhost:11434", "deepseek-v3.1:671b-cloud", 0.7, true, true
        );
        
        // Максимальная скорость
        KnowledgeConfig speedConfig = new KnowledgeConfig(
            "http://localhost:11434", "llama3.2", 0.6, false, true
        );
        
        // Проверка валидности конфигураций
        System.out.println("=== Проверка конфигураций ===");
        System.out.println("High precision config valid: " + highPrecisionConfig.isValid());
        System.out.println("Balanced config valid: " + balancedConfig.isValid());
        System.out.println("Speed config valid: " + speedConfig.isValid());
        
        // Использование разных конфигураций
        System.out.println("\n=== Конфигурации для разных сценариев ===");
        
        System.out.println("Техническая документация:");
        highPrecisionConfig.printConfig();
        
        System.out.println("\nЧат-бот поддержки:");
        balancedConfig.printConfig();
        
        System.out.println("\nРеальное время:");
        speedConfig.printConfig();
    }
}
```

## Интеграционные примеры

### Веб-сервис с VectorDB

```java
// Пример простого HTTP сервера с VectorDB
public class VectorDBWebService {
    private VectorDatabase vectorDB;
    private OllamaKnowledgeClient knowledgeClient;
    
    public VectorDBWebService() throws Exception {
        KnowledgeConfig config = new KnowledgeConfig(
            "http://localhost:11434", "llama3.2", 0.7, true, true
        );
        
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.7
        );
        
        this.vectorDB = new VectorDatabase("./data/webservice", chunker);
        this.knowledgeClient = new OllamaKnowledgeClient(vectorDB, config);
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
    
    public static void main(String[] args) throws Exception {
        VectorDBWebService service = new VectorDBWebService();
        
        // Пример использования
        System.out.println("=== Пример веб-сервиса ===");
        
        String searchResults = service.handleSearch("искусственный интеллект", 3);
        System.out.println(searchResults);
        
        String answer = service.handleQuestion("Что такое машинное обучение?");
        System.out.println("Ответ на вопрос: " + answer);
        
        service.vectorDB.close();
    }
}
```

### Интеграция с Spring Boot

```java
/*
// Пример конфигурации Spring Boot
@Configuration
public class VectorDBConfig {
    
    @Bean
    @Primary
    public SemanticChunker semanticChunker() {
        return new SemanticChunker(
            "http://localhost:11434",
            "all-minilm:22m",
            0.8
        );
    }
    
    @Bean
    @Primary
    public VectorDatabase vectorDatabase(SemanticChunker chunker) {
        return new VectorDatabase("./data/spring_app", chunker);
    }
    
    @Bean
    @Primary
    public KnowledgeConfig knowledgeConfig() {
        return new KnowledgeConfig(
            "http://localhost:11434",
            "llama3.2",
            0.7,
            true,
            true
        );
    }
    
    @Bean
    @Primary
    public OllamaKnowledgeClient knowledgeClient(VectorDatabase vectorDB, 
                                               KnowledgeConfig config) {
        return new OllamaKnowledgeClient(vectorDB, config);
    }
}

// Пример сервиса
@Service
public class KnowledgeService {
    
    @Autowired
    private VectorDatabase vectorDB;
    
    @Autowired
    private OllamaKnowledgeClient knowledgeClient;
    
    @Autowired
    private KnowledgeLoader knowledgeLoader;
    
    public void addDocument(String document, String docId, String category) {
        try {
            vectorDB.storeTextWithChunking(
                document, docId, new Object[]{"docs", category}
            );
        } catch (Exception e) {
            throw new RuntimeException("Ошибка добавления документа", e);
        }
    }
    
    public List<SearchResult> search(String query, int limit) {
        try {
            List<VectorSearchResult> vectorResults = 
                vectorDB.similaritySearch(query, limit);
            
            return vectorResults.stream()
                .map(this::toSearchResult)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка поиска", e);
        }
    }
    
    public String askQuestion(String question) {
        return knowledgeClient.generateResponseWithKnowledge(question);
    }
    
    private SearchResult toSearchResult(VectorSearchResult vectorResult) {
        return new SearchResult(
            vectorResult.getVectorData().getText(),
            vectorResult.getSimilarity()
        );
    }
    
    public static class SearchResult {
        private final String text;
        private final double similarity;
        
        public SearchResult(String text, double similarity) {
            this.text = text;
            this.similarity = similarity;
        }
        
        // getters
    }
}
*/
```

## Утилиты и инструменты

### Мониторинг производительности

```java
public class PerformanceMonitor {
    private long startTime;
    
    public void startTiming() {
        startTime = System.currentTimeMillis();
    }
    
    public long stopTiming() {
        return System.currentTimeMillis() - startTime;
    }
    
    public static void monitorSearch(VectorDatabase vectorDB, String query, int iterations) {
        PerformanceMonitor monitor = new PerformanceMonitor();
        
        System.out.println("=== Мониторинг производительности поиска ===");
        System.out.println("Запрос: " + query);
        System.out.println("Итераций: " + iterations);
        
        long totalTime = 0;
        
        for (int i = 0; i < iterations; i++) {
            monitor.startTiming();
            
            try {
                List<VectorSearchResult> results = vectorDB.similaritySearch(query, 5);
                long duration = monitor.stopTiming();
                totalTime += duration;
                
                System.out.printf("Итерация %d: %d мс, найдено: %d результатов%n",
                    i + 1, duration, results.size());
                    
            } catch (Exception e) {
                System.out.println("Ошибка в итерации " + (i + 1) + ": " + e.getMessage());
            }
        }
        
        System.out.printf("Среднее время: %.2f мс%n", (double) totalTime / iterations);
    }
    
    public static void main(String[] args) throws Exception {
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.8
        );
        VectorDatabase vectorDB = new VectorDatabase("./data/performance", chunker);
        
        // Загрузка тестовых данных
        for (int i = 0; i < 100; i++) {
            vectorDB.storeTextWithChunking(
                "Тестовый документ номер " + i + " с некоторым содержанием",
                "test_doc_" + i,
                new Object[]{"test", "docs"}
            );
        }
        
        // Тестирование производительности
        monitorSearch(vectorDB, "тестовый документ", 10);
        
        vectorDB.close();
    }
}
```

### Утилиты для отладки

```java
public class DebugUtilities {
    
    public static void printVectorDBInfo(VectorDatabase vectorDB) {
        System.out.println("=== Информация о VectorDatabase ===");
        System.out.println("Количество векторов: " + vectorDB.getVectorCount());
        System.out.println("Количество узлов: " + vectorDB.getTreeNodeCount());
        
        // Пример информации о нескольких векторах
        List<VectorData> sampleVectors = vectorDB.exactSearch("");
        int sampleSize = Math.min(3, sampleVectors.size());
        
        System.out.println("\nПримеры векторов (" + sampleSize + " из " + sampleVectors.size() + "):");
        for (int i = 0; i < sampleSize; i++) {
            VectorData vector = sampleVectors.get(i);
            System.out.printf("  %d. ID: %s%n", i + 1, vector.getId());
            System.out.printf("     Текст: %s%n", 
                vector.getText().substring(0, Math.min(50, vector.getText().length())) + "...");
            System.out.printf("     Путь: %s%n", vector.getNodePath());
            System.out.printf("     Документ: %s%n", vector.getDocumentId());
        }
    }
    
    public static void printChunkerInfo(SemanticChunker chunker) {
        System.out.println("\n=== Информация о SemanticChunker ===");
        System.out.println("Конфигурация: " + chunker.getConfigInfo());
        System.out.println("Модель эмбеддингов: " + chunker.getEmbeddingModel());
        System.out.println("URL Ollama: " + chunker.getOllamaBaseUrl());
        System.out.println("Текущий порог: " + chunker.getSimilarityThreshold());
    }
    
    public static void testEmbeddingGeneration(SemanticChunker chunker) throws Exception {
        System.out.println("\n=== Тест генерации эмбеддингов ===");
        
        String[] testTexts = {
            "привет мир",
            "искусственный интеллект",
            "машинное обучение"
        };
        
        for (String text : testTexts) {
            float[] embedding = chunker.getEmbedding(text);
            System.out.printf("Текст: '%s' -> Размерность эмбеддинга: %d%n",
                text, embedding.length);
        }
    }
    
    public static void main(String[] args) throws Exception {
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.8
        );
        VectorDatabase vectorDB = new VectorDatabase("./data/debug_demo", chunker);
        
        // Загрузка демо данных
        vectorDB.storeTextWithChunking(
            "Тестовый текст для отладки и демонстрации возможностей системы",
            "debug_doc",
            new Object[]{"debug", "test"}
        );
        
        // Запуск утилит
        printVectorDBInfo(vectorDB);
        printChunkerInfo(chunker);
        testEmbeddingGeneration(chunker);
        
        vectorDB.close();
    }
}
```

### Миграция данных

```java
public class DataMigration {
    
    public static void migrateData(VectorDatabase sourceDB, VectorDatabase targetDB) {
        System.out.println("=== Миграция данных ===");
        
        try {
            // Получение всех векторов из source
            List<VectorData> allVectors = sourceDB.exactSearch("");
            System.out.println("Найдено векторов для миграции: " + allVectors.size());
            
            // Миграция векторов
            int migratedCount = 0;
            for (VectorData vector : allVectors) {
                targetDB.storeVectorData(vector);
                migratedCount++;
            }
            
            System.out.println("Успешно мигрировано векторов: " + migratedCount);
            
            // Сохранение целевой базы
            targetDB.saveDatabase();
            System.out.println("Миграция завершена успешно");
            
        } catch (Exception e) {
            System.err.println("Ошибка миграции: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) throws Exception {
        SemanticChunker chunker = new SemanticChunker(
            "http://localhost:11434", "all-minilm:22m", 0.8
        );
        
        // Исходная база данных
        VectorDatabase sourceDB = new VectorDatabase("./data/source_db", chunker);
        
        // Целевая база данных
        VectorDatabase targetDB = new VectorDatabase("./data/target_db", chunker);
        
        // Загрузка демо данных в source
        sourceDB.storeTextWithChunking(
            "Данные для миграции в новую базу данных",
            "migration_doc",
            new Object[]{"migration", "test"}
        );
        
        // Выполнение миграции
        migrateData(sourceDB, targetDB);
        
        sourceDB.close();
        targetDB.close();
    }
}
```

## Заключение

Эти примеры демонстрируют полный спектр возможностей VectorDB - от базовых операций до продвинутых сценариев использования. Вы можете использовать эти примеры как основу для построения собственных приложений с семантическим поиском и RAG-функциональностью.

Для начала рекомендуется:
1. Запустить примеры из раздела "Быстрый старт"
2. Изучить базовые операции с VectorDatabase и TreeNode
3. Поэкспериментировать с различными настройками SemanticChunker
4. Построить простое RAG-приложение с использованием OllamaKnowledgeClient

Все примеры готовы к запуску и требуют только наличия запущенного Ollama сервера с соответствующими моделями.
```
