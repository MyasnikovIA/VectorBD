package ru.miacomsoft.vectordb.demo;

import ru.miacomsoft.vectordb.core.SemanticChunker;
import ru.miacomsoft.vectordb.core.VectorDatabase;
import ru.miacomsoft.vectordb.knowledge.KnowledgeConfig;
import ru.miacomsoft.vectordb.knowledge.KnowledgeLoader;
import ru.miacomsoft.vectordb.knowledge.PromptGenerator;

/**
 * Демонстрация работы PromptGenerator для создания AI промптов
 * с использованием семантического поиска в базе знаний
 */
public class PromptGeneratorDemo {
    public static void main(String[] args) {
        // Создание конфигурации знаний
        KnowledgeConfig knowledgeConfig = new KnowledgeConfig(
                "http://localhost:11434",
                "llama3.2",
                0.8,
                true,
                true
        );

        // Инициализация VectorDatabase
        SemanticChunker semanticChunker = new SemanticChunker(
                knowledgeConfig.getOllamaUrl(),
                "all-minilm:22m",
                knowledgeConfig.getSimilarityThreshold()
        );
        VectorDatabase vectorDB = new VectorDatabase("./data/prompt_demo", semanticChunker);

        System.out.println("=== PromptGenerator Demo ===");

        try {
            // Создаем генератор промптов
            PromptGenerator promptGenerator = new PromptGenerator(vectorDB, knowledgeConfig);

            // Загружаем демонстрационные знания
            loadDemoKnowledge(vectorDB, knowledgeConfig);

            // Демонстрация 1: Контекстный промпт для ответа на вопрос
            demonstrateContextPrompt(promptGenerator);

            // Демонстрация 2: Генерация вопросов
            demonstrateQuestionGeneration(promptGenerator);

            // Демонстрация 3: Суммаризация
            demonstrateSummarization(promptGenerator);

            // Демонстрация 4: Различные настройки порога схожести
            demonstrateSimilarityThresholds(promptGenerator);

        } catch (Exception e) {
            System.out.println("Demo error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            vectorDB.close();
        }

        System.out.println("\n=== Demo completed ===");
    }

    /**
     * Демонстрация создания контекстного промпта
     */
    private static void demonstrateContextPrompt(PromptGenerator promptGenerator) throws Exception {
        System.out.println("\n1. Context Prompt Generation Demo");
        System.out.println("=================================");

        String userQuery = "Какие существуют типы машинного обучения и их применение?";

        System.out.println("User query: " + userQuery);

        // Создаем промпт с контекстом
        String contextPrompt = promptGenerator.createContextPrompt(
                userQuery,
                3,      // maxResultsPerChunk
                0.7     // similarityThreshold
        );

        System.out.println("\nGenerated Context Prompt:");
        System.out.println("------------------------");
        System.out.println(contextPrompt);
    }

    /**
     * Демонстрация генерации вопросов
     */
    private static void demonstrateQuestionGeneration(PromptGenerator promptGenerator) throws Exception {
        System.out.println("\n2. Question Generation Demo");
        System.out.println("============================");

        String topic = "нейронные сети";
        int numQuestions = 5;

        System.out.println("Topic: " + topic);
        System.out.println("Number of questions to generate: " + numQuestions);

        String questionPrompt = promptGenerator.createQuestionGenerationPrompt(
                topic,
                numQuestions,
                0.6  // similarityThreshold
        );

        System.out.println("\nGenerated Question Prompt:");
        System.out.println("-------------------------");
        System.out.println(questionPrompt);
    }

    /**
     * Демонстрация суммаризации
     */
    private static void demonstrateSummarization(PromptGenerator promptGenerator) throws Exception {
        System.out.println("\n3. Summarization Demo");
        System.out.println("======================");

        String focusTopic = "базы данных";
        int maxContextItems = 4;

        System.out.println("Focus topic: " + focusTopic);
        System.out.println("Max context items: " + maxContextItems);

        String summarizationPrompt = promptGenerator.createSummarizationPrompt(
                focusTopic,
                maxContextItems,
                0.65  // similarityThreshold
        );

        System.out.println("\nGenerated Summarization Prompt:");
        System.out.println("------------------------------");
        System.out.println(summarizationPrompt);
    }

    /**
     * Демонстрация влияния порога схожести
     */
    private static void demonstrateSimilarityThresholds(PromptGenerator promptGenerator) throws Exception {
        System.out.println("\n4. Similarity Threshold Comparison Demo");
        System.out.println("=======================================");

        String query = "обработка естественного языка";

        System.out.println("Query: " + query);
        System.out.println("Comparing different similarity thresholds:");

        // Тестируем разные пороги схожести
        double[] thresholds = {0.5, 0.7, 0.8, 0.9};

        for (double threshold : thresholds) {
            System.out.println("\n--- Threshold: " + threshold + " ---");

            try {
                String prompt = promptGenerator.createContextPrompt(
                        query,
                        3,  // maxResultsPerChunk
                        threshold
                );

                // Подсчитываем количество найденных документов по количеству разделов
                int docCount = countDocumentsInPrompt(prompt);
                System.out.println("Found documents: " + docCount);
                System.out.println("Prompt length: " + prompt.length() + " characters");

            } catch (Exception e) {
                System.out.println("Error with threshold " + threshold + ": " + e.getMessage());
            }
        }
    }

    /**
     * Загрузка демонстрационных знаний
     */
    private static void loadDemoKnowledge(VectorDatabase vectorDB, KnowledgeConfig knowledgeConfig) throws Exception {
        System.out.println("\nLoading demo knowledge...");

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

        // Загружаем знания
        int aiChunks = loader.loadText(aiText, "AIKnowledge", new Object[]{"knowledge", "ai"}, 500, "AI Demo");
        int techChunks = loader.loadText(techText, "TechKnowledge", new Object[]{"knowledge", "tech"}, 500, "Tech Demo");

        System.out.println("Loaded " + aiChunks + " AI knowledge chunks");
        System.out.println("Loaded " + techChunks + " technology knowledge chunks");

        // Показываем статистику
        System.out.println("\nKnowledge Base Statistics:");
        System.out.println("Total vectors: " + vectorDB.getVectorCount());
        System.out.println("Total tree nodes: " + vectorDB.getTreeNodeCount());
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