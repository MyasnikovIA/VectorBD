package ru.miacomsoft.vectordb.demo;

import ru.miacomsoft.vectordb.core.*;
import ru.miacomsoft.vectordb.knowledge.KnowledgeConfig;
import ru.miacomsoft.vectordb.knowledge.KnowledgeLoader;
import ru.miacomsoft.vectordb.knowledge.OllamaKnowledgeClient;
import org.json.JSONObject;

import java.util.List;
import java.util.Scanner;

/**
 * Демонстрация использования SQL запросов для подготовки контекста и создания AI промптов
 */
public class SQLPromptDemo {
    public static void main(String[] args) {
        System.out.println("=== SQL Prompt Context Demo ===");

        try {
            // Инициализация компонентов
            KnowledgeConfig knowledgeConfig = new KnowledgeConfig(
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
            BinaryVectorDatabase vectorDB = new BinaryVectorDatabase("./data/sql_prompt_demo", semanticChunker);
            SQLParser sqlParser = new SQLParser(vectorDB);
            OllamaKnowledgeClient ollamaClient = new OllamaKnowledgeClient(vectorDB, knowledgeConfig);

            // Загрузка знаний
            loadKnowledgeBase(vectorDB, knowledgeConfig);

            // Демонстрация различных сценариев
            demonstrateContextPreparation(sqlParser, ollamaClient);
            demonstrateInteractivePrompting(sqlParser, ollamaClient);
            demonstrateAdvancedPromptScenarios(sqlParser, ollamaClient);

            vectorDB.close();

        } catch (Exception e) {
            System.out.println("Demo error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Загрузка базы знаний
     */
    private static void loadKnowledgeBase(BinaryVectorDatabase vectorDB, KnowledgeConfig knowledgeConfig) throws Exception {
        System.out.println("\nLoading knowledge base...");

        KnowledgeLoader loader = new KnowledgeLoader(vectorDB, knowledgeConfig);

        // Техническая документация
        String techDocs = """
            Машинное обучение (Machine Learning) - это подраздел искусственного интеллекта, который focuses на создании алгоритмов, способных обучаться на данных и делать прогнозы.
            
            Основные типы машинного обучения:
            - Обучение с учителем (Supervised Learning): алгоритм обучается на размеченных данных
            - Обучение без учителя (Unsupervised Learning): алгоритм находит паттерны в неразмеченных данных  
            - Обучение с подкреплением (Reinforcement Learning): агент учится через взаимодействие со средой
            
            Популярные алгоритмы машинного обучения:
            - Линейная регрессия для прогнозирования числовых значений
            - Логистическая регрессия для классификации
            - Деревья решений для классификации и регрессии
            - Метод опорных векторов (SVM) для классификации
            - K-ближайших соседей для классификации и регрессии
            
            Глубокое обучение (Deep Learning) использует нейронные сети с множеством слоев.
            Сверточные нейронные сети (CNN) эффективны для обработки изображений.
            Рекуррентные нейронные сети (RNN) хорошо работают с последовательными данными.
            Трансформеры revolutionized обработку естественного языка.
            """;

        // Документация по программированию
        String programmingDocs = """
            Python - это интерпретируемый язык программирования высокого уровня с динамической типизацией.
            Основные особенности Python: простота синтаксиса, богатая стандартная библиотека, поддержка multiple парадигм.
            
            Ключевые библиотеки Python для Data Science:
            - NumPy: вычисления с многомерными массивами
            - Pandas: анализ и manipulation табличных данных  
            - Scikit-learn: machine learning алгоритмы
            - TensorFlow и PyTorch: глубокое обучение
            - Matplotlib и Seaborn: визуализация данных
            
            Java - это объектно-ориентированный язык программирования со статической типизацией.
            Java работает на виртуальной машине JVM, что обеспечивает кроссплатформенность.
            
            SQL (Structured Query Language) - это язык для работы с реляционными базами данных.
            Основные команды SQL: SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP.
            """;

        // Загрузка знаний
        loader.loadText(techDocs, "ml_knowledge", new Object[]{"knowledge", "machine_learning"}, 400, "ML Docs");
        loader.loadText(programmingDocs, "programming_knowledge", new Object[]{"knowledge", "programming"}, 400, "Programming Docs");

        System.out.println("Knowledge base loaded successfully");
    }

    /**
     * Демонстрация подготовки контекста с помощью SQL
     */
    private static void demonstrateContextPreparation(SQLParser sqlParser, OllamaKnowledgeClient ollamaClient) {
        System.out.println("\n1. Context Preparation with SQL");
        System.out.println("================================");

        try {
            // Сценарий 1: Подготовка контекста для ответа на технический вопрос
            String technicalQuestion = "Какие существуют типы машинного обучения и их применение?";
            System.out.println("\n--- Сценарий 1: Технический вопрос ---");
            System.out.println("Вопрос: " + technicalQuestion);

            // Используем SQL для поиска релевантного контекста
            List<JSONObject> contextResults = sqlParser.execute(
                    "SELECT text FROM knowledge WHERE text LIKE '%типы машинного обучения%' OR text LIKE '%supervised%' OR text LIKE '%unsupervised%' LIMIT 5"
            );

            String preparedPrompt = preparePromptWithContext(technicalQuestion, contextResults);
            System.out.println("\nПодготовленный промпт:");
            System.out.println("----------------------");
            System.out.println(preparedPrompt);

            // Генерация ответа с использованием контекста
            System.out.println("\nОтвет AI:");
            System.out.println("---------");
            String aiResponse = ollamaClient.generateResponse(preparedPrompt);
            System.out.println(aiResponse);

            // Сценарий 2: Сравнение технологий
            String comparisonQuestion = "Сравни Python и Java для машинного обучения";
            System.out.println("\n--- Сценарий 2: Сравнение технологий ---");
            System.out.println("Вопрос: " + comparisonQuestion);

            List<JSONObject> comparisonContext = sqlParser.execute(
                    "SELECT text FROM knowledge WHERE text LIKE '%Python%' OR text LIKE '%Java%' LIMIT 6"
            );

            String comparisonPrompt = prepareComparisonPrompt(comparisonQuestion, comparisonContext);
            String comparisonResponse = ollamaClient.generateResponse(comparisonPrompt);
            System.out.println("\nОтвет AI:");
            System.out.println("---------");
            System.out.println(comparisonResponse);

        } catch (Exception e) {
            System.out.println("Context preparation error: " + e.getMessage());
        }
    }

    /**
     * Интерактивное создание промптов
     */
    private static void demonstrateInteractivePrompting(SQLParser sqlParser, OllamaKnowledgeClient ollamaClient) {
        System.out.println("\n2. Interactive Prompt Creation");
        System.out.println("==============================");

        Scanner scanner = new Scanner(System.in);

        try {
            System.out.println("\nВведите ваш вопрос (или 'quit' для выхода):");

            while (true) {
                System.out.print("\n❓ Ваш вопрос: ");
                String userQuestion = scanner.nextLine().trim();

                if (userQuestion.equalsIgnoreCase("quit") || userQuestion.equalsIgnoreCase("exit")) {
                    break;
                }

                if (userQuestion.isEmpty()) {
                    continue;
                }

                // Автоматическое определение типа запроса и подготовка контекста
                PromptContext context = prepareDynamicContext(sqlParser, userQuestion);

                System.out.println("\n📚 Найдено релевантных документов: " + context.getContextResults().size());
                System.out.println("🎯 Стратегия поиска: " + context.getSearchStrategy());

                // Создание промпта на основе контекста
                String dynamicPrompt = createDynamicPrompt(userQuestion, context);

                System.out.println("\n🧠 Сгенерированный промпт:");
                System.out.println("--------------------------");
                System.out.println(dynamicPrompt);

                // Генерация ответа
                System.out.println("\n🤖 Ответ AI:");
                System.out.println("------------");
                String response = ollamaClient.generateResponse(dynamicPrompt);
                System.out.println(response);

                // Статистика
                System.out.println("\n📊 Статистика:");
                System.out.println("Контекстные документы: " + context.getContextResults().size());
                System.out.println("Длина промпта: " + dynamicPrompt.length() + " символов");
            }

        } catch (Exception e) {
            System.out.println("Interactive prompting error: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }

    /**
     * Продвинутые сценарии промптов
     */
    private static void demonstrateAdvancedPromptScenarios(SQLParser sqlParser, OllamaKnowledgeClient ollamaClient) {
        System.out.println("\n3. Advanced Prompt Scenarios");
        System.out.println("============================");

        try {
            // Сценарий 1: Генерация учебных материалов
            System.out.println("\n--- Сценарий 1: Генерация учебных материалов ---");
            String learningTopic = "нейронные сети для начинающих";
            System.out.println("Тема: " + learningTopic);

            List<JSONObject> learningContext = sqlParser.semanticSearch(learningTopic, 4);
            String learningPrompt = createLearningPrompt(learningTopic, learningContext);

            System.out.println("\nУчебный материал:");
            System.out.println("-----------------");
            String learningMaterial = ollamaClient.generateResponse(learningPrompt);
            System.out.println(learningMaterial);

            // Сценарий 2: Техническое интервью
            System.out.println("\n--- Сценарий 2: Подготовка к техническому интервью ---");
            String interviewTopic = "машинное обучение";
            List<JSONObject> interviewContext = sqlParser.execute(
                    "SELECT text FROM knowledge WHERE text LIKE '%машинное обучение%' OR text LIKE '%алгоритм%' LIMIT 5"
            );

            String interviewPrompt = createInterviewPrompt(interviewTopic, interviewContext, 5);
            System.out.println("\nВопросы для интервью:");
            System.out.println("---------------------");
            String interviewQuestions = ollamaClient.generateResponse(interviewPrompt);
            System.out.println(interviewQuestions);

            // Сценарий 3: Суммаризация знаний
            System.out.println("\n--- Сценарий 3: Суммаризация знаний ---");
            String summaryTopic = "глубокое обучение";
            List<JSONObject> summaryContext = sqlParser.hybridSearch(summaryTopic, 6);
            String summaryPrompt = createSummaryPrompt(summaryTopic, summaryContext);

            System.out.println("\nСуммаризация:");
            System.out.println("-------------");
            String summary = ollamaClient.generateResponse(summaryPrompt);
            System.out.println(summary);

        } catch (Exception e) {
            System.out.println("Advanced scenarios error: " + e.getMessage());
        }
    }

    /**
     * Подготовка базового промпта с контекстом
     */
    private static String preparePromptWithContext(String question, List<JSONObject> contextResults) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Используй следующую информацию для ответа на вопрос:\n\n");
        prompt.append("=== КОНТЕКСТ ===\n");

        if (contextResults.isEmpty()) {
            prompt.append("Контекстная информация не найдена. Ответь на вопрос используя свои знания.\n");
        } else {
            for (int i = 0; i < contextResults.size(); i++) {
                JSONObject result = contextResults.get(i);
                String text = result.optString("text", result.optString("content", ""));
                if (!text.isEmpty()) {
                    prompt.append("[Источник ").append(i + 1).append("]\n");
                    prompt.append(text).append("\n\n");
                }
            }
        }

        prompt.append("=== ВОПРОС ===\n");
        prompt.append(question).append("\n\n");
        prompt.append("=== ИНСТРУКЦИИ ===\n");
        prompt.append("- Будь точным и информативным\n");
        prompt.append("- Используй информацию из контекста когда это уместно\n");
        prompt.append("- Если информации недостаточно, укажи это\n");
        prompt.append("- Форматируй ответ для лучшей читаемости\n\n");
        prompt.append("=== ОТВЕТ ===\n");

        return prompt.toString();
    }

    /**
     * Промпт для сравнения технологий
     */
    private static String prepareComparisonPrompt(String question, List<JSONObject> contextResults) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Сравни технологии на основе предоставленной информации. Структурируй ответ в виде таблицы сравнения.\n\n");
        prompt.append("=== ИСХОДНАЯ ИНФОРМАЦИЯ ===\n");

        for (int i = 0; i < contextResults.size(); i++) {
            JSONObject result = contextResults.get(i);
            String text = result.optString("text", result.optString("content", ""));
            if (!text.isEmpty()) {
                prompt.append("[Документ ").append(i + 1).append("]\n");
                prompt.append(text).append("\n\n");
            }
        }

        prompt.append("=== ЗАДАНИЕ ===\n");
        prompt.append(question).append("\n\n");
        prompt.append("=== ТРЕБОВАНИЯ К ОТВЕТУ ===\n");
        prompt.append("- Создай подробное сравнение\n");
        prompt.append("- Включи сильные и слабые стороны каждой технологии\n");
        prompt.append("- Укажи области применения\n");
        prompt.append("- Предоставь рекомендации по выбору\n");
        prompt.append("- Используй табличный формат для наглядности\n\n");
        prompt.append("=== ОТВЕТ ===\n");

        return prompt.toString();
    }

    /**
     * Динамическая подготовка контекста на основе запроса
     */
    private static PromptContext prepareDynamicContext(SQLParser sqlParser, String question) throws Exception {
        PromptContext context = new PromptContext();

        // Анализ вопроса для определения стратегии поиска
        if (question.toLowerCase().contains("сравни") || question.toLowerCase().contains("vs") || question.toLowerCase().contains("против")) {
            context.setSearchStrategy("COMPARISON");
            // Поиск по ключевым словам для сравнения
            String[] keywords = extractComparisonKeywords(question);
            for (String keyword : keywords) {
                List<JSONObject> results = sqlParser.execute(
                        "SELECT text FROM knowledge WHERE text LIKE '%" + keyword + "%' LIMIT 3"
                );
                context.addContextResults(results);
            }
        } else if (question.toLowerCase().contains("как") || question.toLowerCase().contains("какой") || question.toLowerCase().contains("что")) {
            context.setSearchStrategy("EXPLANATION");
            // Семантический поиск для объяснительных вопросов
            List<JSONObject> results = sqlParser.semanticSearch(question, 4);
            context.setContextResults(results);
        } else if (question.toLowerCase().contains("пример") || question.toLowerCase().contains("код")) {
            context.setSearchStrategy("EXAMPLES");
            // Поиск практических примеров
            List<JSONObject> results = sqlParser.execute(
                    "SELECT text FROM knowledge WHERE text LIKE '%пример%' OR text LIKE '%код%' LIMIT 4"
            );
            context.setContextResults(results);
        } else {
            context.setSearchStrategy("HYBRID");
            // Гибридный поиск по умолчанию
            List<JSONObject> results = sqlParser.hybridSearch(question, 5);
            context.setContextResults(results);
        }

        return context;
    }

    /**
     * Создание динамического промпта на основе контекста
     */
    private static String createDynamicPrompt(String question, PromptContext context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Ты - эксперт в области технологий и программирования. ");

        switch (context.getSearchStrategy()) {
            case "COMPARISON":
                prompt.append("Сравни технологии объективно и структурированно.\n\n");
                break;
            case "EXPLANATION":
                prompt.append("Объясни концепцию ясно и подробно, используя примеры.\n\n");
                break;
            case "EXAMPLES":
                prompt.append("Предоставь практические примеры и код где это уместно.\n\n");
                break;
            default:
                prompt.append("Ответь на вопрос информативно и точно.\n\n");
        }

        prompt.append("=== РЕЛЕВАНТНАЯ ИНФОРМАЦИЯ ===\n");

        if (context.getContextResults().isEmpty()) {
            prompt.append("Специфическая информация не найдена. Используй свои знания.\n");
        } else {
            for (int i = 0; i < context.getContextResults().size(); i++) {
                JSONObject result = context.getContextResults().get(i);
                String text = result.optString("text", result.optString("content", ""));
                double similarity = result.optDouble("similarity", 0.0);

                if (!text.isEmpty()) {
                    prompt.append("[Документ ").append(i + 1);
                    if (similarity > 0) {
                        prompt.append(", релевантность: ").append(String.format("%.2f", similarity));
                    }
                    prompt.append("]\n");
                    prompt.append(text).append("\n\n");
                }
            }
        }

        prompt.append("=== ВОПРОС ПОЛЬЗОВАТЕЛЯ ===\n");
        prompt.append(question).append("\n\n");
        prompt.append("=== ОТВЕТ ===\n");

        return prompt.toString();
    }

    /**
     * Промпт для создания учебных материалов
     */
    private static String createLearningPrompt(String topic, List<JSONObject> contextResults) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Создай учебный материал по теме: ").append(topic).append("\n\n");
        prompt.append("Используй следующую информацию как основу:\n\n");

        for (int i = 0; i < contextResults.size(); i++) {
            JSONObject result = contextResults.get(i);
            String text = result.optString("text", result.optString("content", ""));
            if (!text.isEmpty()) {
                prompt.append("[Источник ").append(i + 1).append("]\n");
                prompt.append(text).append("\n\n");
            }
        }

        prompt.append("=== ТРЕБОВАНИЯ К МАТЕРИАЛУ ===\n");
        prompt.append("- Структурируй материал логически\n");
        prompt.append("- Объясняй концепции простым языком\n");
        prompt.append("- Включай практические примеры\n");
        prompt.append("- Добавь ключевые выводы\n");
        prompt.append("- Сделай материал engaging для beginners\n\n");
        prompt.append("=== УЧЕБНЫЙ МАТЕРИАЛ ===\n");

        return prompt.toString();
    }

    /**
     * Промпт для подготовки вопросов к интервью
     */
    private static String createInterviewPrompt(String topic, List<JSONObject> contextResults, int numQuestions) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Сгенерируй ").append(numQuestions).append(" вопросов для технического интервью по теме: ").append(topic).append("\n\n");
        prompt.append("Контекстная информация:\n");

        for (int i = 0; i < contextResults.size(); i++) {
            JSONObject result = contextResults.get(i);
            String text = result.optString("text", result.optString("content", ""));
            if (!text.isEmpty()) {
                prompt.append("[Документ ").append(i + 1).append("]\n");
                prompt.append(text).append("\n\n");
            }
        }

        prompt.append("=== ТРЕБОВАНИЯ К ВОПРОСАМ ===\n");
        prompt.append("- Вопросы должны охватывать разные уровни сложности\n");
        prompt.append("- Включи теоретические и практические вопросы\n");
        prompt.append("- Добавь вопросы на понимание концепций\n");
        prompt.append("- Убедись, что вопросы релевантны теме\n");
        prompt.append("- Формат: нумерованный список\n\n");
        prompt.append("=== ВОПРОСЫ ДЛЯ ИНТЕРВЬЮ ===\n");

        return prompt.toString();
    }

    /**
     * Промпт для суммаризации
     */
    private static String createSummaryPrompt(String topic, List<JSONObject> contextResults) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Суммаризируй информацию по теме: ").append(topic).append("\n\n");
        prompt.append("Исходные материалы:\n");

        for (int i = 0; i < contextResults.size(); i++) {
            JSONObject result = contextResults.get(i);
            String text = result.optString("text", result.optString("content", ""));
            if (!text.isEmpty()) {
                prompt.append("[Материал ").append(i + 1).append("]\n");
                prompt.append(text).append("\n\n");
            }
        }

        prompt.append("=== ИНСТРУКЦИИ ДЛЯ СУММАРИЗАЦИИ ===\n");
        prompt.append("- Выдели ключевые идеи и концепции\n");
        prompt.append("- Сохрани важные детали и факты\n");
        prompt.append("- Используй четкую и структурированную форму\n");
        prompt.append("- Сделай акцент на практическом применении\n");
        prompt.append("- Объем: 2-3 абзаца\n\n");
        prompt.append("=== СУММАРИЗАЦИЯ ===\n");

        return prompt.toString();
    }

    /**
     * Извлечение ключевых слов для сравнения
     */
    private static String[] extractComparisonKeywords(String question) {
        // Простая эвристика для извлечения ключевых слов сравнения
        String cleaned = question.toLowerCase()
                .replace("сравни", "")
                .replace("vs", "")
                .replace("против", "")
                .replace("и", ",")
                .replace("или", ",")
                .trim();

        return cleaned.split(",");
    }

    /**
     * Вспомогательный класс для хранения контекста промпта
     */
    private static class PromptContext {
        private List<JSONObject> contextResults;
        private String searchStrategy;

        public PromptContext() {
            this.contextResults = new java.util.ArrayList<>();
            this.searchStrategy = "DEFAULT";
        }

        public List<JSONObject> getContextResults() { return contextResults; }
        public void setContextResults(List<JSONObject> contextResults) { this.contextResults = contextResults; }
        public void addContextResults(List<JSONObject> results) { this.contextResults.addAll(results); }

        public String getSearchStrategy() { return searchStrategy; }
        public void setSearchStrategy(String searchStrategy) { this.searchStrategy = searchStrategy; }
    }
}