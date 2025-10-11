package ru.miacomsoft.vectordb.demo;

import ru.miacomsoft.vectordb.core.SemanticChunker;
import ru.miacomsoft.vectordb.core.VectorDatabase;
import ru.miacomsoft.vectordb.knowledge.KnowledgeConfig;
import ru.miacomsoft.vectordb.knowledge.KnowledgeLoader;
import ru.miacomsoft.vectordb.knowledge.OllamaKnowledgeClient;

public class KnowledgeExample {
    public static void main(String[] args) {
        try {
            // Конфигурация Knowledge системы
            KnowledgeConfig knowledgeConfig = new KnowledgeConfig(
                    "http://localhost:11434",
                    "llama3.2",
                    0.8,
                    true,
                    true
            );

            // Инициализация SemanticChunker
            SemanticChunker semanticChunker = new SemanticChunker(
                    "http://localhost:11434",
                    "all-minilm:22m",
                    0.8
            );

            // Создание векторной базы данных
            VectorDatabase vectorDB = new VectorDatabase("./data/knowledge_db", semanticChunker);

            // Создание KnowledgeLoader
            KnowledgeLoader knowledgeLoader = new KnowledgeLoader(vectorDB, knowledgeConfig);

            // Загрузка знаний из текста
            String knowledgeText = """
                Машинное обучение - это раздел искусственного интеллекта, 
                который позволяет компьютерам обучаться на данных без явного программирования. 
                Глубокое обучение использует нейронные сети с множеством слоев. 
                Обработка естественного языка позволяет компьютерам понимать человеческий язык.
                Векторные базы данных хранят данные в виде векторных эмбеддингов.
                Семантический поиск находит документы по смысловому сходству.
                """;

            knowledgeLoader.loadText(
                    knowledgeText,
                    "ai_knowledge_base",
                    new Object[]{"knowledge", "ai"},
                    1000,
                    "manual_input"
            );

            // Создание клиента для работы с знаниями
            OllamaKnowledgeClient knowledgeClient = new OllamaKnowledgeClient(vectorDB, knowledgeConfig);

            // Демонстрация поиска релевантных фактов
            System.out.println("=== Поиск релевантных фактов ===");
            var facts = knowledgeClient.findRelevantFacts("что такое машинное обучение", 3);
            for (String fact : facts) {
                System.out.println("- " + fact);
            }

            // Демонстрация генерации ответа с использованием знаний
            System.out.println("\n=== Генерация ответа с знаниями ===");
            String response = knowledgeClient.generateResponseWithKnowledge("Объясни что такое глубокое обучение");
            System.out.println("Ответ: " + response);

            // Статистика
            System.out.println("\n=== Статистика знаний ===");
            knowledgeLoader.printKnowledgeStats();

            // Интерактивный чат (раскомментируйте для использования)
            // knowledgeClient.startInteractiveChat();

            // Сохранение и закрытие
            vectorDB.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}