package ru.miacomsoft.vectordb.demo;

import ru.miacomsoft.vectordb.core.SemanticChunker;
import ru.miacomsoft.vectordb.core.VectorData;
import ru.miacomsoft.vectordb.core.VectorDatabase;
import ru.miacomsoft.vectordb.core.VectorSearchResult;

import java.util.List;

/**
 * Расширенный пример работы с Vector Database
 */
public class VectorDBExample {
    public static void main(String[] args) {
        try {
            // Инициализация SemanticChunker
            SemanticChunker semanticChunker = new SemanticChunker(
                    "http://localhost:11434",
                    "all-minilm:22m",
                    0.8
            );

            // Создание векторной базы данных
            VectorDatabase vectorDB = new VectorDatabase("./data/vectordb", semanticChunker);

            System.out.println("=== Extended Vector Database Example ===");

            // Пример текста для индексации
            String documentText = """
                Машинное обучение - это раздел искусственного интеллекта, 
                который позволяет компьютерам обучаться на данных без явного программирования. 
                Глубокое обучение использует нейронные сети с множеством слоев. 
                Обработка естественного языка позволяет компьютерам понимать человеческий язык.
                Векторные базы данных хранят данные в виде векторных эмбеддингов.
                Семантический поиск находит документы по смысловому сходству.
                """;

            // Сохранение текста с семантическим чанкингом
            vectorDB.storeTextWithChunking(
                    documentText,
                    "doc_001",
                    new Object[]{"documents", "ml", "introduction"}
            );

            // Дополнительные документы
            String additionalText = """
                Искусственный интеллект преобразует современные технологии.
                Нейронные сети имитируют работу человеческого мозга.
                Алгоритмы машинного обучения анализируют большие данные.
                """;

            vectorDB.storeTextWithChunking(
                    additionalText,
                    "doc_002",
                    new Object[]{"documents", "ai", "advanced"}
            );

            // Поиск по схожести
            System.out.println("\n=== Semantic Search ===");
            List<VectorSearchResult> similarResults = vectorDB.similaritySearch(
                    "искусственный интеллект и обучение", 5
            );

            for (VectorSearchResult result : similarResults) {
                VectorData vectorData = result.getVectorData();
                System.out.printf("Схожесть: %.4f - %s%n",
                        result.getSimilarity(),
                        vectorData.getText());
            }

            // Точный поиск
            System.out.println("\n=== Exact Search ===");
            List<VectorData> exactResults = vectorDB.exactSearch("нейронные сети");
            for (VectorData data : exactResults) {
                System.out.println("Найдено: " + data.getText());
            }

            // Поиск по пути
            System.out.println("\n=== Path Search ===");
            List<VectorData> pathResults = vectorDB.searchByPath("documents");
            for (VectorData data : pathResults) {
                System.out.println("Путь: " + data.getNodePath() + " - " + data.getText());
            }

            // Расширенная статистика
            System.out.println("\n=== Extended Statistics ===");
            System.out.println("Всего векторов: " + vectorDB.getVectorCount());
            System.out.println("Всего узлов: " + vectorDB.getTreeNodeCount());

            // Информация о чанках
            List<VectorData> allVectors = vectorDB.exactSearch("");
            System.out.println("Всего чанков: " + allVectors.size());

            for (VectorData vector : allVectors.subList(0, Math.min(3, allVectors.size()))) {
                System.out.println("Чанк: " + vector.getText().substring(0, Math.min(50, vector.getText().length())) + "...");
                System.out.println("  Документ: " + vector.getDocumentId());
                System.out.println("  Индекс: " + vector.getChunkIndex());
                System.out.println("  Путь: " + vector.getNodePath());
            }

            // Тестирование различных запросов
            System.out.println("\n=== Query Testing ===");
            String[] testQueries = {
                    "машинное обучение",
                    "глубокое обучение",
                    "векторные базы данных",
                    "семантический поиск"
            };

            for (String query : testQueries) {
                List<VectorSearchResult> results = vectorDB.similaritySearch(query, 2);
                System.out.println("Запрос: '" + query + "' - найдено: " + results.size() + " результатов");
            }

            // Сохранение и закрытие
            vectorDB.saveDatabase();
            vectorDB.close();

            System.out.println("\n=== Example completed successfully ===");

        } catch (Exception e) {
            System.err.println("Error in example: " + e.getMessage());
            e.printStackTrace();
        }
    }
}