package ru.miacomsoft.vectordb.demo;

import ru.miacomsoft.vectordb.core.*;
import org.json.JSONObject;

import java.util.List;

/**
 * Продвинутая демонстрация SQL возможностей VectorBD
 */
public class AdvancedSQLDemo {
    public static void main(String[] args) {
        System.out.println("=== Advanced VectorBD SQL Demo ===");

        try {
            SemanticChunker semanticChunker = new SemanticChunker(
                    "http://localhost:11434",
                    "all-minilm:22m",
                    0.7
            );
            BinaryVectorDatabase vectorDB = new BinaryVectorDatabase("./data/advanced_sql_demo", semanticChunker);
            SQLParser sqlParser = new SQLParser(vectorDB);

            // Загрузка разнообразных данных
            loadAdvancedData(vectorDB);

            // Демонстрация сложных запросов
            demonstrateComplexQueries(sqlParser);

            // Демонстрация работы с индексами
            demonstrateIndexOperations(sqlParser);

            // Демонстрация комбинированного поиска
            demonstrateCombinedSearch(sqlParser);

            vectorDB.close();

        } catch (Exception e) {
            System.out.println("Advanced demo error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Загрузка разнообразных демонстрационных данных
     */
    private static void loadAdvancedData(BinaryVectorDatabase vectorDB) throws Exception {
        System.out.println("\nLoading advanced demo data...");

        // Документы разных категорий
        String[] aiDocuments = {
                "Машинное обучение включает алгоритмы обучения с учителем и без учителя.",
                "Нейронные сети состоят из входного, скрытых и выходного слоев.",
                "Обработка естественного языка позволяет компьютерам понимать текст.",
                "Компьютерное зрение использует сверточные нейронные сети для анализа изображений."
        };

        String[] programmingDocuments = {
                "Java использует виртуальную машину JVM для выполнения байт-кода.",
                "Python имеет динамическую типизацию и поддерживает несколько парадигм программирования.",
                "JavaScript является языком программирования для веб-разработки.",
                "SQL используется для управления реляционными базами данных."
        };

        String[] databaseDocuments = {
                "Реляционные базы данных используют таблицы и связи между ними.",
                "NoSQL базы данных включают документные, ключ-значение и графовые базы.",
                "Векторные базы данных оптимизированы для семантического поиска.",
                "Транзакции обеспечивают атомарность, согласованность, изолированность и долговечность."
        };

        // Загрузка AI документов
        for (int i = 0; i < aiDocuments.length; i++) {
            vectorDB.storeTextWithChunking(
                    aiDocuments[i],
                    "ai_doc_" + (i + 1),
                    new Object[]{"category", "ai", "doc_" + (i + 1)}
            );
        }

        // Загрузка programming документов
        for (int i = 0; i < programmingDocuments.length; i++) {
            vectorDB.storeTextWithChunking(
                    programmingDocuments[i],
                    "prog_doc_" + (i + 1),
                    new Object[]{"category", "programming", "doc_" + (i + 1)}
            );
        }

        // Загрузка database документов
        for (int i = 0; i < databaseDocuments.length; i++) {
            vectorDB.storeTextWithChunking(
                    databaseDocuments[i],
                    "db_doc_" + (i + 1),
                    new Object[]{"category", "databases", "doc_" + (i + 1)}
            );
        }

        System.out.println("Loaded advanced demo data: " +
                (aiDocuments.length + programmingDocuments.length + databaseDocuments.length) + " documents");
    }

    /**
     * Демонстрация сложных SQL запросов
     */
    private static void demonstrateComplexQueries(SQLParser sqlParser) {
        System.out.println("\n1. Complex SQL Queries");
        System.out.println("======================");

        try {
            // Комбинированные условия WHERE
            System.out.println("\n--- Комбинированный WHERE ---");
            List<JSONObject> results1 = sqlParser.execute(
                    "SELECT * FROM category WHERE text LIKE '%базы%' AND documentId LIKE '%db_%'"
            );
            printAdvancedResults(results1, "Комбинированный поиск");

            // Поиск по нескольким критериям
            System.out.println("\n--- Поиск по нескольким критериям ---");
            List<JSONObject> results2 = sqlParser.execute(
                    "SELECT id, text FROM category WHERE text LIKE '%нейрон%' OR text LIKE '%машин%' LIMIT 4"
            );
            printAdvancedResults(results2, "Нейронные сети и машинное обучение");

            // Сортировка по нескольким полям
            System.out.println("\n--- Сортировка по нескольким полям ---");
            List<JSONObject> results3 = sqlParser.execute(
                    "SELECT documentId, chunkIndex, text FROM category ORDER BY documentId ASC, chunkIndex DESC LIMIT 5"
            );
            printAdvancedResults(results3, "Сортировка по documentId и chunkIndex");

        } catch (Exception e) {
            System.out.println("Complex queries error: " + e.getMessage());
        }
    }

    /**
     * Демонстрация работы с индексами
     */
    private static void demonstrateIndexOperations(SQLParser sqlParser) {
        System.out.println("\n2. Index Operations");
        System.out.println("===================");

        try {
            // Создание индекса
            System.out.println("\n--- CREATE INDEX ---");
            List<JSONObject> createIndexResult = sqlParser.execute(
                    "CREATE INDEX idx_document_id ON category(documentId)"
            );
            System.out.println("Index creation result: " + createIndexResult);

            // Удаление индекса
            System.out.println("\n--- DROP INDEX ---");
            List<JSONObject> dropIndexResult = sqlParser.execute(
                    "DROP INDEX idx_document_id"
            );
            System.out.println("Index drop result: " + dropIndexResult);

            // Создание таблицы (для совместимости)
            System.out.println("\n--- CREATE TABLE ---");
            List<JSONObject> createTableResult = sqlParser.execute(
                    "CREATE TABLE test_table (id INT, name VARCHAR(100))"
            );
            System.out.println("Table creation result: " + createTableResult);

        } catch (Exception e) {
            System.out.println("Index operations error: " + e.getMessage());
        }
    }

    /**
     * Демонстрация комбинированного поиска
     */
    private static void demonstrateCombinedSearch(SQLParser sqlParser) {
        System.out.println("\n3. Combined Search Strategies");
        System.out.println("==============================");

        try {
            // Комбинация SQL и семантического поиска
            System.out.println("\n--- Комбинированная стратегия поиска ---");

            // Сначала семантический поиск
            System.out.println("Semantic search for 'программирование языки':");
            List<JSONObject> semanticResults = sqlParser.semanticSearch("программирование языки", 3);
            printAdvancedResults(semanticResults, "Семантический поиск");

            // Затем точный SQL поиск
            System.out.println("\nExact SQL search for 'JavaScript':");
            List<JSONObject> exactResults = sqlParser.execute(
                    "SELECT * FROM category WHERE text LIKE '%JavaScript%'"
            );
            printAdvancedResults(exactResults, "Точный поиск");

            // Гибридный поиск
            System.out.println("\nHybrid search for 'базы данных SQL':");
            List<JSONObject> hybridResults = sqlParser.hybridSearch("базы данных SQL", 4);
            printAdvancedResults(hybridResults, "Гибридный поиск");

        } catch (Exception e) {
            System.out.println("Combined search error: " + e.getMessage());
        }
    }

    /**
     * Вспомогательный метод для вывода результатов
     */
    private static void printAdvancedResults(List<JSONObject> results, String title) {
        System.out.println("\n" + title + " (" + results.size() + " results):");
        System.out.println("-".repeat(50));

        for (int i = 0; i < results.size(); i++) {
            JSONObject result = results.get(i);
            System.out.println((i + 1) + ". " + formatAdvancedResult(result));
        }
    }

    /**
     * Форматирование результата для продвинутого вывода
     */
    private static String formatAdvancedResult(JSONObject result) {
        StringBuilder sb = new StringBuilder();

        if (result.has("id")) {
            sb.append("[").append(result.getString("id")).append("] ");
        }

        if (result.has("documentId")) {
            sb.append("Doc: ").append(result.getString("documentId")).append(" | ");
        }

        if (result.has("chunkIndex") && result.getInt("chunkIndex") >= 0) {
            sb.append("Chunk: ").append(result.getInt("chunkIndex")).append(" | ");
        }

        if (result.has("text")) {
            String text = result.getString("text");
            String shortText = text.length() > 60 ? text.substring(0, 57) + "..." : text;
            sb.append(shortText);
        }

        if (result.has("similarity")) {
            sb.append(" | Similarity: ").append(String.format("%.3f", result.getDouble("similarity")));
        }

        return sb.toString();
    }
}