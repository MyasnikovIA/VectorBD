package ru.miacomsoft.vectordb.demo;

import ru.miacomsoft.vectordb.core.*;
import ru.miacomsoft.vectordb.knowledge.KnowledgeConfig;
import ru.miacomsoft.vectordb.knowledge.KnowledgeLoader;
import ru.miacomsoft.vectordb.knowledge.OllamaKnowledgeClient;
import org.json.JSONObject;

import java.util.List;
import java.util.Scanner;

/**
 * Демонстрация RAG (Retrieval-Augmented Generation) с использованием SQL запросов в VectorBD
 * Этот демо-пример показывает полный цикл RAG:
 *
 * Retrieval - поиск релевантных документов через SQL запросы
 *
 * Augmentation - подготовка контекста из найденных документов
 *
 * Generation - генерация ответа с использованием контекста
 *
 * Особенности реализации:
 *
 * Гибридный поиск - комбинация семантического и keyword поиска
 *
 * Автоматическая настройка параметров - на основе типа вопроса
 *
 * Оценка качества - метрики для анализа работы RAG системы
 *
 * Интерактивный режим - чат с автоматической подготовкой контекста
 *
 * Различные стратегии - для разных типов вопросов
 *
 * Для запуска убедитесь, что Ollama сервер доступен и база знаний инициализирована.
 *
 */
