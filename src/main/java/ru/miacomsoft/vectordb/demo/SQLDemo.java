package ru.miacomsoft.vectordb.demo;

import ru.miacomsoft.vectordb.core.*;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.List;

/**
 * Демонстрация использования SQL запросов в VectorBD
 */
public class SQLDemo {
    public static void main(String[] args) {
        System.out.println("=== VectorBD SQL Demo ===");

        try {
            // Инициализация SemanticChunker и VectorDatabase
            SemanticChunker semanticChunker = new SemanticChunker(
                    "http://localhost:11434",
                    "all-minilm:22m",
                    0.8
            );
            BinaryVectorDatabase vectorDB = new BinaryVectorDatabase("./data/sql_demo", semanticChunker);
            SQLParser sqlParser = new SQLParser(vectorDB);

            // Загрузка демонстрационных данных
            loadDemoData(vectorDB);

            // Демонстрация SQL запросов
            demonstrateSQLQueries(sqlParser);

            // Демонстрация семантического поиска через SQL
            demonstrateSemanticSearch(sqlParser);

            // Демонстрация гибридного поиска
            demonstrateHybridSearch(sqlParser);

            // Демонстрация операций INSERT, UPDATE, DELETE
            demonstrateCRUDOperations(sqlParser);

            vectorDB.close();

        } catch (Exception e) {
            System.out.println("Demo error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Загрузка демонстрационных данных
     */
    private static void loadDemoData(BinaryVectorDatabase vectorDB) throws Exception {
        System.out.println("\n1. Loading demo data...");

        // Документы о технологиях
        String[] documents = {
                "Машинное обучение - это область искусственного интеллекта, которая позволяет компьютерам обучаться на данных.",
                "Глубокое обучение использует нейронные сети с множеством слоев для изучения сложных паттернов.",
                "Python является популярным языком программирования для анализа данных и машинного обучения.",
                "Java - это объектно-ориентированный язык программирования с сильной типизацией.",
                "Базы данных хранят структурированную информацию и обеспечивают эффективный доступ к данным.",
                "Векторные базы данных используются для семантического поиска и хранения эмбеддингов.",
                "SQL - это язык структурированных запросов для работы с реляционными базами данных.",
                "Искусственный интеллект преобразует современные технологии и автоматизирует процессы.",
                "Трансформеры - это архитектура нейронных сетей для обработки последовательностей.",
                "Облачные вычисления предоставляют ресурсы как услугу через интернет."
        };

        for (int i = 0; i < documents.length; i++) {
            vectorDB.storeTextWithChunking(
                    documents[i],
                    "doc_" + (i + 1),
                    new Object[]{"documents", "tech", "doc_" + (i + 1)}
            );
        }

        System.out.println("Loaded " + documents.length + " demo documents");
    }

    /**
     * Демонстрация SQL SELECT запросов
     */
    private static void demonstrateSQLQueries(SQLParser sqlParser) {
        System.out.println("\n2. SQL SELECT Queries Demo");
        System.out.println("===========================");

        try {
            // Простой SELECT всех данных
            System.out.println("\n--- SELECT * ---");
            List<JSONObject> results1 = sqlParser.execute("SELECT * FROM documents");
            System.out.println("Found " + results1.size() + " records");
            printResults(results1, 2);

            // SELECT с LIMIT
            System.out.println("\n--- SELECT * LIMIT 3 ---");
            List<JSONObject> results2 = sqlParser.execute("SELECT * FROM documents LIMIT 3");
            printResults(results2, 3);

            // SELECT конкретных полей
            System.out.println("\n--- SELECT id, text FROM documents ---");
            List<JSONObject> results3 = sqlParser.execute("SELECT id, text FROM documents LIMIT 2");
            printResults(results3, 2);

            // SELECT с WHERE условием (точное совпадение)
            System.out.println("\n--- SELECT * WHERE text LIKE '%машинное%' ---");
            List<JSONObject> results4 = sqlParser.execute("SELECT * FROM documents WHERE text LIKE '%машинное%'");
            printResults(results4, results4.size());

            // SELECT с WHERE условием (по documentId)
            System.out.println("\n--- SELECT * WHERE documentId = 'doc_1' ---");
            List<JSONObject> results5 = sqlParser.execute("SELECT * FROM documents WHERE documentId = 'doc_1'");
            printResults(results5, results5.size());

            // SELECT с ORDER BY
            System.out.println("\n--- SELECT id, documentId ORDER BY documentId DESC LIMIT 4 ---");
            List<JSONObject> results6 = sqlParser.execute(
                    "SELECT id, documentId FROM documents ORDER BY documentId DESC LIMIT 4"
            );
            printResults(results6, 4);

        } catch (Exception e) {
            System.out.println("SQL query error: " + e.getMessage());
        }
    }

    /**
     * Демонстрация семантического поиска
     */
    private static void demonstrateSemanticSearch(SQLParser sqlParser) {
        System.out.println("\n3. Semantic Search Demo");
        System.out.println("========================");

        try {
            // Семантический поиск через специальный метод
            System.out.println("\n--- Semantic Search: 'нейронные сети' ---");
            List<JSONObject> semanticResults = sqlParser.semanticSearch("нейронные сети", 3);
            printResults(semanticResults, semanticResults.size());

            System.out.println("\n--- Semantic Search: 'программирование' ---");
            List<JSONObject> semanticResults2 = sqlParser.semanticSearch("программирование", 2);
            printResults(semanticResults2, semanticResults2.size());

        } catch (Exception e) {
            System.out.println("Semantic search error: " + e.getMessage());
        }
    }

    /**
     * Демонстрация гибридного поиска
     */
    private static void demonstrateHybridSearch(SQLParser sqlParser) {
        System.out.println("\n4. Hybrid Search Demo");
        System.out.println("======================");

        try {
            // Гибридный поиск (семантический + точный)
            System.out.println("\n--- Hybrid Search: 'базы данных' ---");
            List<JSONObject> hybridResults = sqlParser.hybridSearch("базы данных", 4);
            printResults(hybridResults, hybridResults.size());

            System.out.println("\n--- Hybrid Search: 'искусственный интеллект' ---");
            List<JSONObject> hybridResults2 = sqlParser.hybridSearch("искусственный интеллект", 3);
            printResults(hybridResults2, hybridResults2.size());

        } catch (Exception e) {
            System.out.println("Hybrid search error: " + e.getMessage());
        }
    }

    /**
     * Демонстрация операций INSERT, UPDATE, DELETE
     */
    private static void demonstrateCRUDOperations(SQLParser sqlParser) {
        System.out.println("\n5. CRUD Operations Demo");
        System.out.println("========================");

        try {
            // INSERT операция
            System.out.println("\n--- INSERT ---");
            List<JSONObject> insertResult = sqlParser.execute(
                    "INSERT INTO users (name, email, role) VALUES ('Иван Петров', 'ivan@example.com', 'admin')"
            );
            System.out.println("INSERT result: " + insertResult);

            // Еще один INSERT
            List<JSONObject> insertResult2 = sqlParser.execute(
                    "INSERT INTO products (name, category, price) VALUES ('Ноутбук', 'Электроника', 50000)"
            );
            System.out.println("INSERT result: " + insertResult2);

            // SELECT для проверки вставленных данных
            System.out.println("\n--- SELECT после INSERT ---");
            List<JSONObject> afterInsert = sqlParser.execute("SELECT * FROM users");
            printResults(afterInsert, afterInsert.size());

            // UPDATE операция
            System.out.println("\n--- UPDATE ---");
            List<JSONObject> updateResult = sqlParser.execute(
                    "UPDATE users SET role = 'superadmin' WHERE name = 'Иван Петров'"
            );
            System.out.println("UPDATE affected " + updateResult.size() + " records");

            // DELETE операция
            System.out.println("\n--- DELETE ---");
            List<JSONObject> deleteResult = sqlParser.execute(
                    "DELETE FROM products WHERE name = 'Ноутбук'"
            );
            System.out.println("DELETE affected " + deleteResult.size() + " records");

            // Финальный SELECT
            System.out.println("\n--- Финальный SELECT ---");
            List<JSONObject> finalResults = sqlParser.execute("SELECT * FROM documents LIMIT 5");
            printResults(finalResults, 3);

        } catch (Exception e) {
            System.out.println("CRUD operations error: " + e.getMessage());
        }
    }

    /**
     * Вспомогательный метод для вывода результатов
     */
    private static void printResults(List<JSONObject> results, int maxToShow) {
        if (results.isEmpty()) {
            System.out.println("No results found");
            return;
        }

        int count = Math.min(results.size(), maxToShow);
        System.out.println("Showing " + count + " of " + results.size() + " results:");

        for (int i = 0; i < count; i++) {
            JSONObject result = results.get(i);
            System.out.println((i + 1) + ". " + formatResult(result));
        }
    }

    /**
     * Форматирование результата для вывода
     */
    private static String formatResult(JSONObject result) {
        StringBuilder sb = new StringBuilder();

        if (result.has("id")) {
            sb.append("ID: ").append(result.getString("id")).append(", ");
        }

        if (result.has("documentId")) {
            sb.append("Doc: ").append(result.getString("documentId")).append(", ");
        }

        if (result.has("text")) {
            String text = result.getString("text");
            String shortText = text.length() > 50 ? text.substring(0, 47) + "..." : text;
            sb.append("Text: ").append(shortText);
        } else if (result.has("content")) {
            String content = result.getString("content");
            String shortContent = content.length() > 50 ? content.substring(0, 47) + "..." : content;
            sb.append("Content: ").append(shortContent);
        }

        if (result.has("chunkIndex")) {
            sb.append(", Chunk: ").append(result.getInt("chunkIndex"));
        }

        return sb.toString();
    }
}