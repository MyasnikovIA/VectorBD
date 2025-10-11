package ru.miacomsoft.vectordb.knowledge;

import ru.miacomsoft.vectordb.core.VectorDatabase;
import ru.miacomsoft.vectordb.core.VectorSearchResult;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Клиент для работы с знаниями и Ollama
 */
public class OllamaKnowledgeClient {
    private final VectorDatabase database;
    private final OllamaStreamClient ollamaClient;
    private final KnowledgeConfig knowledgeConfig;
    private String defaultModel = "deepseek-v3.1:671b-cloud";
    private double similarityThreshold = 0.7;
    private int maxContextResults = 3;

    public OllamaKnowledgeClient(VectorDatabase database, KnowledgeConfig knowledgeConfig) {
        this.database = database;
        this.knowledgeConfig = knowledgeConfig;
        this.ollamaClient = new OllamaStreamClient(knowledgeConfig.getOllamaUrl());

        configureFromConfig();
    }

    public OllamaKnowledgeClient(VectorDatabase database, KnowledgeConfig knowledgeConfig, String ollamaUrl) {
        this.database = database;
        this.knowledgeConfig = knowledgeConfig;
        this.ollamaClient = new OllamaStreamClient(ollamaUrl);
        configureFromConfig();
    }

    /**
     * Настройка из конфигурации
     */
    private void configureFromConfig() {
        if (knowledgeConfig.isEnabled()) {
            this.defaultModel = knowledgeConfig.getModel();
            this.similarityThreshold = knowledgeConfig.getSimilarityThreshold();

            System.out.println("OllamaKnowledgeClient configured:");
            System.out.println("  - Model: " + defaultModel);
            System.out.println("  - Similarity Threshold: " + similarityThreshold);
            System.out.println("  - Ollama URL: " + knowledgeConfig.getOllamaUrl());
        } else {
            System.out.println("Knowledge functionality is disabled in configuration");
        }
    }

    /**
     * Получить конфигурацию Knowledge
     */
    public KnowledgeConfig getKnowledgeConfig() {
        return knowledgeConfig;
    }

    /**
     * Получить клиент Ollama
     */
    public OllamaStreamClient getOllamaClient() {
        return ollamaClient;
    }

    /**
     * Установить модель по умолчанию
     */
    public void setDefaultModel(String model) {
        this.defaultModel = model;
    }

    /**
     * Установить порог схожести
     */
    public void setSimilarityThreshold(double threshold) {
        this.similarityThreshold = threshold;
    }

    /**
     * Установить максимальное количество результатов контекста
     */
    public void setMaxContextResults(int maxResults) {
        this.maxContextResults = maxResults;
    }

    /**
     * Поиск релевантных фактов в базе знаний
     */
    public List<String> findRelevantFacts(String query, int maxResults) {
        List<String> relevantFacts = new ArrayList<>();
        try {
            // Поиск в векторной базе данных
            List<VectorSearchResult> searchResults = database.similaritySearch(query, maxResults);

            for (VectorSearchResult result : searchResults) {
                if (result.getSimilarity() >= similarityThreshold) {
                    relevantFacts.add(result.getVectorData().getText());
                }
            }

            // Если результатов мало, добавляем демо-факты
            if (relevantFacts.isEmpty()) {
                relevantFacts = getDemoFacts(query, maxResults);
            }

        } catch (Exception e) {
            System.err.println("Error finding relevant facts: " + e.getMessage());
            relevantFacts = getDemoFacts(query, maxResults);
        }

        return relevantFacts;
    }

