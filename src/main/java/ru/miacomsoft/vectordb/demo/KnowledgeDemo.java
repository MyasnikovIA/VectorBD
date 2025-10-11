package ru.miacomsoft.vectordb.demo;

import ru.miacomsoft.vectordb.core.SemanticChunker;
import ru.miacomsoft.vectordb.core.BinaryVectorDatabase;
import ru.miacomsoft.vectordb.core.VectorSearchResult;
import ru.miacomsoft.vectordb.knowledge.KnowledgeConfig;
import ru.miacomsoft.vectordb.knowledge.KnowledgeLoader;

import java.util.List;

/**
 * Демонстрация загрузки и использования текстовых знаний в бинарной БД
 */
public class KnowledgeDemo {
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
        BinaryVectorDatabase vectorDB = new BinaryVectorDatabase("./data/binary_knowledge_demo", semanticChunker);

        KnowledgeLoader loader = new KnowledgeLoader(vectorDB, knowledgeConfig);

        System.out.println("=== Binary Knowledge Loading Demo ===");

        try {
            // Пример текста для загрузки в бинарную БД
            String sampleText = """
                Машинное обучение - это область искусственного интеллекта, 
                которая focuses на разработке алгоритмов и моделей, способных 
                обучаться на данных и делать прогнозы или принимать решения 
                без явного программирования.
                
                Глубокое обучение является подразделом машинного обучения, 
                которое использует искусственные нейронные сети с множеством слоев. 
                Эти сети могут изучать сложные паттерны в больших объемах данных.
                
                Обработка естественного языка (NLP) позволяет компьютерам 
                понимать, интерпретировать и генерировать человеческий язык. 
                Современные NLP модели основаны на глубоком обучении.
                """;

            // Новые возможности загрузки в бинарную БД
            System.out.println("=== Binary Loading Features ===");
            System.out.println("Current similarity threshold: " + loader.getCurrentSimilarityThreshold());
            System.out.println("SemanticChunker config: " + loader.getSemanticChunkerConfig());
            System.out.println("Database type: Binary");

            // Загрузка знаний в бинарную БД с текущим порогом схожести
            System.out.println("\n1. Loading sample text to binary database with current similarity threshold...");
            int chunks = loader.loadText(sampleText, "AIKnowledge", new Object[]{"knowledge", "ai"}, 500, "binary_demo");
            System.out.println("Created " + chunks + " semantic chunks in binary database");

            // Показываем статистику бинарной БД
            System.out.println("\n2. Binary knowledge statistics:");
            loader.printKnowledgeStats();

            // Демонстрация изменения порога схожести для бинарной БД
            System.out.println("\n3. Adjusting similarity threshold for binary DB...");
            double oldThreshold = loader.getCurrentSimilarityThreshold();
            loader.setSimilarityThreshold(0.9); // Более строгий порог
            System.out.println("Changed threshold from " + oldThreshold + " to " + loader.getCurrentSimilarityThreshold());

            // Загрузка с новым порогом в бинарную БД
            String additionalText = """
                Рекуррентные нейронные сети (RNN) особенно эффективны 
                для обработки последовательных данных, таких как текст и временные ряды.
                """;

            int additionalChunks = loader.loadText(additionalText, "AIKnowledge",
                    new Object[]{"knowledge", "ai", "additional"}, 500, "binary_additional");
            System.out.println("Created " + additionalChunks + " additional chunks in binary database with new threshold");

            // Показать обновленную статистику бинарной БД
            System.out.println("\n4. Updated binary knowledge statistics:");
            loader.printKnowledgeStats();

            // Демонстрация работы с конфигурацией SemanticChunker для бинарной БД
            System.out.println("\n5. Binary SemanticChunker configuration demo:");
            String chunkerConfig = loader.getSemanticChunkerConfig();
            System.out.println("Current SemanticChunker config: " + chunkerConfig);

            // Тестирование поиска в бинарной БД
            System.out.println("\n6. Testing binary database search:");
            try {
                List<VectorSearchResult> results = vectorDB.similaritySearch("нейронные сети", 2);
                System.out.println("Found " + results.size() + " results in binary database");
                for (VectorSearchResult result : results) {
                    System.out.printf("  - Similarity: %.3f | Text: %s\n",
                            result.getSimilarity(),
                            result.getVectorData().getText().substring(0, Math.min(60, result.getVectorData().getText().length())) + "...");
                }
            } catch (Exception e) {
                System.out.println("Search test error: " + e.getMessage());
            }

        } catch (Exception e) {
            System.out.println("Binary demo error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            vectorDB.close();
        }

        System.out.println("\n=== Binary Knowledge Demo completed ===");
    }
}