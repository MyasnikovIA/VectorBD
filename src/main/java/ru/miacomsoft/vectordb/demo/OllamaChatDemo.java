package ru.miacomsoft.vectordb.demo;

import ru.miacomsoft.vectordb.core.BinaryVectorDatabase;
import ru.miacomsoft.vectordb.core.SemanticChunker;

import ru.miacomsoft.vectordb.knowledge.KnowledgeConfig;
import ru.miacomsoft.vectordb.knowledge.OllamaKnowledgeClient;

import java.util.List;

/**
 * Демонстрация интерактивного чата с использованием знаний
 */
public class OllamaChatDemo {
    public static void main(String[] args) {
        // Создание конфигурации знаний
        KnowledgeConfig knowledgeConfig = new KnowledgeConfig(
                "http://localhost:11434",
                "deepseek-v3.1:671b-cloud",
                0.7,
                true,
                true
        );

        // Инициализация VectorDatabase
        SemanticChunker semanticChunker = new SemanticChunker(
                knowledgeConfig.getOllamaUrl(),
                "all-minilm:22m",
                knowledgeConfig.getSimilarityThreshold()
        );
        BinaryVectorDatabase vectorDB = new BinaryVectorDatabase("./data/chat_demo", semanticChunker);

        OllamaKnowledgeClient chatClient = new OllamaKnowledgeClient(vectorDB, knowledgeConfig);

        try {
            // Проверка доступности Ollama
            if (!chatClient.getOllamaClient().isServerAvailable()) {
                System.out.println("❌ Ollama server is not available at: " +
                        chatClient.getOllamaClient().getOllamaUrl());
                System.out.println("Please make sure Ollama is running and try again.");
                return;
            }

            // Показать доступные модели
            System.out.println("Checking available models...");
            List<String> availableModels = chatClient.getOllamaClient().getAvailableModels();
            System.out.println("Available models:");
            for (String model : availableModels) {
                System.out.println("  - " + model);
            }

            // Автоматически выбрать доступную модель
            String selectedModel = selectAvailableModel(availableModels);
            if (selectedModel == null) {
                System.out.println("❌ No suitable models found. Please install at least one model in Ollama.");
                System.out.println("You can install a model using: ollama pull deepseek-v3.1:671b-cloud");
                return;
            }

            chatClient.setDefaultModel(selectedModel);
            System.out.println("✅ Using model: " + selectedModel);

            // Настройка параметров
            chatClient.setSimilarityThreshold(0.7);
            chatClient.setMaxContextResults(3);

            // Загрузка начальных знаний
            String initialFacts = """
                Искусственный интеллект - это область компьютерных наук, 
                которая занимается созданием машин, способных выполнять задачи, 
                требующие человеческого интеллекта.
                
                Машинное обучение является подразделом искусственного интеллекта 
                и фокусируется на разработке алгоритмов, которые могут обучаться на данных.
                
                Глубокое обучение использует нейронные сети с множеством слоев 
                для изучения сложных паттернов в данных.
                
                ИИ (искусственный интеллект) - это способность машин имитировать 
                человеческий интеллект и выполнять задачи, которые обычно требуют 
                человеческого мышления.
                """;

            // Добавляем факты через векторную базу
            try {
                vectorDB.storeTextWithChunking(
                        initialFacts,
                        "AIKnowledge",
                        new Object[]{"knowledge", "ai"}
                );
                System.out.println("Initial knowledge loaded successfully");
            } catch (Exception e) {
                System.out.println("Error loading initial knowledge: " + e.getMessage());
            }

            // Запуск интерактивного чата
            chatClient.startInteractiveChat();

        } catch (Exception e) {
            System.out.println("Demo error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            vectorDB.close();
        }
    }

    /**
     * Выбор доступной модели из списка
     */
    private static String selectAvailableModel(List<String> availableModels) {
        // Приоритетный список моделей (от наиболее предпочтительных к менее)
        String[] preferredModels = {
                "deepseek-v3.1:671b-cloud", "llama3.2", "llama3.1", "llama3",
                "mistral", "gemma", "qwen", "phi3"
        };

        for (String preferred : preferredModels) {
            for (String available : availableModels) {
                if (available.toLowerCase().contains(preferred.toLowerCase())) {
                    return available;
                }
            }
        }

        // Если не найдено предпочтительных моделей, вернуть первую доступную
        return availableModels.isEmpty() ? null : availableModels.get(0);
    }
}