    /**
     * Демо-факты для случаев, когда база данных недоступна
     */
    private List<String> getDemoFacts(String query, int maxResults) {
        List<String> demoFacts = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        if (lowerQuery.contains("ии") || lowerQuery.contains("искусственный интеллект")) {
            demoFacts.add("Искусственный интеллект - это область компьютерных наук, которая занимается созданием машин, способных выполнять задачи, требующие человеческого интеллекта.");
            demoFacts.add("ИИ (искусственный интеллект) - это способность машин имитировать человеческий интеллект и выполнять задачи, которые обычно требуют человеческого мышления.");
        }

        if (lowerQuery.contains("машинное обучение")) {
            demoFacts.add("Машинное обучение является подразделом искусственного интеллекта и фокусируется на разработке алгоритмов, которые могут обучаться на данных.");
        }

        if (lowerQuery.contains("глубокое обучение") || lowerQuery.contains("нейронные сети")) {
            demoFacts.add("Глубокое обучение использует нейронные сети с множеством слоев для изучения сложных паттернов в данных.");
        }

        if (demoFacts.isEmpty()) {
            demoFacts.add("Информация по вашему запросу находится в процессе добавления в базу знаний.");
        }

        return demoFacts.subList(0, Math.min(demoFacts.size(), maxResults));
    }

    /**
     * Базовая генерация ответа без использования знаний
     */
    public String generateResponse(String prompt) {
        try {
            // Используем простой вызов к Ollama без RAG
            Iterator<String> responseStream = ollamaClient.generateResponseStream(
                    defaultModel, prompt, false);

            StringBuilder response = new StringBuilder();
            while (responseStream.hasNext()) {
                response.append(responseStream.next());
            }

            return response.toString();
        } catch (Exception e) {
            return "Извините, произошла ошибка при генерации ответа: " + e.getMessage();
        }
    }

    /**
     * Генерация ответа с использованием знаний (RAG)
     */
    public String generateResponseWithKnowledge(String query) {
        try {
            // Поиск релевантных фактов
            List<String> contextFacts = findRelevantFacts(query, maxContextResults);

            // Формирование промпта с контекстом
            StringBuilder context = new StringBuilder();
            if (!contextFacts.isEmpty()) {
                context.append("Контекст:\n");
                for (String fact : contextFacts) {
                    context.append("- ").append(fact).append("\n");
                }
                context.append("\n");
            }

            context.append("Вопрос: ").append(query).append("\nОтвет:");

            // Генерация ответа
            return generateResponse(context.toString());

        } catch (Exception e) {
            return "Извините, произошла ошибка: " + e.getMessage();
        }
    }

    /**
     * Потоковая генерация ответа с использованием знаний
     */
    public Iterator<String> generateResponseStream(String query) {
        try {
            // Поиск релевантных фактов
            List<String> contextFacts = findRelevantFacts(query, maxContextResults);

            // Формирование промпта с контекстом
            StringBuilder context = new StringBuilder();
            if (!contextFacts.isEmpty()) {
                context.append("Используй следующий контекст для ответа:\n");
                for (String fact : contextFacts) {
                    context.append("- ").append(fact).append("\n");
                }
                context.append("\n");
            }

            context.append("Вопрос: ").append(query).append("\nОтвет:");

            // Потоковая генерация ответа
            return ollamaClient.generateResponseStream(defaultModel, context.toString(), true);

        } catch (Exception e) {
            // Возвращаем итератор с сообщением об ошибке
            List<String> errorMessage = List.of("Извините, произошла ошибка: " + e.getMessage());
            return errorMessage.iterator();
        }
    }

    /**
     * Запуск интерактивного чата
     */
    public void startInteractiveChat() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n💬 Interactive Chat Started!");
        System.out.println("Type your questions (or 'quit' to exit):");

        while (true) {
            System.out.print("\nYou: ");
            String question = scanner.nextLine().trim();

            if (question.equalsIgnoreCase("quit") || question.equalsIgnoreCase("exit")) {
                break;
            }

            if (question.isEmpty()) {
                continue;
            }

            try {
                System.out.print("AI: ");
                String response = generateResponseWithKnowledge(question);
                System.out.println(response);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        scanner.close();
    }
}