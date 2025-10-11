package ru.miacomsoft.vectordb.demo;

import ru.miacomsoft.vectordb.core.BinaryVectorDatabase;
import ru.miacomsoft.vectordb.core.SemanticChunker;
import ru.miacomsoft.vectordb.core.VectorSearchResult;


import java.util.List;

/**
 * Демонстрация работы с Embedding функциональностью в бинарной БД
 */
public class EmbeddingDemo {
    public static void main(String[] args) {
        // Инициализация SemanticChunker и бинарной VectorDatabase
        SemanticChunker semanticChunker = new SemanticChunker(
                "http://localhost:11434",
                "all-minilm:22m",
                0.8
        );
        BinaryVectorDatabase vectorDB = new BinaryVectorDatabase("./data/binary_embedding_demo", semanticChunker);

        System.out.println("=== Binary Vector Database with Embedding Demo ===");

        try {
            // Сохраняем тексты с автоматическим embedding в бинарную БД
            System.out.println("\n1. Storing texts with auto-embedding to binary database:");

            String[] texts = {
                    "Машинное обучение - это область искусственного интеллекта",
                    "Глубокое обучение использует нейронные сети с многими слоями",
                    "Базы данных хранят структурированную информацию",
                    "Векторные базы данных используются для семантического поиска",
                    "Искусственный интеллект преобразует современные технологии"
            };

            for (int i = 0; i < texts.length; i++) {
                vectorDB.storeTextWithChunking(
                        texts[i],
                        "article_" + (i + 1),
                        new Object[]{"articles", "text_" + (i + 1)}
                );
                System.out.println("Stored to binary DB: " + texts[i].substring(0, Math.min(30, texts[i].length())) + "...");
            }

            // Получаем embedding для текста
            System.out.println("\n2. Getting embedding for text:");
            try {
                String testText = "Машинное обучение и искусственный интеллект";
                float[] embedding = semanticChunker.getEmbedding(testText);
                System.out.println("Embedding dimension: " + embedding.length);
                System.out.println("First 5 values: ");
                for (int i = 0; i < Math.min(5, embedding.length); i++) {
                    System.out.printf("%.4f ", embedding[i]);
                }
                System.out.println();
            } catch (Exception e) {
                System.out.println("Error getting embedding: " + e.getMessage());
            }

            // Поиск похожих текстов в бинарной БД
            System.out.println("\n3. Binary similarity search:");
            try {
                String query = "нейронные сети и машинное обучение";
                List<VectorSearchResult> results = vectorDB.similaritySearch(query, 3);

                System.out.println("Query: " + query);
                System.out.println("Top 3 similar texts from binary DB:");
                for (VectorSearchResult result : results) {
                    System.out.printf("Similarity: %.4f | Text: %s\n",
                            result.getSimilarity(),
                            result.getVectorData().getText().substring(0, Math.min(50, result.getVectorData().getText().length())) + "...");
                }
            } catch (Exception e) {
                System.out.println("Error in binary similarity search: " + e.getMessage());
            }

            // Точный поиск в бинарной БД
            System.out.println("\n4. Binary exact search:");
            try {
                String searchText = "базы данных";
                List<ru.miacomsoft.vectordb.core.BinaryVectorData> exactResults = vectorDB.exactSearch(searchText);

                System.out.println("Query: " + searchText);
                System.out.println("Exact matches from binary DB:");
                for (var data : exactResults) {
                    System.out.printf("Found: %s\n",
                            data.getText().substring(0, Math.min(50, data.getText().length())) + "...");
                }
            } catch (Exception e) {
                System.out.println("Error in binary exact search: " + e.getMessage());
            }

            // Статистика бинарной БД
            System.out.println("\n5. Binary database statistics:");
            System.out.println("Total vectors: " + vectorDB.getVectorCount());
            System.out.println("Database type: Binary serialization");
            System.out.println("Storage file: binary_vectordb.dat");

            // Сохранение бинарной БД
            System.out.println("\n6. Saving binary database...");
            vectorDB.saveDatabase();

        } catch (Exception e) {
            System.out.println("Binary demo error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            vectorDB.close();
        }

        System.out.println("\n=== Binary Demo completed ===");
    }
}