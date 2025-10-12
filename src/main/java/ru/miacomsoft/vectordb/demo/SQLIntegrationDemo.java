package ru.miacomsoft.vectordb.demo;

import ru.miacomsoft.vectordb.core.*;
import ru.miacomsoft.vectordb.knowledge.KnowledgeConfig;
import ru.miacomsoft.vectordb.knowledge.KnowledgeLoader;

import java.util.List;
import org.json.JSONObject;

/**
 * Демонстрация интеграции SQL с Knowledge системой
 */
public class SQLIntegrationDemo {
    public static void main(String[] args) {
        System.out.println("=== SQL Integration with Knowledge System Demo ===");

        try {
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
            BinaryVectorDatabase vectorDB = new BinaryVectorDatabase("./data/sql_integration_demo", semanticChunker);
            SQLParser sqlParser = new SQLParser(vectorDB);

            // Загрузка знаний через KnowledgeLoader
            KnowledgeLoader knowledgeLoader = new KnowledgeLoader(vectorDB, knowledgeConfig);
            loadKnowledgeData(knowledgeLoader);

            // Демонстрация SQL запросов к знаниям
            demonstrateKnowledgeQueries(sqlParser);

            // Интеграция с семантическим поиском
            demonstrateIntegratedSearch(sqlParser, knowledgeLoader);

            vectorDB.close();

        } catch (Exception e) {
            System.out.println("Integration demo error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadKnowledgeData(KnowledgeLoader knowledgeLoader) throws Exception {
        System.out.println("\nLoading knowledge data...");

        String knowledgeText = """
            Машинное обучение - это раздел искусственного интеллекта, который позволяет компьютерам обучаться на данных.
            Глубокое обучение использует нейронные сети с множеством слоев для изучения сложных паттернов.
            Python популярен в data science благодаря библиотекам как NumPy, Pandas и Scikit-learn.
            Java используется для enterprise приложений и имеет сильную систему типов.
            SQL необходим для работы с реляционными базами данных и выполнения запросов.
            Векторные базы данных хранят embedding векторы и поддерживают семантический поиск.
            """;

        knowledgeLoader.loadText(
                knowledgeText,
                "knowledge_base",
                new Object[]{"knowledge", "ai_tech"},
                500,
                "manual_input"
        );

        System.out.println("Knowledge data loaded successfully");
    }

    private static void demonstrateKnowledgeQueries(SQLParser sqlParser) {
        System.out.println("\n1. Knowledge Queries with SQL");
        System.out.println("==============================");

        try {
            // Поиск в знаниях с помощью SQL
            System.out.println("\n--- Поиск знаний о машинном обучении ---");
            List<JSONObject> results1 = sqlParser.execute(
                    "SELECT * FROM knowledge WHERE text LIKE '%машинное обучение%'"
            );
            printIntegrationResults(results1, "Машинное обучение");

            // Поиск по нескольким темам
            System.out.println("\n--- Поиск знаний о Python и Java ---");
            List<JSONObject> results2 = sqlParser.execute(
                    "SELECT text FROM knowledge WHERE text LIKE '%Python%' OR text LIKE '%Java%' LIMIT 3"
            );
            printIntegrationResults(results2, "Python и Java");

            // Семантический поиск в знаниях
            System.out.println("\n--- Семантический поиск в знаниях ---");
            List<JSONObject> semanticResults = sqlParser.semanticSearch("программирование данные анализ", 3);
            printIntegrationResults(semanticResults, "Семантический поиск");

        } catch (Exception e) {
            System.out.println("Knowledge queries error: " + e.getMessage());
        }
    }

    private static void demonstrateIntegratedSearch(SQLParser sqlParser, KnowledgeLoader knowledgeLoader) {
        System.out.println("\n2. Integrated Search Strategies");
        System.out.println("================================");

        try {
            // Комплексный поиск с использованием разных методов
            String query = "нейронные сети и базы данных";

            System.out.println("\nКомплексный поиск: '" + query + "'");
            System.out.println("=" .repeat(50));

            // 1. Точный поиск
            System.out.println("\n1. Точный поиск (SQL LIKE):");
            List<JSONObject> exactResults = sqlParser.execute(
                    "SELECT * FROM knowledge WHERE text LIKE '%нейрон%' OR text LIKE '%баз%данн%'"
            );
            printIntegrationResults(exactResults, "Точные совпадения");

            // 2. Семантический поиск
            System.out.println("\n2. Семантический поиск:");
            List<JSONObject> semanticResults = sqlParser.semanticSearch(query, 3);
            printIntegrationResults(semanticResults, "Семантические совпадения");

            // 3. Гибридный поиск
            System.out.println("\n3. Гибридный поиск:");
            List<JSONObject> hybridResults = sqlParser.hybridSearch(query, 4);
            printIntegrationResults(hybridResults, "Гибридные результаты");

            // Статистика знаний
            System.out.println("\n4. Статистика базы знаний:");
            knowledgeLoader.printKnowledgeStats();

        } catch (Exception e) {
            System.out.println("Integrated search error: " + e.getMessage());
        }
    }

    private static void printIntegrationResults(List<JSONObject> results, String searchType) {
        System.out.println("\n" + searchType + " (" + results.size() + " results):");

        for (int i = 0; i < results.size(); i++) {
            JSONObject result = results.get(i);
            System.out.println((i + 1) + ". " + formatIntegrationResult(result));
        }
    }

    private static String formatIntegrationResult(JSONObject result) {
        StringBuilder sb = new StringBuilder();

        if (result.has("text")) {
            String text = result.getString("text");
            // Обрезаем длинный текст для читаемости
            String displayText = text.length() > 80 ? text.substring(0, 77) + "..." : text;
            sb.append(displayText);
        }

        if (result.has("similarity")) {
            sb.append(" [similarity: ").append(String.format("%.3f", result.getDouble("similarity"))).append("]");
        }

        if (result.has("documentId")) {
            sb.append(" [doc: ").append(result.getString("documentId")).append("]");
        }

        return sb.toString();
    }
}