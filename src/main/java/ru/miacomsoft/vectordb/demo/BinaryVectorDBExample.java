package ru.miacomsoft.vectordb.demo;

import ru.miacomsoft.vectordb.core.*;

import java.util.List;

/**
 * Пример работы с BinaryVectorDatabase
 */
public class BinaryVectorDBExample {
    public static void main(String[] args) {
        try {
            // Инициализация SemanticChunker
            SemanticChunker semanticChunker = new SemanticChunker(
                    "http://localhost:11434",
                    "all-minilm:22m",
                    0.8
            );

            // Создание бинарной векторной базы данных
            BinaryVectorDatabase vectorDB = new BinaryVectorDatabase("./data/binary_vectordb", semanticChunker);

            System.out.println("=== Binary Vector Database Example ===");

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

            // Поиск по схожести
            System.out.println("\n=== Semantic Search ===");
            List<VectorSearchResult> similarResults = vectorDB.similaritySearch(
                    "искусственный интеллект и обучение", 5
            );

            for (VectorSearchResult result : similarResults) {
                BinaryVectorData vectorData = result.getVectorData();
                System.out.printf("Схожесть: %.4f - %s%n",
                        result.getSimilarity(),
                        vectorData.getText());
            }

            // Точный поиск
            System.out.println("\n=== Exact Search ===");
            List<BinaryVectorData> exactResults = vectorDB.exactSearch("нейронные сети");
            for (BinaryVectorData data : exactResults) {
                System.out.println("Найдено: " + data.getText());
            }

            // Статистика
            System.out.println("\n=== Statistics ===");
            System.out.println("Всего векторов: " + vectorDB.getVectorCount());
            System.out.println("Всего узлов: " + vectorDB.getTreeNodeCount());

            // Сохранение и закрытие
            vectorDB.close();

            System.out.println("\n=== Example completed successfully ===");

        } catch (Exception e) {
            System.err.println("Error in example: " + e.getMessage());
            e.printStackTrace();
        }
    }
}