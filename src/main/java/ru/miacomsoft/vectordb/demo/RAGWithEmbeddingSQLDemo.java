package ru.miacomsoft.vectordb.demo;

import ru.miacomsoft.vectordb.core.*;
import ru.miacomsoft.vectordb.knowledge.KnowledgeConfig;
import ru.miacomsoft.vectordb.knowledge.KnowledgeLoader;
import ru.miacomsoft.vectordb.knowledge.OllamaKnowledgeClient;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Демонстрация RAG с поиском контекста через SQL запросы по Embedding с индексами
 */
public class RAGWithEmbeddingSQLDemo {
    public static void main(String[] args) {
        System.out.println("=== VectorBD RAG with Embedding SQL Demo ===");

        try {
            // Инициализация RAG системы с поддержкой embedding поиска
            RAGWithEmbeddingSystem ragSystem = new RAGWithEmbeddingSystem("./data/rag_embedding_demo");

            // Создание индексов для ускорения поиска
            ragSystem.createEmbeddingIndexes();

            // Загрузка базы знаний
            ragSystem.initializeKnowledgeBase();

            // Демонстрация RAG с embedding поиском
            demonstrateEmbeddingRAG(ragSystem);
            demonstrateIndexPerformance(ragSystem);
            demonstrateHybridSearchRAG(ragSystem);
            demonstrateInteractiveEmbeddingRAG(ragSystem);

            ragSystem.close();

        } catch (Exception e) {
            System.out.println("RAG Embedding Demo error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Демонстрация RAG с поиском по embedding
     */
    private static void demonstrateEmbeddingRAG(RAGWithEmbeddingSystem ragSystem) {
        System.out.println("\n1. RAG with Embedding Search");
        System.out.println("============================");

        try {
            // Сценарий 1: Семантический поиск через embedding
            System.out.println("\n--- Сценарий 1: Семантический поиск по embedding ---");
            String question1 = "Что такое машинное обучение и как оно работает?";
            RAGResult result1 = ragSystem.executeEmbeddingRAGQuery(question1, 4, 0.7);
            printEmbeddingRAGResult(result1, "Embedding Search RAG");

            // Сценарий 2: Поиск похожих концепций
            System.out.println("\n--- Сценарий 2: Поиск похожих концепций ---");
            String question2 = "Нейронные сети и глубокое обучение";
            RAGResult result2 = ragSystem.executeEmbeddingRAGQuery(question2, 3, 0.8);
            printEmbeddingRAGResult(result2, "Similar Concepts Search");

            // Сценарий 3: Технический поиск
            System.out.println("\n--- Сценарий 3: Технический поиск ---");
            String question3 = "Объясни разницу между supervised и unsupervised learning";
            RAGResult result3 = ragSystem.executeEmbeddingRAGQuery(question3, 5, 0.6);
            printEmbeddingRAGResult(result3, "Technical Comparison");

        } catch (Exception e) {
            System.out.println("Embedding RAG error: " + e.getMessage());
        }
    }

    /**
     * Демонстрация производительности с индексами
     */
    private static void demonstrateIndexPerformance(RAGWithEmbeddingSystem ragSystem) {
        System.out.println("\n2. Index Performance Comparison");
        System.out.println("===============================");

        try {
            String testQuestion = "машинное обучение алгоритмы классификация";

            System.out.println("\nТестовый вопрос: " + testQuestion);

            // Тест производительности с индексами
            long startTimeWithIndex = System.currentTimeMillis();
            RAGResult resultWithIndex = ragSystem.executeEmbeddingRAGQuery(testQuestion, 5, 0.7);
            long endTimeWithIndex = System.currentTimeMillis();

            System.out.printf("⏱️  Поиск с индексами: %d мс%n", (endTimeWithIndex - startTimeWithIndex));
            System.out.printf("📊 Найдено документов: %d%n", resultWithIndex.getRetrievedDocuments().size());

            // Тест производительности без индексов (если поддерживается)
            try {
                long startTimeWithoutIndex = System.currentTimeMillis();
                RAGResult resultWithoutIndex = ragSystem.executeRAGQueryWithoutIndex(testQuestion, 5, 0.7);
                long endTimeWithoutIndex = System.currentTimeMillis();

                System.out.printf("⏱️  Поиск без индексов: %d мс%n", (endTimeWithoutIndex - startTimeWithoutIndex));
                System.out.printf("📊 Найдено документов: %d%n", resultWithoutIndex.getRetrievedDocuments().size());

                long speedup = (endTimeWithoutIndex - startTimeWithoutIndex) - (endTimeWithIndex - startTimeWithIndex);
                if (speedup > 0) {
                    System.out.printf("🚀 Ускорение с индексами: %d мс%n", speedup);
                }
            } catch (UnsupportedOperationException e) {
                System.out.println("ℹ️  Сравнение без индексов не поддерживается");
            }

        } catch (Exception e) {
            System.out.println("Performance test error: " + e.getMessage());
        }
    }

    /**
     * Демонстрация гибридного поиска
     */
    private static void demonstrateHybridSearchRAG(RAGWithEmbeddingSystem ragSystem) {
        System.out.println("\n3. Hybrid Search RAG");
        System.out.println("=====================");

        try {
            // Сценарий 1: Гибридный поиск (embedding + keywords)
            System.out.println("\n--- Сценарий 1: Гибридный поиск ---");
            String question1 = "Python библиотеки для анализа данных и машинного обучения";
            RAGResult result1 = ragSystem.executeHybridRAGQuery(question1, 6, 0.7, 0.5);
            printEmbeddingRAGResult(result1, "Hybrid Search");

            // Сценарий 2: Комбинированный поиск с приоритетами
            System.out.println("\n--- Сценарий 2: Weighted Hybrid Search ---");
            String question2 = "Глубокое обучение с использованием TensorFlow и PyTorch";
            RAGResult result2 = ragSystem.executeWeightedHybridRAGQuery(question2, 5, 0.7, 0.8);
            printEmbeddingRAGResult(result2, "Weighted Hybrid Search");

            // Сценарий 3: Мультимодальный поиск
            System.out.println("\n--- Сценарий 3: Multi-stage Search ---");
            String question3 = "Методы оптимизации в машинном обучении: градиентный спуск и Adam";
            RAGResult result3 = ragSystem.executeMultiStageRAGQuery(question3, 4);
            printEmbeddingRAGResult(result3, "Multi-stage Search");

        } catch (Exception e) {
            System.out.println("Hybrid search error: " + e.getMessage());
        }
    }

    /**
     * Интерактивный RAG с embedding поиском
     */
    private static void demonstrateInteractiveEmbeddingRAG(RAGWithEmbeddingSystem ragSystem) {
        System.out.println("\n4. Interactive Embedding RAG");
        System.out.println("=============================");

        Scanner scanner = new Scanner(System.in);
        System.out.println("\n🚀 Запуск интерактивного RAG с embedding поиском...");
        System.out.println("Введите вопросы (или 'quit' для выхода):");

        try {
            while (true) {
                System.out.print("\n🧠 Ваш вопрос: ");
                String userQuestion = scanner.nextLine().trim();

                if (userQuestion.equalsIgnoreCase("quit") || userQuestion.equalsIgnoreCase("exit")) {
                    break;
                }

                if (userQuestion.isEmpty()) {
                    continue;
                }

                // Определение стратегии поиска на основе вопроса
                SearchStrategy strategy = ragSystem.analyzeQuestion(userQuestion);
                System.out.println("🎯 Стратегия поиска: " + strategy.getName());
                System.out.println("⚙️  Параметры: " + strategy.getParameters());

                // Выполнение RAG запроса с выбранной стратегией
                long startTime = System.currentTimeMillis();
                RAGResult ragResult = ragSystem.executeStrategicRAGQuery(userQuestion, strategy);
                long endTime = System.currentTimeMillis();

                // Вывод результатов
                System.out.println("\n📚 Найдено релевантных документов: " + ragResult.getRetrievedDocuments().size());
                System.out.printf("⏱️  Время поиска: %d мс%n", (endTime - startTime));
                System.out.println("🤖 Ответ RAG:");
                System.out.println("--------------");
                System.out.println(ragResult.getGeneratedResponse());

                // Детальная информация о найденных документах
                if (ragSystem.isDebugMode()) {
                    System.out.println("\n🔍 Top embedding matches:");
                    for (int i = 0; i < Math.min(3, ragResult.getRetrievedDocuments().size()); i++) {
                        JSONObject doc = ragResult.getRetrievedDocuments().get(i);
                        double similarity = doc.optDouble("similarity", 0.0);
                        double embeddingScore = doc.optDouble("embedding_score", 0.0);
                        String source = doc.optString("documentId", "unknown");
                        String text = doc.optString("text", "").substring(0, Math.min(80, doc.optString("text", "").length()));

                        System.out.printf("  [%d] similarity: %.3f, embedding: %.3f, source: %s%n",
                                i + 1, similarity, embeddingScore, source);
                        System.out.printf("      %s...%n", text);
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Interactive RAG error: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }

    /**
     * Вывод результата RAG с embedding поиском
     */
    private static void printEmbeddingRAGResult(RAGResult result, String scenario) {
        System.out.println("\n🎯 " + scenario + " Result:");
        System.out.println("=" .repeat(60));

        System.out.println("🤖 Сгенерированный ответ:");
        System.out.println(result.getGeneratedResponse());

        System.out.println("\n📊 Статистика поиска:");
        System.out.printf("- Найдено документов: %d%n", result.getRetrievedDocuments().size());
        System.out.printf("- Длина ответа: %d символов%n", result.getGeneratedResponse().length());

        if (!result.getRetrievedDocuments().isEmpty()) {
            System.out.println("\n🔍 Top embedding matches:");
            for (int i = 0; i < Math.min(3, result.getRetrievedDocuments().size()); i++) {
                JSONObject doc = result.getRetrievedDocuments().get(i);
                double similarity = doc.optDouble("similarity", 0.0);
                double embeddingScore = doc.optDouble("embedding_score", similarity);
                String source = doc.optString("documentId", "unknown");
                String text = doc.optString("text", "");
                String preview = text.length() > 70 ? text.substring(0, 67) + "..." : text;

                System.out.printf("  %d. [%s] embedding: %.3f, similarity: %.3f%n",
                        i + 1, source, embeddingScore, similarity);
                System.out.printf("     %s%n", preview);
            }
        }

        // Информация о стратегии поиска
        if (result.getMetadata().has("search_strategy")) {
            System.out.printf("- Стратегия поиска: %s%n", result.getMetadata().getString("search_strategy"));
        }
        if (result.getMetadata().has("search_time_ms")) {
            System.out.printf("- Время поиска: %d мс%n", result.getMetadata().getLong("search_time_ms"));
        }

        System.out.println("=" .repeat(60));
    }
}

/**
 * RAG система с поддержкой embedding поиска через SQL
 */
class RAGWithEmbeddingSystem {
    private final BinaryVectorDatabase vectorDB;
    private final SQLParser sqlParser;
    private final OllamaKnowledgeClient ollamaClient;
    private final SemanticChunker semanticChunker;
    private final KnowledgeConfig knowledgeConfig;
    private boolean debugMode = true;
    private boolean indexesCreated = false;

    public RAGWithEmbeddingSystem(String databasePath) throws Exception {
        this.knowledgeConfig = new KnowledgeConfig(
                "http://localhost:11434",
                "llama3.2",
                0.7,
                true,
                true
        );

        this.semanticChunker = new SemanticChunker(
                knowledgeConfig.getOllamaUrl(),
                "all-minilm:22m",
                knowledgeConfig.getSimilarityThreshold()
        );

        this.vectorDB = new BinaryVectorDatabase(databasePath, semanticChunker);
        this.sqlParser = new SQLParser(vectorDB);
        this.ollamaClient = new OllamaKnowledgeClient(vectorDB, knowledgeConfig);
    }

    /**
     * Создание индексов для ускорения embedding поиска
     */
    public void createEmbeddingIndexes() throws Exception {
        System.out.println("Creating embedding indexes for faster search...");

        // Создание индексов через SQL команды
        try {
            // Индекс для embedding векторов
            sqlParser.execute("CREATE INDEX idx_embedding_vector ON documents USING vector(vector)");
            System.out.println("✅ Created vector index for embedding search");

            // Индекс для текстового поиска
            sqlParser.execute("CREATE INDEX idx_text_search ON documents(text)");
            System.out.println("✅ Created text index for keyword search");

            // Индекс для метаданных
            sqlParser.execute("CREATE INDEX idx_metadata ON documents(documentId, chunkIndex)");
            System.out.println("✅ Created metadata index");

            indexesCreated = true;

        } catch (Exception e) {
            System.out.println("⚠️  Index creation warning: " + e.getMessage());
            System.out.println("ℹ️  Continuing without indexes...");
        }
    }

    /**
     * Инициализация базы знаний с embedding
     */
    public void initializeKnowledgeBase() throws Exception {
        System.out.println("Initializing RAG knowledge base with embeddings...");

        KnowledgeLoader loader = new KnowledgeLoader(vectorDB, knowledgeConfig);

        // Загрузка знаний с автоматическим созданием embedding
        String[] knowledgeDomains = {
                // Машинное обучение
                """
            Машинное обучение (Machine Learning) - это область искусственного интеллекта, которая позволяет компьютерам обучаться на данных без явного программирования. 
            Основные типы: supervised learning (с учителем), unsupervised learning (без учителя), reinforcement learning (с подкреплением).
            Алгоритмы включают линейную регрессию, decision trees, SVM, neural networks. Применяется в прогнозировании, классификации, кластеризации.
            """,

                // Глубокое обучение
                """
            Глубокое обучение (Deep Learning) использует нейронные сети с множеством слоев. Архитектуры: CNN для изображений, RNN для последовательностей, Transformers для NLP.
            Популярные фреймворки: TensorFlow, PyTorch, Keras. Требует больших данных и вычислительных ресурсов. Применения: computer vision, NLP, speech recognition.
            """,

                // Data Science инструменты
                """
            Python - основной язык для data science. Библиотеки: NumPy для вычислений, Pandas для анализа данных, Scikit-learn для ML, Matplotlib для визуализации.
            Jupyter Notebooks для интерактивного анализа. SQL для работы с базами данных. Cloud platforms: AWS SageMaker, Google AI Platform, Azure ML.
            """,

                // Базы данных и SQL
                """
            SQL (Structured Query Language) используется для управления реляционными базами данных. Основные команды: SELECT, INSERT, UPDATE, DELETE, JOIN.
            NoSQL базы данных: MongoDB (документная), Redis (ключ-значение), Cassandra (колоночная). Векторные базы данных для семантического поиска.
            """,

                // Программирование
                """
            Java - объектно-ориентированный язык со статической типизацией. Используется в enterprise приложениях. JVM обеспечивает кроссплатформенность.
            Python - интерпретируемый язык с динамической типизацией. Популярен в data science, web development (Django, Flask), automation.
            """
        };

        for (int i = 0; i < knowledgeDomains.length; i++) {
            loader.loadText(
                    knowledgeDomains[i],
                    "embedding_domain_" + (i + 1),
                    new Object[]{"embedding_rag", "domain_" + (i + 1)},
                    400,
                    "Embedding Domain " + (i + 1)
            );
        }

        System.out.println("✅ RAG knowledge base initialized with " + knowledgeDomains.length + " domains and embeddings");
    }

    /**
     * Выполнение RAG запроса с поиском по embedding
     */
    public RAGResult executeEmbeddingRAGQuery(String question, int topK, double similarityThreshold) throws Exception {
        long searchStartTime = System.currentTimeMillis();

        // Шаг 1: Получение embedding для вопроса
        float[] questionEmbedding = semanticChunker.getEmbedding(question);

        // Шаг 2: Поиск похожих документов по embedding через SQL
        List<JSONObject> retrievedDocs = findSimilarByEmbedding(questionEmbedding, topK, similarityThreshold);

        long searchEndTime = System.currentTimeMillis();

        // Шаг 3: Подготовка контекста и генерация ответа
        String context = buildEnhancedContext(retrievedDocs, question);
        String response = generateContextualResponse(question, context);

        // Создание результата с метаданными
        RAGResult result = new RAGResult(question, response, retrievedDocs, context);
        result.getMetadata().put("search_strategy", "embedding_similarity");
        result.getMetadata().put("search_time_ms", searchEndTime - searchStartTime);
        result.getMetadata().put("indexes_used", indexesCreated);

        return result;
    }

    /**
     * Гибридный RAG запрос (embedding + keywords)
     */
    public RAGResult executeHybridRAGQuery(String question, int topK, double embeddingThreshold, double keywordThreshold) throws Exception {
        long searchStartTime = System.currentTimeMillis();

        // Embedding поиск
        float[] questionEmbedding = semanticChunker.getEmbedding(question);
        List<JSONObject> embeddingResults = findSimilarByEmbedding(questionEmbedding, topK, embeddingThreshold);

        // Keyword поиск
        List<JSONObject> keywordResults = findSimilarByKeywords(question, topK, keywordThreshold);

        // Объединение и ранжирование результатов
        List<JSONObject> hybridResults = mergeAndRankResults(embeddingResults, keywordResults, topK);

        long searchEndTime = System.currentTimeMillis();

        String context = buildEnhancedContext(hybridResults, question);
        String response = generateContextualResponse(question, context);

        RAGResult result = new RAGResult(question, response, hybridResults, context);
        result.getMetadata().put("search_strategy", "hybrid_embedding_keywords");
        result.getMetadata().put("search_time_ms", searchEndTime - searchStartTime);
        result.getMetadata().put("embedding_results", embeddingResults.size());
        result.getMetadata().put("keyword_results", keywordResults.size());

        return result;
    }

    /**
     * Weighted hybrid search с приоритетами
     */
    public RAGResult executeWeightedHybridRAGQuery(String question, int topK, double embeddingWeight, double keywordWeight) throws Exception {
        // Реализация weighted hybrid search
        float[] questionEmbedding = semanticChunker.getEmbedding(question);

        List<JSONObject> embeddingResults = findSimilarByEmbedding(questionEmbedding, topK * 2, 0.5);
        List<JSONObject> keywordResults = findSimilarByKeywords(question, topK * 2, 0.5);

        // Взвешенное объединение результатов
        List<JSONObject> weightedResults = mergeWithWeights(embeddingResults, keywordResults, embeddingWeight, keywordWeight, topK);

        String context = buildEnhancedContext(weightedResults, question);
        String response = generateContextualResponse(question, context);

        RAGResult result = new RAGResult(question, response, weightedResults, context);
        result.getMetadata().put("search_strategy", "weighted_hybrid");
        result.getMetadata().put("embedding_weight", embeddingWeight);
        result.getMetadata().put("keyword_weight", keywordWeight);

        return result;
    }

    /**
     * Многоэтапный поиск
     */
    public RAGResult executeMultiStageRAGQuery(String question, int topK) throws Exception {
        // Этап 1: Broad embedding search
        float[] questionEmbedding = semanticChunker.getEmbedding(question);
        List<JSONObject> broadResults = findSimilarByEmbedding(questionEmbedding, topK * 3, 0.3);

        // Этап 2: Keyword refinement
        List<JSONObject> refinedResults = refineWithKeywords(broadResults, question, 0.6);

        // Этап 3: Final ranking
        List<JSONObject> finalResults = finalRanking(refinedResults, questionEmbedding, topK);

        String context = buildEnhancedContext(finalResults, question);
        String response = generateContextualResponse(question, context);

        RAGResult result = new RAGResult(question, response, finalResults, context);
        result.getMetadata().put("search_strategy", "multi_stage");
        result.getMetadata().put("stages", 3);

        return result;
    }

    /**
     * Стратегический RAG запрос
     */
    public RAGResult executeStrategicRAGQuery(String question, SearchStrategy strategy) throws Exception {
        switch (strategy.getType()) {
            case "embedding":
                return executeEmbeddingRAGQuery(question, strategy.getTopK(), strategy.getSimilarityThreshold());
            case "hybrid":
                return executeHybridRAGQuery(question, strategy.getTopK(),
                        strategy.getEmbeddingThreshold(), strategy.getKeywordThreshold());
            case "weighted_hybrid":
                return executeWeightedHybridRAGQuery(question, strategy.getTopK(),
                        strategy.getEmbeddingWeight(), strategy.getKeywordWeight());
            case "multi_stage":
                return executeMultiStageRAGQuery(question, strategy.getTopK());
            default:
                return executeEmbeddingRAGQuery(question, strategy.getTopK(), strategy.getSimilarityThreshold());
        }
    }

    /**
     * Поиск похожих документов по embedding
     */
    private List<JSONObject> findSimilarByEmbedding(float[] queryEmbedding, int topK, double similarityThreshold) throws Exception {
        // Используем семантический поиск через SQLParser, который internally использует векторный индекс
        List<JSONObject> results = sqlParser.semanticSearch(Arrays.toString(queryEmbedding), topK);

        // Фильтрация по порогу схожести
        List<JSONObject> filteredResults = new java.util.ArrayList<>();
        for (JSONObject doc : results) {
            double similarity = doc.optDouble("similarity", 0.0);
            if (similarity >= similarityThreshold) {
                doc.put("embedding_score", similarity);
                doc.put("search_type", "embedding");
                filteredResults.add(doc);
            }
        }

        return filteredResults;
    }

    /**
     * Поиск по ключевым словам
     */
    private List<JSONObject> findSimilarByKeywords(String question, int topK, double similarityThreshold) throws Exception {
        // Извлечение ключевых слов
        String[] keywords = extractKeywords(question);

        List<JSONObject> allResults = new java.util.ArrayList<>();

        for (String keyword : keywords) {
            if (keyword.length() > 2) { // Игнорируем слишком короткие слова
                List<JSONObject> keywordResults = sqlParser.execute(
                        String.format("SELECT * FROM embedding_rag WHERE text LIKE '%%%s%%' LIMIT %d", keyword, topK)
                );

                for (JSONObject doc : keywordResults) {
                    doc.put("search_type", "keyword");
                    doc.put("keyword_score", 1.0);
                    allResults.add(doc);
                }
            }
        }

        // Удаление дубликатов и ранжирование
        return removeDuplicatesAndRank(allResults, topK);
    }

    /**
     * Анализ вопроса для определения стратегии поиска
     */
    public SearchStrategy analyzeQuestion(String question) {
        SearchStrategy strategy = new SearchStrategy();

        // Эвристики для определения оптимальной стратегии
        if (question.length() < 20) {
            // Короткие вопросы - embedding search
            strategy.setType("embedding");
            strategy.setTopK(3);
            strategy.setSimilarityThreshold(0.8);
        } else if (containsTechnicalTerms(question)) {
            // Технические вопросы - hybrid search
            strategy.setType("hybrid");
            strategy.setTopK(5);
            strategy.setEmbeddingThreshold(0.7);
            strategy.setKeywordThreshold(0.6);
        } else if (isComparativeQuestion(question)) {
            // Сравнительные вопросы - weighted hybrid
            strategy.setType("weighted_hybrid");
            strategy.setTopK(6);
            strategy.setEmbeddingWeight(0.7);
            strategy.setKeywordWeight(0.3);
        } else if (isComplexQuestion(question)) {
            // Сложные вопросы - multi-stage
            strategy.setType("multi_stage");
            strategy.setTopK(4);
        } else {
            // По умолчанию - embedding search
            strategy.setType("embedding");
            strategy.setTopK(4);
            strategy.setSimilarityThreshold(0.7);
        }

        return strategy;
    }

    /**
     * Вспомогательные методы
     */
    private String[] extractKeywords(String text) {
        return text.toLowerCase()
                .replaceAll("[^а-яa-z0-9\\s]", "")
                .split("\\s+");
    }

    private List<JSONObject> mergeAndRankResults(List<JSONObject> embeddingResults, List<JSONObject> keywordResults, int topK) {
        // Объединение результатов с приоритетом embedding результатов
        List<JSONObject> allResults = new java.util.ArrayList<>();
        allResults.addAll(embeddingResults);
        allResults.addAll(keywordResults);

        return removeDuplicatesAndRank(allResults, topK);
    }

    private List<JSONObject> mergeWithWeights(List<JSONObject> embeddingResults, List<JSONObject> keywordResults,
                                              double embeddingWeight, double keywordWeight, int topK) {
        // Взвешенное объединение результатов
        List<JSONObject> allResults = new java.util.ArrayList<>();

        for (JSONObject doc : embeddingResults) {
            double currentScore = doc.optDouble("embedding_score", 0.0);
            doc.put("final_score", currentScore * embeddingWeight);
            allResults.add(doc);
        }

        for (JSONObject doc : keywordResults) {
            double currentScore = doc.optDouble("keyword_score", 0.0);
            double finalScore = doc.optDouble("final_score", 0.0) + (currentScore * keywordWeight);
            doc.put("final_score", finalScore);
            allResults.add(doc);
        }

        // Сортировка по final_score
        allResults.sort((a, b) -> Double.compare(
                b.optDouble("final_score", 0.0),
                a.optDouble("final_score", 0.0)
        ));

        return allResults.stream().limit(topK).collect(java.util.stream.Collectors.toList());
    }

    private List<JSONObject> removeDuplicatesAndRank(List<JSONObject> documents, int topK) {
        List<JSONObject> uniqueDocs = new java.util.ArrayList<>();
        java.util.Set<String> seenTexts = new java.util.HashSet<>();

        for (JSONObject doc : documents) {
            String text = doc.optString("text", "");
            if (!seenTexts.contains(text) && !text.trim().isEmpty()) {
                seenTexts.add(text);
                uniqueDocs.add(doc);
            }
        }

        // Сортировка по similarity/score
        uniqueDocs.sort((a, b) -> Double.compare(
                b.optDouble("similarity", b.optDouble("embedding_score", b.optDouble("keyword_score", 0.0))),
                a.optDouble("similarity", a.optDouble("embedding_score", a.optDouble("keyword_score", 0.0)))
        ));

        return uniqueDocs.stream().limit(topK).collect(java.util.stream.Collectors.toList());
    }

    private List<JSONObject> refineWithKeywords(List<JSONObject> documents, String question, double threshold) {
        String[] keywords = extractKeywords(question);
        List<JSONObject> refined = new java.util.ArrayList<>();

        for (JSONObject doc : documents) {
            String text = doc.optString("text", "").toLowerCase();
            int keywordMatches = 0;

            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    keywordMatches++;
                }
            }

            double keywordScore = (double) keywordMatches / keywords.length;
            if (keywordScore >= threshold) {
                doc.put("keyword_refinement_score", keywordScore);
                refined.add(doc);
            }
        }

        return refined;
    }

    private List<JSONObject> finalRanking(List<JSONObject> documents, float[] queryEmbedding, int topK) {
        // Финальное ранжирование на основе комбинированных метрик
        for (JSONObject doc : documents) {
            double embeddingScore = doc.optDouble("embedding_score", 0.0);
            double keywordScore = doc.optDouble("keyword_refinement_score", 0.0);
            double finalScore = (embeddingScore * 0.7) + (keywordScore * 0.3);
            doc.put("final_ranking_score", finalScore);
        }

        documents.sort((a, b) -> Double.compare(
                b.optDouble("final_ranking_score", 0.0),
                a.optDouble("final_ranking_score", 0.0)
        ));

        return documents.stream().limit(topK).collect(java.util.stream.Collectors.toList());
    }

    private String buildEnhancedContext(List<JSONObject> documents, String question) {
        if (documents.isEmpty()) {
            return "Релевантная информация не найдена в базе знаний.";
        }

        StringBuilder context = new StringBuilder();
        context.append("Контекстная информация для ответа на вопрос: '").append(question).append("'\n\n");

        for (int i = 0; i < documents.size(); i++) {
            JSONObject doc = documents.get(i);
            String text = doc.optString("text", "");
            double similarity = doc.optDouble("similarity", doc.optDouble("embedding_score", 0.0));
            String source = doc.optString("documentId", "unknown");
            String searchType = doc.optString("search_type", "unknown");

            context.append("=== Источник ").append(i + 1).append(" ===\n");
            context.append("Тип поиска: ").append(searchType).append(" | ");
            context.append("Релевантность: ").append(String.format("%.3f", similarity)).append(" | ");
            context.append("Источник: ").append(source).append("\n");
            context.append(text).append("\n\n");
        }

        return context.toString();
    }

    private String generateContextualResponse(String question, String context) {
        String prompt = String.format("""
            Используй предоставленный контекст для точного ответа на вопрос. 
            Если информации в контексте недостаточно, используй свои знания, но укажи это явно.
            
            Контекст:
            %s
            
            Вопрос: %s
            
            Ответ (будь точным, информативным и используй информацию из контекста):
            """, context, question);

        return ollamaClient.generateResponse(prompt);
    }

    private boolean containsTechnicalTerms(String question) {
        String[] technicalTerms = {"алгоритм", "модель", "нейрон", "сеть", "обучение", "данные", "анализ"};
        String lowerQuestion = question.toLowerCase();
        for (String term : technicalTerms) {
            if (lowerQuestion.contains(term)) return true;
        }
        return false;
    }

    private boolean isComparativeQuestion(String question) {
        return question.toLowerCase().contains("сравни") ||
                question.toLowerCase().contains("разница") ||
                question.toLowerCase().contains("отличие");
    }

    private boolean isComplexQuestion(String question) {
        return question.length() > 50 || question.split(" ").length > 10;
    }

    // Методы для тестирования производительности
    public RAGResult executeRAGQueryWithoutIndex(String question, int topK, double similarityThreshold) throws Exception {
        // Этот метод может быть реализован для сравнения производительности
        // В реальной системе может потребоваться временное отключение индексов
        throw new UnsupportedOperationException("Index-less search not implemented in this demo");
    }

    public boolean isDebugMode() { return debugMode; }
    public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }
    public void close() { vectorDB.close(); }
}

/**
 * Класс стратегии поиска
 */
class SearchStrategy {
    private String type = "embedding";
    private int topK = 3;
    private double similarityThreshold = 0.7;
    private double embeddingThreshold = 0.7;
    private double keywordThreshold = 0.5;
    private double embeddingWeight = 0.7;
    private double keywordWeight = 0.3;

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
    public double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
    public double getEmbeddingThreshold() { return embeddingThreshold; }
    public void setEmbeddingThreshold(double embeddingThreshold) { this.embeddingThreshold = embeddingThreshold; }
    public double getKeywordThreshold() { return keywordThreshold; }
    public void setKeywordThreshold(double keywordThreshold) { this.keywordThreshold = keywordThreshold; }
    public double getEmbeddingWeight() { return embeddingWeight; }
    public void setEmbeddingWeight(double embeddingWeight) { this.embeddingWeight = embeddingWeight; }
    public double getKeywordWeight() { return keywordWeight; }
    public void setKeywordWeight(double keywordWeight) { this.keywordWeight = keywordWeight; }

    public String getName() {
        switch (type) {
            case "embedding": return "Embedding Search";
            case "hybrid": return "Hybrid Search";
            case "weighted_hybrid": return "Weighted Hybrid Search";
            case "multi_stage": return "Multi-stage Search";
            default: return "Unknown Strategy";
        }
    }

    public String getParameters() {
        switch (type) {
            case "embedding":
                return String.format("topK=%d, threshold=%.2f", topK, similarityThreshold);
            case "hybrid":
                return String.format("topK=%d, embedding=%.2f, keyword=%.2f", topK, embeddingThreshold, keywordThreshold);
            case "weighted_hybrid":
                return String.format("topK=%d, embedding_weight=%.2f, keyword_weight=%.2f", topK, embeddingWeight, keywordWeight);
            case "multi_stage":
                return String.format("topK=%d, 3 stages", topK);
            default:
                return "No parameters";
        }
    }
}

/**
 * Класс результата RAG
 */
class RAGResult {
    private final String question;
    private final String generatedResponse;
    private final List<JSONObject> retrievedDocuments;
    private final String context;
    private final JSONObject metadata;

    public RAGResult(String question, String generatedResponse, List<JSONObject> retrievedDocuments, String context) {
        this.question = question;
        this.generatedResponse = generatedResponse;
        this.retrievedDocuments = retrievedDocuments;
        this.context = context;
        this.metadata = new JSONObject();
    }

    // Getters
    public String getQuestion() { return question; }
    public String getGeneratedResponse() { return generatedResponse; }
    public List<JSONObject> getRetrievedDocuments() { return retrievedDocuments; }
    public String getContext() { return context; }
    public JSONObject getMetadata() { return metadata; }
}