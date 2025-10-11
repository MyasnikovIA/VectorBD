package ru.miacomsoft.vectordb.demo;

import ru.miacomsoft.vectordb.core.*;

import java.util.List;

/**
 * Демонстрация работы с Embedding функциональностью и BinaryVectorDatabase
 */
public class BinaryEmbeddingDemo {
    public static void main(String[] args) {
        // Инициализация SemanticChunker и BinaryVectorDatabase
        SemanticChunker semanticChunker = new SemanticChunker(
                "http://localhost:11434",
                "all-minilm:22m",
                0.8
        );
        BinaryVectorDatabase vectorDB = new BinaryVectorDatabase("./data/binary_embedding_demo", semanticChunker);

        System.out.println("=== Binary Vector Database with Embedding Demo ===");

        try {
            // Сохраняем тексты с автоматическим embedding
            System.out.println("\n1. Storing texts with auto-embedding:");

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
                System.out.println("Stored: " + texts[i].substring(0, Math.min(30, texts[i].length())) + "...");
            }

            // Поиск похожих текстов
            System.out.println("\n2. Similarity search:");
            try {
                String query = "нейронные сети и машинное обучение";
                List<VectorSearchResult> results = vectorDB.similaritySearch(query, 3);

                System.out.println("Query: " + query);
                System.out.println("Top 3 similar texts:");
                for (VectorSearchResult result : results) {
                    BinaryVectorData vectorData = result.getVectorData();
                    System.out.printf("Similarity: %.4f | Text: %s\n",
                            result.getSimilarity(),
                            vectorData.getText().substring(0, Math.min(50, vectorData.getText().length())) + "...");
                }
            } catch (Exception e) {
                System.out.println("Error in similarity search: " + e.getMessage());
            }

            // Статистика
            System.out.println("\n3. Database statistics:");
            System.out.println("Total vectors: " + vectorDB.getVectorCount());
            System.out.println("Total tree nodes: " + vectorDB.getTreeNodeCount());

        } catch (Exception e) {
            System.out.println("Demo error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            vectorDB.close();
        }

        System.out.println("\n=== Demo completed ===");
    }
}