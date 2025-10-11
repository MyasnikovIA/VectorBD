package ru.miacomsoft.vectordb.demo;

import ru.miacomsoft.vectordb.core.BinaryVectorDatabase;
import ru.miacomsoft.vectordb.core.SemanticChunker;
import ru.miacomsoft.vectordb.core.BinaryVectorData;
import ru.miacomsoft.vectordb.knowledge.KnowledgeConfig;
import ru.miacomsoft.vectordb.knowledge.KnowledgeLoader;
import ru.miacomsoft.vectordb.knowledge.PromptGenerator;

/**
 * Демонстрация работы PromptGenerator для создания AI промптов
 * с использованием семантического поиска в бинарной базе знаний
 */
public class PromptGeneratorDemo {
    public static void main(String[] args) {
        // Создание конфигурации знаний
        KnowledgeConfig knowledgeConfig = new KnowledgeConfig(
                "http://localhost:11434",
                "deepseek-v3.1:671b-cloud",
                0.8,
                true,
                true
        );

        // Инициализация бинарной VectorDatabase
        SemanticChunker semanticChunker = new SemanticChunker(
                knowledgeConfig.getOllamaUrl(),
                "all-minilm:22m",
                knowledgeConfig.getSimilarityThreshold()
        );
        BinaryVectorDatabase vectorDB = new BinaryVectorDatabase("./data/binary_prompt_demo", semanticChunker);

        System.out.println("=== Binary PromptGenerator Demo ===");

        try {
            // Создаем генератор промптов для бинарной БД
            PromptGenerator promptGenerator = new PromptGenerator(vectorDB, knowledgeConfig);

            // Загружаем демонстрационные знания в бинарную БД
            loadDemoKnowledge(vectorDB, knowledgeConfig);

            // Демонстрация 1: Контекстный промпт для ответа на вопрос из бинарной БД
            demonstrateContextPrompt(promptGenerator);

            // Демонстрация 2: Генерация вопросов из бинарной БД
            demonstrateQuestionGeneration(promptGenerator);

            // Демонстрация 3: Суммаризация из бинарной БД
            demonstrateSummarization(promptGenerator);

            // Демонстрация 4: Различные настройки порога схожести в бинарной БД
            demonstrateSimilarityThresholds(promptGenerator);

        } catch (Exception e) {
            System.out.println("Binary demo error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            vectorDB.close();
        }

        System.out.println("\n=== Binary Demo completed ===");
    }

    /**
     * Демонстрация создания контекстного промпта из бинарной БД
     */
    private static void demonstrateContextPrompt(PromptGenerator promptGenerator) throws Exception {
        System.out.println("\n1. Binary Context Prompt Generation Demo");
        System.out.println("========================================");

        String userQuery = "Какие существуют типы машинного обучения и их применение?";

        System.out.println("User query: " + userQuery);
        System.out.println("Using binary database for context retrieval");

        // Создаем промпт с контекстом из бинарной БД
        String contextPrompt = promptGenerator.createContextPrompt(
                userQuery,
                3,      // maxResultsPerChunk
                0.7     // similarityThreshold
        );

        System.out.println("\nGenerated Context Prompt (from binary DB):");
        System.out.println("------------------------------------------");
        System.out.println(contextPrompt);
    }

    /**
     * Демонстрация генерации вопросов из бинарной БД
     */
    private static void demonstrateQuestionGeneration(PromptGenerator promptGenerator) throws Exception {
        System.out.println("\n2. Binary Question Generation Demo");
        System.out.println("===================================");

        String topic = "нейронные сети";
        int numQuestions = 5;

        System.out.println("Topic: " + topic);
        System.out.println("Number of questions to generate: " + numQuestions);
        System.out.println("Using binary database for context");

        String questionPrompt = promptGenerator.createQuestionGenerationPrompt(
                topic,
                numQuestions,
                0.6  // similarityThreshold
        );

        System.out.println("\nGenerated Question Prompt (from binary DB):");
        System.out.println("-------------------------------------------");
        System.out.println(questionPrompt);
    }

    /**
     * Демонстрация суммаризации из бинарной БД
     */
    private static void demonstrateSummarization(PromptGenerator promptGenerator) throws Exception {
        System.out.println("\n3. Binary Summarization Demo");
        System.out.println("=============================");

        String focusTopic = "базы данных";
        int maxContextItems = 4;

        System.out.println("Focus topic: " + focusTopic);
        System.out.println("Max context items: " + maxContextItems);
        System.out.println("Using binary database for summarization");

        String summarizationPrompt = promptGenerator.createSummarizationPrompt(
                focusTopic,
                maxContextItems,
                0.65  // similarityThreshold
        );

        System.out.println("\nGenerated Summarization Prompt (from binary DB):");
        System.out.println("------------------------------------------------");
        System.out.println(summarizationPrompt);
    }

    /**
     * Демонстрация влияния порога схожести в бинарной БД
     */
    private static void demonstrateSimilarityThresholds(PromptGenerator promptGenerator) throws Exception {
        System.out.println("\n4. Binary Similarity Threshold Comparison Demo");
        System.out.println("==============================================");

        String query = "обработка естественного языка";

        System.out.println("Query: " + query);
        System.out.println("Comparing different similarity thresholds in binary database:");

        // Тестируем разные пороги схожести в бинарной БД
        double[] thresholds = {0.5, 0.7, 0.8, 0.9};

        for (double threshold : thresholds) {
            System.out.println("\n--- Binary DB Threshold: " + threshold + " ---");

            try {
                String prompt = promptGenerator.createContextPrompt(
                        query,
                        3,  // maxResultsPerChunk
                        threshold
                );

                // Подсчитываем количество найденных документов по количеству разделов
                int docCount = countDocumentsInPrompt(prompt);
                System.out.println("Found documents in binary DB: " + docCount);
                System.out.println("Prompt length: " + prompt.length() + " characters");

            } catch (Exception e) {
                System.out.println("Error with threshold " + threshold + ": " + e.getMessage());
            }
        }
    }

    /**
     * Загрузка демонстрационных знаний в бинарную БД
     */
    private static void loadDemoKnowledge(BinaryVectorDatabase vectorDB, KnowledgeConfig knowledgeConfig) throws Exception {
        System.out.println("\nLoading demo knowledge to binary database...");

        KnowledgeLoader loader = new KnowledgeLoader(vectorDB, knowledgeConfig);

        // Тексты о машинном обучении
        String aiText = """
            Машинное обучение - это область искусственного интеллекта, которая focuses на разработке алгоритмов и моделей, 
            способных обучаться на данных и делать прогнозы или принимать решения без явного программирования.
            
            Основные типы машинного обучения:
            1. Обучение с учителем (Supervised Learning) - алгоритм обучается на размеченных данных
            2. Обучение без учителя (Unsupervised Learning) - алгоритм находит паттерны в неразмеченных данных
            3. Обучение с подкреплением (Reinforcement Learning) - агент учится через взаимодействие со средой
            
            Глубокое обучение является подразделом машинного обучения, которое использует искусственные нейронные сети 
            с множеством слоев. Эти сети могут изучать сложные паттерны в больших объемах данных.
            
            Обработка естественного языка (NLP) позволяет компьютерам понимать, интерпретировать и генерировать человеческий язык. 
            Современные NLP модели основаны на глубоком обучении и трансформерах.
            """;

        // Тексты о технологиях
        String techText = """
            Базы данных - это организованные коллекции данных, которые хранят и управляют информацией.
            Реляционные базы данных используют таблицы для хранения данных и SQL для запросов.
            NoSQL базы данных включают документные, ключ-значение, колоночные и графовые базы данных.
            
            Векторные базы данных используются для семантического поиска и хранения embedding векторов.
            Они позволяют находить похожие объекты по их векторным представлениям.
            
            Облачные вычисления предоставляют вычислительные ресурсы как услугу через интернет.
            Основные модели: IaaS, PaaS, SaaS.
            """;

        // Загружаем знания в бинарную БД
        int aiChunks = loader.loadText(aiText, "AIKnowledge", new Object[]{"knowledge", "ai"}, 500, "Binary AI Demo");
        int techChunks = loader.loadText(techText, "TechKnowledge", new Object[]{"knowledge", "tech"}, 500, "Binary Tech Demo");

        System.out.println("Loaded " + aiChunks + " AI knowledge chunks to binary DB");
        System.out.println("Loaded " + techChunks + " technology knowledge chunks to binary DB");

        // Показываем статистику бинарной БД
        System.out.println("\nBinary Knowledge Base Statistics:");
        System.out.println("Total vectors: " + vectorDB.getVectorCount());
        System.out.println("Database type: Binary serialization");
        System.out.println("Storage: binary_vectordb.dat");
    }

    /**
     * Вспомогательный метод для подсчета документов в промпте
     */
    private static int countDocumentsInPrompt(String prompt) {
        int count = 0;
        int index = 0;

        while ((index = prompt.indexOf("[Документ", index)) != -1) {
            count++;
            index += "[Документ".length();
        }

        return count;
    }
}