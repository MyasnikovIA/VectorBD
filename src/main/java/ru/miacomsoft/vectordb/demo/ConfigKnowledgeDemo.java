package ru.miacomsoft.vectordb.demo;

import ru.miacomsoft.vectordb.core.SemanticChunker;
import ru.miacomsoft.vectordb.core.VectorDatabase;
import ru.miacomsoft.vectordb.knowledge.KnowledgeConfig;
import ru.miacomsoft.vectordb.knowledge.KnowledgeLoader;
import ru.miacomsoft.vectordb.knowledge.OllamaKnowledgeClient;

/**
 * Демонстрация конфигурации Knowledge системы
 */
public class ConfigKnowledgeDemo {
    public static void main(String[] args) {
        System.out.println("=== Knowledge Configuration Demo ===");

        // 1. Создание KnowledgeConfig с разными способами
        demoKnowledgeConfigCreation();

        // 2. Использование KnowledgeConfig в VectorDatabase
        demoVectorDatabaseWithConfig();

        // 3. Настройка Knowledge из конфигурации
        demoKnowledgeConfiguration();

        // 4. Расширенная конфигурация
        demoAdvancedConfiguration();
    }

    private static void demoKnowledgeConfigCreation() {
        System.out.println("\n1. KnowledgeConfig Creation Methods:");

        // Способ 1: Стандартный конструктор
        KnowledgeConfig config1 = new KnowledgeConfig(
                "http://localhost:11434",
                "llama3.2",
                0.8,
                true,
                true
        );
        System.out.println("Standard KnowledgeConfig - Enabled: " + config1.isEnabled());

        // Способ 2: С минимальными параметрами
        KnowledgeConfig config2 = new KnowledgeConfig(
                "http://localhost:11434",
                "deepseek-v3.1:671b-cloud",
                0.7,
                false,
                true
        );
        System.out.println("Custom KnowledgeConfig - Model: " + config2.getModel());

        // Проверка валидности
        System.out.println("Config 1 valid: " + config1.isValid());
        System.out.println("Config 2 valid: " + config2.isValid());
    }

    private static void demoVectorDatabaseWithConfig() {
        System.out.println("\n2. VectorDatabase with KnowledgeConfig:");

        // Создание кастомного KnowledgeConfig
        KnowledgeConfig knowledgeConfig = new KnowledgeConfig(
                "http://localhost:11434",
                "llama3.2",
                0.8,
                true,
                true
        );

        // Создание VectorDatabase
        SemanticChunker semanticChunker = new SemanticChunker(
                knowledgeConfig.getOllamaUrl(),
                "all-minilm:22m",
                knowledgeConfig.getSimilarityThreshold()
        );
        VectorDatabase vectorDB = new VectorDatabase("./data/knowledge_db", semanticChunker);

        // Создание KnowledgeLoader с конфигурацией
        KnowledgeLoader knowledgeLoader = new KnowledgeLoader(vectorDB, knowledgeConfig);

        // Получение конфигурации Knowledge
        var knowledgeConfigMap = knowledgeLoader.getKnowledgeConfig();
        System.out.println("Knowledge configuration: " + knowledgeConfigMap);
    }

    private static void demoKnowledgeConfiguration() {
        System.out.println("\n3. Knowledge Configuration:");

        // Создание VectorDatabase
        SemanticChunker semanticChunker = new SemanticChunker(
                "http://localhost:11434",
                "all-minilm:22m",
                0.8
        );
        VectorDatabase vectorDB = new VectorDatabase("./data/knowledge_db", semanticChunker);

        KnowledgeConfig knowledgeConfig = new KnowledgeConfig(
                "http://localhost:11434",
                "llama3.2",
                0.8,
                true,
                true
        );

        KnowledgeLoader knowledgeLoader = new KnowledgeLoader(vectorDB, knowledgeConfig);
        OllamaKnowledgeClient knowledgeClient = new OllamaKnowledgeClient(vectorDB, knowledgeConfig);

        // Получение конфигурации Knowledge
        var knowledgeConfigMap = knowledgeLoader.getKnowledgeConfig();
        System.out.println("KnowledgeLoader config: " + knowledgeConfigMap);

        // Использование KnowledgeConfig класса
        KnowledgeConfig config = knowledgeClient.getKnowledgeConfig();
        config.printConfig();

        System.out.println("Configuration valid: " + config.isValid());
    }

    private static void demoAdvancedConfiguration() {
        System.out.println("\n4. Advanced Configuration Scenarios:");

        // Сценарий 1: Динамическое изменение конфигурации
        KnowledgeConfig dynamicConfig = new KnowledgeConfig(
                "http://localhost:11434",
                "llama3.2",
                0.8,
                true,
                true
        );

        SemanticChunker semanticChunker = new SemanticChunker(
                dynamicConfig.getOllamaUrl(),
                "all-minilm:22m",
                dynamicConfig.getSimilarityThreshold()
        );
        VectorDatabase vectorDB = new VectorDatabase("./data/knowledge_db", semanticChunker);

        // Сценарий 2: Валидация конфигурации
        KnowledgeConfig knowledgeConfig = new KnowledgeConfig(
                "http://localhost:11434",
                "llama3.2",
                0.8,
                true,
                true
        );
        if (!knowledgeConfig.isValid()) {
            System.out.println("Warning: Knowledge configuration is invalid!");
        }

        // Сценарий 3: Использование разных конфигураций для разных баз данных
        KnowledgeConfig config1 = new KnowledgeConfig(
                "http://localhost:11434",
                "llama3.2",
                0.8,
                true,
                true
        );

        KnowledgeConfig config2 = new KnowledgeConfig(
                "http://localhost:11434",
                "deepseek-v3.1:671b-cloud",
                0.6,
                false,
                true
        );

        System.out.println("Database 1 Knowledge model: " + config1.getModel());
        System.out.println("Database 2 Knowledge model: " + config2.getModel());
        System.out.println("Database 1 Similarity threshold: " + config1.getSimilarityThreshold());
        System.out.println("Database 2 Similarity threshold: " + config2.getSimilarityThreshold());
    }
}