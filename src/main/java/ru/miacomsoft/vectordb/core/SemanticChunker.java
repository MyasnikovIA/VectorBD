package ru.miacomsoft.vectordb.core;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class SemanticChunker {

    private final HttpClient httpClient;
    private final String ollamaBaseUrl;
    private final String embeddingModel;
    private double similarityThreshold;

    public SemanticChunker(String ollamaBaseUrl, String embeddingModel, double similarityThreshold) {
        this.httpClient = HttpClient.newHttpClient();
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.embeddingModel = embeddingModel;
        this.similarityThreshold = similarityThreshold;
    }

    /**
     * Установить порог схожести для семантического чанкинга
     * @param threshold порог схожести (0.0 - 1.0)
     */
    public void setSimilarityThreshold(double threshold) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Similarity threshold must be between 0.0 and 1.0");
        }
        this.similarityThreshold = threshold;
    }

    public List<Chunk> semanticChunking(String text, int maxChunkSize) throws Exception {
        List<String> sentences = splitIntoSentences(text);

        if (sentences.isEmpty()) {
            return new ArrayList<>();
        }

        List<float[]> embeddings = getEmbeddingsForSentences(sentences);
        return groupSentencesSemantically(sentences, embeddings, maxChunkSize);
    }

    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return sentences;
        }

        String[] rawSentences = text.split("(?<=[.!?])\\s+");

        for (String sentence : rawSentences) {
            sentence = sentence.trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }
        return sentences;
    }

    private List<float[]> getEmbeddingsForSentences(List<String> sentences) throws Exception {
        List<float[]> embeddings = new ArrayList<>();

        for (String sentence : sentences) {
            float[] embedding = getEmbedding(sentence);
            embeddings.add(embedding);
            Thread.sleep(100);
        }

        return embeddings;
    }

    public float[] getEmbedding(String text) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", embeddingModel);
            requestBody.put("prompt", text);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaBaseUrl + "/api/embeddings"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Ошибка при получении эмбеддинга: " +
                        response.statusCode() + " - " + response.body());
            }

            JSONObject responseJson = new JSONObject(response.body());
            if (!responseJson.has("embedding")) {
                throw new RuntimeException("Invalid response format: missing embedding field");
            }

            JSONArray embeddingArray = responseJson.getJSONArray("embedding");

            if (embeddingArray.length() == 0) {
                throw new RuntimeException("Empty embedding received");
            }

            float[] embedding = new float[embeddingArray.length()];
            for (int i = 0; i < embeddingArray.length(); i++) {
                embedding[i] = (float) embeddingArray.getDouble(i);
            }

            return embedding;

        } catch (java.net.ConnectException e) {
            throw new RuntimeException("Cannot connect to embedding service at: " + ollamaBaseUrl, e);
        } catch (java.net.http.HttpTimeoutException e) {
            throw new RuntimeException("Embedding service timeout", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get embedding: " + e.getMessage(), e);
        }
    }

    public double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Векторы должны иметь одинаковую размерность");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private List<Chunk> groupSentencesSemantically(List<String> sentences,
                                                   List<float[]> embeddings,
                                                   int maxChunkSize) {
        List<Chunk> chunks = new ArrayList<>();

        if (sentences.isEmpty()) {
            return chunks;
        }

        List<String> currentChunkSentences = new ArrayList<>();
        currentChunkSentences.add(sentences.get(0));
        float[] lastEmbedding = embeddings.get(0);

        for (int i = 1; i < sentences.size(); i++) {
            String currentSentence = sentences.get(i);
            float[] currentEmbedding = embeddings.get(i);

            double similarity = cosineSimilarity(lastEmbedding, currentEmbedding);

            boolean shouldStartNewChunk = similarity < similarityThreshold ||
                    getTotalLength(currentChunkSentences) + currentSentence.length() > maxChunkSize;

            if (shouldStartNewChunk) {
                String chunkText = String.join(" ", currentChunkSentences);
                float[] chunkEmbedding = calculateAverageEmbedding(
                        embeddings,
                        i - currentChunkSentences.size(),
                        currentChunkSentences.size()
                );
                chunks.add(new Chunk(chunkText, chunkEmbedding, i - currentChunkSentences.size()));

                currentChunkSentences = new ArrayList<>();
            }

            currentChunkSentences.add(currentSentence);
            lastEmbedding = currentEmbedding;
        }

        if (!currentChunkSentences.isEmpty()) {
            String chunkText = String.join(" ", currentChunkSentences);
            int startIndex = sentences.size() - currentChunkSentences.size();
            float[] chunkEmbedding = calculateAverageEmbedding(
                    embeddings,
                    startIndex,
                    currentChunkSentences.size()
            );
            chunks.add(new Chunk(chunkText, chunkEmbedding, startIndex));
        }

        return chunks;
    }

    private float[] calculateAverageEmbedding(List<float[]> allEmbeddings,
                                              int startIndex, int count) {
        if (count == 0) return new float[0];

        int dimensions = allEmbeddings.get(0).length;
        float[] average = new float[dimensions];

        for (int i = startIndex; i < startIndex + count; i++) {
            float[] embedding = allEmbeddings.get(i);
            for (int j = 0; j < dimensions; j++) {
                average[j] += embedding[j];
            }
        }

        for (int j = 0; j < dimensions; j++) {
            average[j] /= count;
        }

        return average;
    }

    private int getTotalLength(List<String> sentences) {
        return sentences.stream().mapToInt(String::length).sum() + sentences.size() - 1;
    }

    /**
     * Получить порог схожести для семантического чанкинга
     */
    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    /**
     * Получить информацию о конфигурации SemanticChunker
     */
    public String getConfigInfo() {
        return String.format(
                "SemanticChunker Config: model=%s, similarityThreshold=%.2f, url=%s",
                embeddingModel, similarityThreshold, ollamaBaseUrl
        );
    }

    /**
     * Получить используемую модель для эмбеддингов
     */
    public String getEmbeddingModel() {
        return embeddingModel;
    }

    /**
     * Получить базовый URL Ollama
     */
    public String getOllamaBaseUrl() {
        return ollamaBaseUrl;
    }

    public static class Chunk {
        private final String text;
        private final float[] embedding;
        private final int position;

        public Chunk(String text, float[] embedding, int position) {
            this.text = text;
            this.embedding = embedding;
            this.position = position;
        }

        public String getText() { return text; }
        public float[] getEmbedding() { return embedding; }
        public int getPosition() { return position; }
        public int getLength() { return text.length(); }

        @Override
        public String toString() {
            return String.format("Chunk[position=%d, length=%d characters]\n%s\n",
                    position, text.length(), text);
        }

        public String getFullInfo() {
            return String.format("=== CHUNK INFO ===\nPosition: %d\nLength: %d characters\nText:\n%s\n",
                    position, text.length(), text);
        }
    }
}