public class RAGSQLDemo {
    public static void main(String[] args) {
        System.out.println("=== VectorBD RAG with SQL Demo ===");

        try {
            // Инициализация компонентов RAG системы
            RAGSystem ragSystem = new RAGSystem("./data/rag_demo");

            // Загрузка или создание базы знаний
            ragSystem.initializeKnowledgeBase();

            // Демонстрация различных сценариев RAG
            demonstrateBasicRAG(ragSystem);
            demonstrateAdvancedRAG(ragSystem);
            demonstrateInteractiveRAG(ragSystem);
            demonstrateRAGEvaluation(ragSystem);

            ragSystem.close();

        } catch (Exception e) {
            System.out.println("RAG Demo error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Демонстрация базового RAG сценария
     */
    private static void demonstrateBasicRAG(RAGSystem ragSystem) {
        System.out.println("\n1. Basic RAG Scenarios");
        System.out.println("======================");

        try {
            // Сценарий 1: Простой вопрос-ответ
            System.out.println("\n--- Сценарий 1: Простой QA RAG ---");
            String question1 = "Что такое машинное обучение и какие основные типы существуют?";
            RAGResultV1 result1 = ragSystem.executeRAGQuery(question1, 4, 0.7);
            printRAGResult(result1, "Базовый QA");

            // Сценарий 2: Сравнительный анализ
            System.out.println("\n--- Сценарий 2: Сравнительный RAG ---");
            String question2 = "Сравни supervised и unsupervised learning";
            RAGResultV1 result2 = ragSystem.executeRAGQuery(question2, 5, 0.6);
            printRAGResult(result2, "Сравнительный анализ");

            // Сценарий 3: Техническое объяснение
            System.out.println("\n--- Сценарий 3: Техническое объяснение ---");
            String question3 = "Объясни как работают нейронные сети";
            RAGResultV1 result3 = ragSystem.executeRAGQuery(question3, 3, 0.8);
            printRAGResult(result3, "Техническое объяснение");

        } catch (Exception e) {
            System.out.println("Basic RAG error: " + e.getMessage());
        }
    }

    /**
     * Демонстрация продвинутых RAG сценариев
     */
    private static void demonstrateAdvancedRAG(RAGSystem ragSystem) {
        System.out.println("\n2. Advanced RAG Scenarios");
        System.out.println("=========================");

        try {
            // Сценарий 1: Многошаговый reasoning
            System.out.println("\n--- Сценарий 1: Многошаговый Reasoning RAG ---");
            String complexQuestion = """
                Представь что ты data scientist. Опиши полный pipeline машинного обучения:
                1. Сбор и подготовка данных
                2. Выбор и обучение модели
                3. Оценка и валидация
                4. Деплой и мониторинг
                """;
            RAGResultV1 complexResult = ragSystem.executeComplexRAGQuery(complexQuestion, 6, 0.65);
            printRAGResult(complexResult, "Многошаговый reasoning");

            // Сценарий 2: RAG с фильтрацией по источнику
            System.out.println("\n--- Сценарий 2: RAG с фильтрацией источников ---");
            String filteredQuestion = "Какие библиотеки Python используются для анализа данных?";
            RAGResultV1 filteredResult = ragSystem.executeFilteredRAGQuery(
                    filteredQuestion,
                    "programming", // фильтр по категории
                    4,
                    0.7
            );
            printRAGResult(filteredResult, "Фильтрованный RAG");

            // Сценарий 3: Гиперпараметрический RAG
            System.out.println("\n--- Сценарий 3: RAG с разными гиперпараметрами ---");
            demonstrateHyperparameterRAG(ragSystem);

        } catch (Exception e) {
            System.out.println("Advanced RAG error: " + e.getMessage());
        }
    }

    /**
     * Интерактивный RAG режим
     */
    private static void demonstrateInteractiveRAG(RAGSystem ragSystem) {
        System.out.println("\n3. Interactive RAG Chat");
        System.out.println("=======================");

        Scanner scanner = new Scanner(System.in);
        System.out.println("\n🚀 Запуск интерактивного RAG чата...");
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

                // Автоматическое определение оптимальных параметров RAG
                RAGParameters params = ragSystem.autoDetectParameters(userQuestion);
                System.out.println("⚙️  Параметры RAG: " + params);

                // Выполнение RAG запроса
                RAGResultV1 ragResult = ragSystem.executeRAGQuery(userQuestion, params.getTopK(), params.getSimilarityThreshold());

                // Вывод результатов
                System.out.println("\n📚 Найдено релевантных чанков: " + ragResult.getRetrievedDocuments().size());
                System.out.println("🤖 Ответ RAG:");
                System.out.println("--------------");
                System.out.println(ragResult.getGeneratedResponse());

                // Дополнительная информация
                if (ragSystem.isDebugMode()) {
                    System.out.println("\n🔍 Контекстные чанки:");
                    for (int i = 0; i < Math.min(2, ragResult.getRetrievedDocuments().size()); i++) {
                        JSONObject doc = ragResult.getRetrievedDocuments().get(i);
                        double similarity = doc.optDouble("similarity", 0.0);
                        String text = doc.optString("text", "").substring(0, Math.min(100, doc.optString("text", "").length()));
                        System.out.printf("  [%d] (similarity: %.3f) %s...%n", i + 1, similarity, text);
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
     * Демонстрация оценки качества RAG
     */
    private static void demonstrateRAGEvaluation(RAGSystem ragSystem) {
        System.out.println("\n4. RAG Evaluation");
        System.out.println("=================");

        try {
            // Тестовые вопросы для оценки
            String[] testQuestions = {
                    "Что такое машинное обучение?",
                    "Какие типы нейронных сетей существуют?",
                    "Чем отличается Python от Java?",
                    "Как работает градиентный спуск?",
                    "Что такое overfitting в машинном обучении?"
            };

            System.out.println("\n🧪 Запуск оценки RAG системы...");
            System.out.println("Тестовых вопросов: " + testQuestions.length);

            for (String question : testQuestions) {
                System.out.println("\n--- Оценка: '" + question + "' ---");

                RAGResultV1 result = ragSystem.executeRAGQuery(question, 3, 0.7);
                RAGEvaluationMetrics metrics = ragSystem.evaluateRAGResult(result, question);

                System.out.printf("📊 Метрики оценки:%n");
                System.out.printf("  - Retrieved Documents: %d%n", metrics.getRetrievedCount());
                System.out.printf("  - Avg Similarity: %.3f%n", metrics.getAverageSimilarity());
                System.out.printf("  - Max Similarity: %.3f%n", metrics.getMaxSimilarity());
                System.out.printf("  - Response Length: %d chars%n", metrics.getResponseLength());
                System.out.printf("  - Context Utilization: %.1f%%%n", metrics.getContextUtilization() * 100);

                if (metrics.getMaxSimilarity() < 0.5) {
                    System.out.println("  ⚠️  Низкая релевантность контекста");
                }
            }

        } catch (Exception e) {
            System.out.println("RAG evaluation error: " + e.getMessage());
        }
    }

    /**
     * Демонстрация RAG с разными гиперпараметрами
     */
    private static void demonstrateHyperparameterRAG(RAGSystem ragSystem) throws Exception {
        String testQuestion = "Объясни концепцию глубокого обучения";

        System.out.println("\n🔬 Сравнение разных параметров RAG:");
        System.out.println("Вопрос: " + testQuestion);

        // Тестируем разные значения topK
        int[] topKValues = {2, 4, 6};
        double[] thresholdValues = {0.5, 0.7, 0.9};

        for (int topK : topKValues) {
            for (double threshold : thresholdValues) {
                System.out.printf("%n--- topK=%d, threshold=%.1f ---%n", topK, threshold);

                RAGResultV1 result = ragSystem.executeRAGQuery(testQuestion, topK, threshold);

                System.out.printf("Retrieved: %d docs, Response: %d chars%n",
                        result.getRetrievedDocuments().size(),
                        result.getGeneratedResponse().length());

                // Краткий preview ответа
                String preview = result.getGeneratedResponse().length() > 100 ?
                        result.getGeneratedResponse().substring(0, 100) + "..." : result.getGeneratedResponse();
                System.out.println("Preview: " + preview);
            }
        }
    }

    /**
     * Вывод результата RAG
     */
    private static void printRAGResult(RAGResultV1 result, String scenario) {
        System.out.println("\n🎯 " + scenario + " Result:");
        System.out.println("=" .repeat(50));

        System.out.println("🤖 Сгенерированный ответ:");
        System.out.println(result.getGeneratedResponse());

        System.out.println("\n📊 Статистика:");
        System.out.printf("- Retrieved documents: %d%n", result.getRetrievedDocuments().size());
        System.out.printf("- Response length: %d characters%n", result.getGeneratedResponse().length());

        if (!result.getRetrievedDocuments().isEmpty()) {
            System.out.println("\n🔍 Top context documents:");
            for (int i = 0; i < Math.min(3, result.getRetrievedDocuments().size()); i++) {
                JSONObject doc = result.getRetrievedDocuments().get(i);
                double similarity = doc.optDouble("similarity", 0.0);
                String source = doc.optString("documentId", "unknown");
                String text = doc.optString("text", "");
                String preview = text.length() > 80 ? text.substring(0, 77) + "..." : text;

                System.out.printf("  %d. [%s] (similarity: %.3f)%n", i + 1, source, similarity);
                System.out.printf("     %s%n", preview);
            }
        }

        System.out.println("=" .repeat(50));
    }
}

/**
 * Основная RAG система
 */
class RAGSystem {
    private final BinaryVectorDatabase vectorDB;
    private final SQLParser sqlParser;
    private final OllamaKnowledgeClient ollamaClient;
    private final KnowledgeConfig knowledgeConfig;
    private boolean debugMode = true;

    public RAGSystem(String databasePath) throws Exception {
        this.knowledgeConfig = new KnowledgeConfig(
                "http://localhost:11434",
                "llama3.2",
                0.7,
                true,
                true
        );

        SemanticChunker semanticChunker = new SemanticChunker(
                knowledgeConfig.getOllamaUrl(),
                "all-minilm:22m",
                knowledgeConfig.getSimilarityThreshold()
        );

        this.vectorDB = new BinaryVectorDatabase(databasePath, semanticChunker);
        this.sqlParser = new SQLParser(vectorDB);
        this.ollamaClient = new OllamaKnowledgeClient(vectorDB, knowledgeConfig);
    }

    /**
     * Инициализация базы знаний
     */
    public void initializeKnowledgeBase() throws Exception {
        System.out.println("Initializing RAG knowledge base...");

        KnowledgeLoader loader = new KnowledgeLoader(vectorDB, knowledgeConfig);

        // Загрузка разнообразных знаний для RAG
        String[] knowledgeDomains = {
                // Машинное обучение
                """
            Машинное обучение (Machine Learning) - это подраздел искусственного интеллекта, который focuses на создании алгоритмов, способных обучаться на данных и делать прогнозы без явного программирования.
            
            Основные категории ML:
            1. Обучение с учителем (Supervised Learning): Алгоритм обучается на размеченных данных. Примеры: классификация, регрессия.
            2. Обучение без учителя (Unsupervised Learning): Алгоритм находит паттерны в неразмеченных данных. Примеры: кластеризация, снижение размерности.
            3. Обучение с подкреплением (Reinforcement Learning): Агент учится через взаимодействие со средой, получая reward за правильные действия.
            
            Популярные алгоритмы:
            - Линейная регрессия для прогнозирования непрерывных значений
            - Логистическая регрессия для бинарной классификации
            - Деревья решений и Random Forest для классификации и регрессии
            - Метод опорных векторов (SVM) для классификации
            - K-means для кластеризации данных
            """,

                // Глубокое обучение
                """
            Глубокое обучение (Deep Learning) - это подраздел машинного обучения, который использует нейронные сети с множеством слоев (глубокие сети).
            
            Архитектуры глубокого обучения:
            - Сверточные нейронные сети (CNN): Эффективны для обработки изображений и компьютерного зрения
            - Рекуррентные нейронные сети (RNN): Хорошо работают с последовательными данными (текст, временные ряды)
            - Трансформеры: Revolutionized обработку естественного языка (BERT, GPT, T5)
            - Автокодировщики (Autoencoders): Для снижения размерности и генерации данных
            
            Ключевые концепции:
            - Прямое распространение (Forward Propagation)
            - Обратное распространение ошибки (Backpropagation) 
            - Функции активации (ReLU, Sigmoid, Tanh)
            - Регуляризация (Dropout, L1/L2)
            - Оптимизаторы (SGD, Adam, RMSprop)
            """,

                // Программирование и инструменты
                """
            Python - основной язык для data science и машинного обучения.
            
            Ключевые библиотеки Python:
            - NumPy: Вычисления с многомерными массивами
            - Pandas: Анализ и manipulation табличных данных
            - Scikit-learn: Классические алгоритмы machine learning
            - TensorFlow: Глубокое обучение от Google
            - PyTorch: Глубокое обучение от Facebook, популярен в research
            - Matplotlib/Seaborn: Визуализация данных
            - NLTK/spaCy: Обработка естественного языка
            
            Java также используется в ML, особенно в enterprise-системах:
            - Deeplearning4j: Deep learning библиотека для Java
            - Weka: Коллекция алгоритмов ML
            - Apache Spark MLlib: Распределенное machine learning
            """
        };

        for (int i = 0; i < knowledgeDomains.length; i++) {
            loader.loadText(
                    knowledgeDomains[i],
                    "rag_domain_" + (i + 1),
                    new Object[]{"rag", "domain_" + (i + 1)},
                    500,
                    "RAG Knowledge Domain " + (i + 1)
            );
        }

        System.out.println("RAG knowledge base initialized with " + knowledgeDomains.length + " domains");
    }

    /**
     * Выполнение базового RAG запроса
     */
    public RAGResultV1 executeRAGQuery(String question, int topK, double similarityThreshold) throws Exception {
        // Шаг 1: Retrieval - поиск релевантных документов
        List<JSONObject> retrievedDocs = retrieveRelevantDocuments(question, topK, similarityThreshold);

        // Шаг 2: Augmentation - подготовка контекста
        String context = buildContextFromDocuments(retrievedDocs);

        // Шаг 3: Generation - генерация ответа с использованием контекста
        String generatedResponse = generateResponseWithContext(question, context);

        return new RAGResultV1(question, generatedResponse, retrievedDocs, context);
    }

    /**
     * Выполнение сложного RAG запроса с многошаговым reasoning
     */
    public RAGResultV1 executeComplexRAGQuery(String question, int topK, double similarityThreshold) throws Exception {
        // Многоэтапный retrieval для сложных вопросов
        List<JSONObject> allRetrievedDocs = new java.util.ArrayList<>();

        // Этап 1: Поиск по основным ключевым словам
        String[] mainKeywords = extractMainKeywords(question);
        for (String keyword : mainKeywords) {
            List<JSONObject> docs = sqlParser.execute(
                    String.format("SELECT * FROM rag WHERE text LIKE '%%%s%%' LIMIT %d", keyword, topK/2)
            );
            allRetrievedDocs.addAll(docs);
        }

        // Этап 2: Семантический поиск
        List<JSONObject> semanticDocs = sqlParser.semanticSearch(question, topK);
        allRetrievedDocs.addAll(semanticDocs);

        // Удаление дубликатов
        List<JSONObject> uniqueDocs = removeDuplicateDocuments(allRetrievedDocs);

        // Подготовка структурированного контекста
        String structuredContext = buildStructuredContext(uniqueDocs, question);

        // Генерация ответа
        String response = generateStructuredResponse(question, structuredContext);

        return new RAGResultV1(question, response, uniqueDocs, structuredContext);
    }

    /**
     * RAG запрос с фильтрацией по источнику
     */
    public RAGResultV1 executeFilteredRAGQuery(String question, String sourceFilter, int topK, double similarityThreshold) throws Exception {
        // Retrieval с фильтрацией
        List<JSONObject> retrievedDocs;

        if (sourceFilter != null && !sourceFilter.isEmpty()) {
            retrievedDocs = sqlParser.execute(
                    String.format("SELECT * FROM rag WHERE text LIKE '%%%s%%' AND documentId LIKE '%%%s%%' LIMIT %d",
                            extractMainKeywords(question)[0], sourceFilter, topK)
            );
        } else {
            retrievedDocs = retrieveRelevantDocuments(question, topK, similarityThreshold);
        }

        String context = buildContextFromDocuments(retrievedDocs);
        String response = generateResponseWithContext(question, context);

        return new RAGResultV1(question, response, retrievedDocs, context);
    }

    /**
     * Retrieval этап: поиск релевантных документов
     */
    private List<JSONObject> retrieveRelevantDocuments(String question, int topK, double similarityThreshold) throws Exception {
        // Используем гибридный поиск: семантический + ключевые слова
        List<JSONObject> semanticResults = sqlParser.semanticSearch(question, topK);
        List<JSONObject> keywordResults = sqlParser.hybridSearch(question, topK);

        // Объединяем и фильтруем по порогу схожести
        List<JSONObject> allResults = new java.util.ArrayList<>();
        allResults.addAll(semanticResults);
        allResults.addAll(keywordResults);

        // Фильтрация по порогу схожести
        List<JSONObject> filteredResults = new java.util.ArrayList<>();
        for (JSONObject doc : allResults) {
            double similarity = doc.optDouble("similarity", 0.0);
            if (similarity >= similarityThreshold) {
                filteredResults.add(doc);
            }
        }

        // Удаление дубликатов и ограничение количества
        return removeDuplicateDocuments(filteredResults).stream()
                .limit(topK)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Подготовка контекста из найденных документов
     */
    private String buildContextFromDocuments(List<JSONObject> documents) {
        if (documents.isEmpty()) {
            return "Релевантная информация не найдена в базе знаний.";
        }

        StringBuilder context = new StringBuilder();
        context.append("Релевантная информация из базы знаний:\n\n");

        for (int i = 0; i < documents.size(); i++) {
            JSONObject doc = documents.get(i);
            String text = doc.optString("text", doc.optString("content", ""));
            double similarity = doc.optDouble("similarity", 0.0);
            String source = doc.optString("documentId", "unknown");

            if (!text.trim().isEmpty()) {
                context.append("[Документ ").append(i + 1);
                context.append(" | Источник: ").append(source);
                context.append(" | Релевантность: ").append(String.format("%.3f", similarity));
                context.append("]\n");
                context.append(text).append("\n\n");
            }
        }

        return context.toString();
    }

    /**
     * Генерация ответа с использованием контекста
     */
    private String generateResponseWithContext(String question, String context) {
        String prompt = String.format("""
            Ты - AI ассистент с доступом к базе знаний. Используй предоставленную информацию для точного ответа на вопрос.
            
            %s
            
            Вопрос пользователя: %s
            
            Инструкции:
            - Ответь максимально точно на основе предоставленной информации
            - Если информации недостаточно, укажи это явно
            - Будь конкретен и информативен
            - Сохраняй профессиональный тон
            - Если в информации есть противоречия, укажи на это
            
            Ответ:
            """, context, question);

        return ollamaClient.generateResponse(prompt);
    }

    /**
     * Автоматическое определение параметров RAG на основе вопроса
     */
    public RAGParameters autoDetectParameters(String question) {
        RAGParameters params = new RAGParameters();

        // Эвристики для определения параметров
        if (question.length() > 100) {
            params.setTopK(6); // Сложные вопросы требуют больше контекста
            params.setSimilarityThreshold(0.6); // Более низкий порог для широкого охвата
        } else if (question.toLowerCase().contains("сравни") || question.toLowerCase().contains("разница")) {
            params.setTopK(5); // Для сравнения нужно несколько источников
            params.setSimilarityThreshold(0.7);
        } else if (question.toLowerCase().contains("как") || question.toLowerCase().contains("какой")) {
            params.setTopK(4); // Для объяснительных вопросов
            params.setSimilarityThreshold(0.75);
        } else {
            params.setTopK(3); // По умолчанию
            params.setSimilarityThreshold(0.7);
        }

        return params;
    }

    /**
     * Оценка результата RAG
     */
    public RAGEvaluationMetrics evaluateRAGResult(RAGResultV1 result, String originalQuestion) {
        RAGEvaluationMetrics metrics = new RAGEvaluationMetrics();

        metrics.setRetrievedCount(result.getRetrievedDocuments().size());
        metrics.setResponseLength(result.getGeneratedResponse().length());

        // Вычисление средней и максимальной схожести
        double totalSimilarity = 0;
        double maxSimilarity = 0;
        for (JSONObject doc : result.getRetrievedDocuments()) {
            double similarity = doc.optDouble("similarity", 0.0);
            totalSimilarity += similarity;
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
            }
        }

        metrics.setAverageSimilarity(result.getRetrievedDocuments().isEmpty() ? 0 : totalSimilarity / result.getRetrievedDocuments().size());
        metrics.setMaxSimilarity(maxSimilarity);

        // Эвристика для оценки использования контекста
        double contextUtilization = Math.min(1.0, result.getRetrievedDocuments().size() / 5.0);
        metrics.setContextUtilization(contextUtilization);

        return metrics;
    }

    /**
     * Вспомогательные методы
     */
    private String[] extractMainKeywords(String text) {
        // Простая эвристика для извлечения ключевых слов
        return text.toLowerCase()
                .replaceAll("[^а-яa-z0-9\\s]", "")
                .split("\\s+");
    }

    private List<JSONObject> removeDuplicateDocuments(List<JSONObject> documents) {
        List<JSONObject> uniqueDocs = new java.util.ArrayList<>();
        java.util.Set<String> seenTexts = new java.util.HashSet<>();

        for (JSONObject doc : documents) {
            String text = doc.optString("text", doc.optString("content", ""));
            if (!seenTexts.contains(text) && !text.trim().isEmpty()) {
                seenTexts.add(text);
                uniqueDocs.add(doc);
            }
        }

        return uniqueDocs;
    }

    private String buildStructuredContext(List<JSONObject> documents, String question) {
        // Более структурированная подготовка контекста для сложных вопросов
        StringBuilder context = new StringBuilder();
        context.append("Структурированная информация для ответа на вопрос: ").append(question).append("\n\n");

        for (int i = 0; i < documents.size(); i++) {
            JSONObject doc = documents.get(i);
            context.append("=== Блок информации ").append(i + 1).append(" ===\n");
            context.append(doc.optString("text", "")).append("\n\n");
        }

        return context.toString();
    }

    private String generateStructuredResponse(String question, String structuredContext) {
        String prompt = String.format("""
            На основе структурированной информации ниже, дай развернутый и хорошо организованный ответ на вопрос.
            
            Вопрос: %s
            
            Структурированная информация:
            %s
            
            Требования к ответу:
            - Используй markdown для форматирования
            - Структурируй ответ логическими разделами
            - Включи конкретные примеры и детали
            - Сделай выводы и рекомендации если уместно
            
            Ответ:
            """, question, structuredContext);

        return ollamaClient.generateResponse(prompt);
    }

    public boolean isDebugMode() { return debugMode; }
    public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }
    public void close() { vectorDB.close(); }
}

/**
 * Классы для хранения данных RAG
 */
class RAGResultV1 {
    private final String originalQuestion;
    private final String generatedResponse;
    private final List<JSONObject> retrievedDocuments;
    private final String contextUsed;

    public RAGResultV1(String originalQuestion, String generatedResponse,
                     List<JSONObject> retrievedDocuments, String contextUsed) {
        this.originalQuestion = originalQuestion;
        this.generatedResponse = generatedResponse;
        this.retrievedDocuments = retrievedDocuments;
        this.contextUsed = contextUsed;
    }

    // Getters
    public String getOriginalQuestion() { return originalQuestion; }
    public String getGeneratedResponse() { return generatedResponse; }
    public List<JSONObject> getRetrievedDocuments() { return retrievedDocuments; }
    public String getContextUsed() { return contextUsed; }
}

class RAGParameters {
    private int topK = 3;
    private double similarityThreshold = 0.7;

    // Getters and Setters
    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }
    public double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }

    @Override
    public String toString() {
        return String.format("topK=%d, threshold=%.2f", topK, similarityThreshold);
    }
}

class RAGEvaluationMetrics {
    private int retrievedCount;
    private double averageSimilarity;
    private double maxSimilarity;
    private int responseLength;
    private double contextUtilization;

    // Getters and Setters
    public int getRetrievedCount() { return retrievedCount; }
    public void setRetrievedCount(int retrievedCount) { this.retrievedCount = retrievedCount; }
    public double getAverageSimilarity() { return averageSimilarity; }
    public void setAverageSimilarity(double averageSimilarity) { this.averageSimilarity = averageSimilarity; }
    public double getMaxSimilarity() { return maxSimilarity; }
    public void setMaxSimilarity(double maxSimilarity) { this.maxSimilarity = maxSimilarity; }
    public int getResponseLength() { return responseLength; }
    public void setResponseLength(int responseLength) { this.responseLength = responseLength; }
    public double getContextUtilization() { return contextUtilization; }
    public void setContextUtilization(double contextUtilization) { this.contextUtilization = contextUtilization; }
}