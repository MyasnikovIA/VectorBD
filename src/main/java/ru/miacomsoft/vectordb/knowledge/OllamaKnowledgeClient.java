package ru.miacomsoft.vectordb.knowledge;

import ru.miacomsoft.vectordb.core.BinaryVectorDatabase;
import ru.miacomsoft.vectordb.core.VectorSearchResult;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Клиент для работы с знаниями и Ollama для бинарной векторной базы данных
 */
public class OllamaKnowledgeClient {
    private final BinaryVectorDatabase database;
    private final OllamaStreamClient ollamaClient;
    private final KnowledgeConfig knowledgeConfig;
    private String defaultModel = "deepseek-v3.1:671b-cloud";
    private double similarityThreshold = 0.7;
    private int maxContextResults = 3;

    public OllamaKnowledgeClient(BinaryVectorDatabase database, KnowledgeConfig knowledgeConfig) {
        this.database = database;
        this.knowledgeConfig = knowledgeConfig;
        this.ollamaClient = new OllamaStreamClient(knowledgeConfig.getOllamaUrl());

        configureFromConfig();
    }

    public OllamaKnowledgeClient(BinaryVectorDatabase database, KnowledgeConfig knowledgeConfig, String ollamaUrl) {
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

            System.out.println("Binary OllamaKnowledgeClient configured:");
            System.out.println("  - Model: " + defaultModel);
            System.out.println("  - Similarity Threshold: " + similarityThreshold);
            System.out.println("  - Ollama URL: " + knowledgeConfig.getOllamaUrl());
            System.out.println("  - Database type: Binary");
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
     * Поиск релевантных фактов в бинарной базе знаний
     */
    public List<String> findRelevantFacts(String query, int maxResults) {
        List<String> relevantFacts = new ArrayList<>();
        try {
            // Поиск в бинарной векторной базе данных
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
            System.err.println("Error finding relevant facts in binary database: " + e.getMessage());
            relevantFacts = getDemoFacts(query, maxResults);
        }

        return relevantFacts;
    }

    /**
     * Демо-факты для случаев, когда бинарная база данных недоступна
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

        if (lowerQuery.contains("бинарная") || lowerQuery.contains("база данных")) {
            demoFacts.add("Бинарная векторная база данных использует сериализацию для эффективного хранения и поиска векторных данных.");
            demoFacts.add("Бинарное хранение данных обеспечивает лучшую производительность и компактность по сравнению с текстовыми форматами.");
        }

        if (demoFacts.isEmpty()) {
            demoFacts.add("Информация по вашему запросу находится в процессе добавления в бинарную базу знаний.");
            demoFacts.add("Бинарная база данных обеспечивает быстрый семантический поиск по векторным представлениям текста.");
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
     * Генерация ответа с использованием знаний из бинарной БД (RAG)
     */
    public String generateResponseWithKnowledge(String query) {
        try {
            // Поиск релевантных фактов в бинарной БД
            List<String> contextFacts = findRelevantFacts(query, maxContextResults);

            // Формирование промпта с контекстом из бинарной БД
            StringBuilder context = new StringBuilder();
            if (!contextFacts.isEmpty()) {
                context.append("Контекст из бинарной базы знаний:\n");
                for (int i = 0; i < contextFacts.size(); i++) {
                    context.append(i + 1).append(". ").append(contextFacts.get(i)).append("\n");
                }
                context.append("\n");
            }

            context.append("Вопрос: ").append(query).append("\n\n");
            context.append("Ответь на вопрос, используя предоставленный контекст. Если в контексте нет нужной информации, используй свои знания.\n");
            context.append("Ответ:");

            // Генерация ответа
            return generateResponse(context.toString());

        } catch (Exception e) {
            return "Извините, произошла ошибка при работе с бинарной базой знаний: " + e.getMessage();
        }
    }

    /**
     * Потоковая генерация ответа с использованием знаний из бинарной БД
     */
    public Iterator<String> generateResponseStream(String query) {
        try {
            // Поиск релевантных фактов в бинарной БД
            List<String> contextFacts = findRelevantFacts(query, maxContextResults);

            // Формирование промпта с контекстом из бинарной БД
            StringBuilder context = new StringBuilder();
            if (!contextFacts.isEmpty()) {
                context.append("Используй следующий контекст из бинарной базы знаний для ответа:\n");
                for (int i = 0; i < contextFacts.size(); i++) {
                    context.append(i + 1).append(". ").append(contextFacts.get(i)).append("\n");
                }
                context.append("\n");
            }

            context.append("Вопрос: ").append(query).append("\n\n");
            context.append("Ответь на вопрос, используя предоставленный контекст. Будь точным и информативным.\n");
            context.append("Ответ:");

            // Потоковая генерация ответа
            return ollamaClient.generateResponseStream(defaultModel, context.toString(), true);

        } catch (Exception e) {
            // Возвращаем итератор с сообщением об ошибке
            List<String> errorMessage = List.of("Извините, произошла ошибка при работе с бинарной базой знаний: " + e.getMessage());
            return errorMessage.iterator();
        }
    }

    /**
     * Расширенная генерация ответа с детальной статистикой из бинарной БД
     */
    public Map<String, Object> generateResponseWithStats(String query) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Поиск релевантных фактов в бинарной БД
            List<VectorSearchResult> searchResults = database.similaritySearch(query, maxContextResults);
            List<String> contextFacts = new ArrayList<>();
            List<Double> similarities = new ArrayList<>();

            for (VectorSearchResult resultItem : searchResults) {
                if (resultItem.getSimilarity() >= similarityThreshold) {
                    contextFacts.add(resultItem.getVectorData().getText());
                    similarities.add(resultItem.getSimilarity());
                }
            }

            // Формирование промпта с контекстом
            StringBuilder context = new StringBuilder();
            if (!contextFacts.isEmpty()) {
                context.append("Контекст из бинарной базы знаний:\n");
                for (int i = 0; i < contextFacts.size(); i++) {
                    context.append(i + 1).append(". ").append(contextFacts.get(i));
                    context.append(" [схожесть: ").append(String.format("%.3f", similarities.get(i))).append("]\n");
                }
                context.append("\n");
            }

            context.append("Вопрос: ").append(query).append("\nОтвет:");

            // Генерация ответа
            String response = generateResponse(context.toString());

            // Формирование результата со статистикой
            result.put("response", response);
            result.put("contextFacts", contextFacts);
            result.put("similarities", similarities);
            result.put("totalFacts", contextFacts.size());
            result.put("databaseType", "binary");
            result.put("vectorCount", database.getVectorCount());

        } catch (Exception e) {
            result.put("error", "Ошибка при генерации ответа: " + e.getMessage());
            result.put("response", generateResponse(query)); // Fallback to simple response
        }

