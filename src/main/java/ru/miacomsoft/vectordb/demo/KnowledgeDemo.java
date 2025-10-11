package ru.miacomsoft.vectordb.demo;

import ru.miacomsoft.vectordb.core.SemanticChunker;
import ru.miacomsoft.vectordb.core.VectorDatabase;
import ru.miacomsoft.vectordb.knowledge.KnowledgeConfig;
import ru.miacomsoft.vectordb.knowledge.KnowledgeLoader;

/**
 * Демонстрация загрузки и использования текстовых знаний
 */
public class KnowledgeDemo {
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
        VectorDatabase vectorDB = new VectorDatabase("./data/knowledge_demo", semanticChunker);

        KnowledgeLoader loader = new KnowledgeLoader(vectorDB, knowledgeConfig);

        System.out.println("=== Knowledge Loading Demo ===");

        try {
            // Пример текста для загрузки
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

            // Новые возможности загрузки
            System.out.println("=== New Loading Features ===");
            System.out.println("Current similarity threshold: " + loader.getCurrentSimilarityThreshold());
            System.out.println("SemanticChunker config: " + loader.getSemanticChunkerConfig());

            // Загрузка знаний с текущим порогом схожести
            System.out.println("\n1. Loading sample text with current similarity threshold...");
            int chunks = loader.loadText(sampleText, "AIKnowledge", new Object[]{"knowledge", "ai"}, 500, "demo");
            System.out.println("Created " + chunks + " semantic chunks");

            // Показываем статистику с информацией о пороге схожести
            System.out.println("\n2. Knowledge statistics with similarity threshold info:");
            loader.printKnowledgeStats();

            // Демонстрация изменения порога схожести
            System.out.println("\n3. Adjusting similarity threshold...");
            double oldThreshold = loader.getCurrentSimilarityThreshold();
            loader.setSimilarityThreshold(0.9); // Более строгий порог
            System.out.println("Changed threshold from " + oldThreshold + " to " + loader.getCurrentSimilarityThreshold());

            // Загрузка с новым порогом
            String additionalText = """
                Рекуррентные нейронные сети (RNN) особенно эффективны 
                для обработки последовательных данных, таких как текст и временные ряды.
                """;

            int additionalChunks = loader.loadText(additionalText, "AIKnowledge",
                    new Object[]{"knowledge", "ai", "additional"}, 500, "additional");
            System.out.println("Created " + additionalChunks + " additional chunks with new threshold");

            // Показать обновленную статистику
            System.out.println("\n4. Updated knowledge statistics:");
            loader.printKnowledgeStats();

            // Демонстрация работы с конфигурацией SemanticChunker
            System.out.println("\n5. SemanticChunker configuration demo:");
            String chunkerConfig = loader.getSemanticChunkerConfig();
            System.out.println("Current SemanticChunker config: " + chunkerConfig);

        } catch (Exception e) {
            System.out.println("Demo error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            vectorDB.close();
        }

        System.out.println("\n=== Demo completed ===");
    }
}