        return result;
    }

    /**
     * Поиск похожих документов в бинарной БД с детальной информацией
     */
    public List<Map<String, Object>> findSimilarDocuments(String query, int maxResults) {
        List<Map<String, Object>> results = new ArrayList<>();

        try {
            List<VectorSearchResult> searchResults = database.similaritySearch(query, maxResults);

            for (VectorSearchResult result : searchResults) {
                Map<String, Object> docInfo = new HashMap<>();
                docInfo.put("text", result.getVectorData().getText());
                docInfo.put("similarity", result.getSimilarity());
                docInfo.put("documentId", result.getVectorData().getDocumentId());
                docInfo.put("chunkIndex", result.getVectorData().getChunkIndex());
                docInfo.put("nodePath", result.getVectorData().getNodePath());
                docInfo.put("databaseType", "binary");

                results.add(docInfo);
            }
        } catch (Exception e) {
            System.err.println("Error searching similar documents in binary database: " + e.getMessage());
        }

        return results;
    }

    /**
     * Получить статистику бинарной базы знаний
     */
    public Map<String, Object> getDatabaseStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            stats.put("totalVectors", database.getVectorCount());
            stats.put("databaseType", "binary");
            stats.put("similarityThreshold", similarityThreshold);
            stats.put("maxContextResults", maxContextResults);
            stats.put("defaultModel", defaultModel);
            stats.put("ollamaUrl", ollamaClient.getOllamaUrl());

            // Информация о размере базы (примерная)
            int estimatedSize = database.getVectorCount() * 1024; // Примерная оценка
            stats.put("estimatedSizeKB", estimatedSize);

        } catch (Exception e) {
            stats.put("error", "Error getting database stats: " + e.getMessage());
        }

        return stats;
    }

    /**
     * Запуск интерактивного чата с бинарной базой знаний
     */
    public void startInteractiveChat() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n💬 Binary Interactive Chat Started!");
        System.out.println("Database type: Binary serialization");
        System.out.println("Total vectors: " + database.getVectorCount());
        System.out.println("Model: " + defaultModel);
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

                // Используем потоковую генерацию для лучшего UX
                Iterator<String> responseStream = generateResponseStream(question);
                while (responseStream.hasNext()) {
                    String token = responseStream.next();
                    if (token != null && !token.trim().isEmpty()) {
                        System.out.print(token);
                    }
                }
                System.out.println(); // Новая строка после ответа

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        scanner.close();
        System.out.println("\n🔚 Binary chat session ended.");
    }

    /**
     * Тестирование подключения к бинарной БД и Ollama
     */
    public Map<String, Boolean> testConnections() {
        Map<String, Boolean> results = new HashMap<>();

        // Тест подключения к бинарной БД
        try {
            int vectorCount = database.getVectorCount();
            results.put("binary_database", true);
            results.put("database_vectors", vectorCount > 0);
        } catch (Exception e) {
            results.put("binary_database", false);
            results.put("database_vectors", false);
        }

        // Тест подключения к Ollama
        try {
            boolean ollamaAvailable = ollamaClient.isServerAvailable();
            results.put("ollama_server", ollamaAvailable);

            if (ollamaAvailable) {
                List<String> models = ollamaClient.getAvailableModels();
                results.put("ollama_models", !models.isEmpty());
                results.put("default_model_available", models.contains(defaultModel));
            } else {
                results.put("ollama_models", false);
                results.put("default_model_available", false);
            }
        } catch (Exception e) {
            results.put("ollama_server", false);
            results.put("ollama_models", false);
            results.put("default_model_available", false);
        }

        return results;
    }

    /**
     * Показать результаты тестирования подключений
     */
    public void printConnectionTest() {
        System.out.println("=== Binary Database Connection Test ===");
        Map<String, Boolean> testResults = testConnections();

        for (Map.Entry<String, Boolean> entry : testResults.entrySet()) {
            String status = entry.getValue() ? "✅" : "❌";
            System.out.println(status + " " + entry.getKey() + ": " + entry.getValue());
        }

        System.out.println("=== Binary Database Stats ===");
        Map<String, Object> stats = getDatabaseStats();
        for (Map.Entry<String, Object> entry : stats.entrySet()) {
            System.out.println("📊 " + entry.getKey() + ": " + entry.getValue());
        }
    }
    /**
     * Генерация ответа с использованием предоставленного контекста
     * @param queryWithContext запрос с уже подготовленным контекстом
     * @return сгенерированный ответ
     */
    public String generateResponseWithContext(String queryWithContext) {
        try {
            // Формируем промпт для модели с явным указанием использовать контекст
            String prompt = String.format("""
            Ты - AI ассистент. Используй предоставленный контекст для точного ответа на вопрос.
            
            %s
            
            Инструкции:
            - Ответь максимально точно на основе предоставленного контекста
            - Если в контексте нет нужной информации, явно укажи это
            - Будь конкретен и информативен
            - Сохраняй профессиональный тон
            - Если информация в контексте противоречива, укажи на это
            
            Ответ:
            """, queryWithContext);

            // Генерация ответа через Ollama
            Iterator<String> responseStream = ollamaClient.generateResponseStream(
                    defaultModel, prompt, false);

            StringBuilder response = new StringBuilder();
            while (responseStream.hasNext()) {
                String token = responseStream.next();
                if (token != null && !token.trim().isEmpty()) {
                    response.append(token);
                }
            }

            return response.toString();

        } catch (Exception e) {
            System.err.println("Error generating response with context: " + e.getMessage());
            return "Извините, произошла ошибка при генерации ответа с использованием контекста: " + e.getMessage();
        }
    }

    /**
     * Генерация ответа с контекстом и потоковой передачей
     * @param queryWithContext запрос с контекстом
     * @return итератор для потокового чтения ответа
     */
    public Iterator<String> generateResponseWithContextStream(String queryWithContext) {
        try {
            String prompt = String.format("""
            Ты - AI ассистент. Используй предоставленный контекст для точного ответа.
            Будь точным и информативным, основывай ответ на контексте.
            
            %s
            
            Ответ:
            """, queryWithContext);

            return ollamaClient.generateResponseStream(defaultModel, prompt, true);

        } catch (Exception e) {
            System.err.println("Error generating stream response with context: " + e.getMessage());
            List<String> errorMessage = List.of("Извините, произошла ошибка: " + e.getMessage());
            return errorMessage.iterator();
        }
    }

    /**
     * Генерация ответа с автоматическим поиском контекста из базы знаний
     * @param query пользовательский запрос
     * @param context дополнительный контекст (может быть null)
     * @return сгенерированный ответ
     */
    public String generateResponseWithContext(String query, String context) {
        try {
            StringBuilder fullContext = new StringBuilder();

            // Добавляем контекст из базы знаний, если запрос не пустой
            if (query != null && !query.trim().isEmpty()) {
                List<String> relevantFacts = findRelevantFacts(query, maxContextResults);
                if (!relevantFacts.isEmpty()) {
                    fullContext.append("Релевантная информация из базы знаний:\n");
                    for (int i = 0; i < relevantFacts.size(); i++) {
                        fullContext.append(i + 1).append(". ").append(relevantFacts.get(i)).append("\n");
                    }
                    fullContext.append("\n");
                }
            }

            // Добавляем предоставленный контекст
            if (context != null && !context.trim().isEmpty()) {
                fullContext.append("Дополнительный контекст:\n").append(context).append("\n\n");
            }

            // Формируем финальный промпт
            String enhancedQuery;
            if (fullContext.length() > 0) {
                enhancedQuery = fullContext.toString() + "Вопрос: " + query;
            } else {
                enhancedQuery = "Вопрос: " + query;
            }

            return generateResponseWithContext(enhancedQuery);

        } catch (Exception e) {
            System.err.println("Error in generateResponseWithContext: " + e.getMessage());
            return generateResponse(query); // Fallback to simple response
        }
    }

    /**
     * Расширенная версия с детальной статистикой
     */
    public Map<String, Object> generateResponseWithContextDetailed(String query, String context) {
        Map<String, Object> result = new HashMap<>();

        try {
            long startTime = System.currentTimeMillis();

            // Поиск релевантных фактов
            List<VectorSearchResult> searchResults = database.similaritySearch(query, maxContextResults);
            List<String> contextFacts = new ArrayList<>();
            List<Double> similarities = new ArrayList<>();

            for (VectorSearchResult resultItem : searchResults) {
                if (resultItem.getSimilarity() >= similarityThreshold) {
                    contextFacts.add(resultItem.getVectorData().getText());
                    similarities.add(resultItem.getSimilarity());
                }
            }

            // Формирование полного контекста
            StringBuilder fullContext = new StringBuilder();

            if (!contextFacts.isEmpty()) {
                fullContext.append("Контекст из бинарной базы знаний:\n");
                for (int i = 0; i < contextFacts.size(); i++) {
                    fullContext.append("[Документ ").append(i + 1)
                            .append(", схожесть: ").append(String.format("%.3f", similarities.get(i)))
                            .append("]\n")
                            .append(contextFacts.get(i)).append("\n\n");
                }
            }

            if (context != null && !context.trim().isEmpty()) {
                fullContext.append("Дополнительный контекст:\n").append(context).append("\n\n");
            }

            // Генерация ответа
            String enhancedQuery = fullContext.toString() + "Вопрос: " + query;
            String response = generateResponseWithContext(enhancedQuery);

            long endTime = System.currentTimeMillis();

            // Формирование результата
            result.put("response", response);
            result.put("contextFacts", contextFacts);
            result.put("similarities", similarities);
            result.put("providedContext", context);
            result.put("processingTimeMs", endTime - startTime);
            result.put("databaseResultsUsed", contextFacts.size());
            result.put("databaseType", "binary");

        } catch (Exception e) {
            result.put("error", "Ошибка при генерации ответа: " + e.getMessage());
            result.put("response", generateResponse(query)); // Fallback
        }

        return result;
    }

}