package ru.miacomsoft.vectordb.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VectorIndex {
    private final Map<String, float[]> vectors;
    private final Map<String, float[]> normalizedVectors;

    public VectorIndex() {
        this.vectors = new ConcurrentHashMap<>();
        this.normalizedVectors = new ConcurrentHashMap<>();
    }

    public void addVector(String vectorId, float[] vector) {
        vectors.put(vectorId, vector);
        normalizedVectors.put(vectorId, normalizeVector(vector));
    }

    public void removeVector(String vectorId) {
        vectors.remove(vectorId);
        normalizedVectors.remove(vectorId);
    }

    public List<SearchResult> search(float[] queryVector, int limit) {
        return search(queryVector, limit, "cosine");
    }

    public List<SearchResult> search(float[] queryVector, int limit, String metric) {
        float[] normalizedQuery = normalizeVector(queryVector);
        PriorityQueue<SearchResult> results = new PriorityQueue<>(
                limit, (a, b) -> Float.compare(b.similarity, a.similarity)
        );

        for (Map.Entry<String, float[]> entry : normalizedVectors.entrySet()) {
            String vectorId = entry.getKey();
            float[] vector = entry.getValue();

            float similarity = calculateSimilarity(normalizedQuery, vector, metric);
            float distance = calculateDistance(queryVector, vectors.get(vectorId), metric);

            results.offer(new SearchResult(vectorId, similarity, distance));

            if (results.size() > limit) {
                results.poll();
            }
        }

        List<SearchResult> sortedResults = new ArrayList<>(results);
        sortedResults.sort((a, b) -> Float.compare(b.similarity, a.similarity));
        return sortedResults;
    }

    private float calculateSimilarity(float[] v1, float[] v2, String metric) {
        switch (metric.toLowerCase()) {
            case "cosine":
                return cosineSimilarity(v1, v2);
            case "dot":
                return dotProduct(v1, v2);
            default:
                return cosineSimilarity(v1, v2);
        }
    }

    private float calculateDistance(float[] v1, float[] v2, String metric) {
        switch (metric.toLowerCase()) {
            case "cosine":
                return 1 - cosineSimilarity(v1, v2);
            case "euclidean":
                return euclideanDistance(v1, v2);
            default:
                return 1 - cosineSimilarity(v1, v2);
        }
    }

    private float cosineSimilarity(float[] v1, float[] v2) {
        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;

        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0f;
        }

        return (float) (dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2)));
    }

    private float dotProduct(float[] v1, float[] v2) {
        float result = 0.0f;
        for (int i = 0; i < v1.length; i++) {
            result += v1[i] * v2[i];
        }
        return result;
    }

    private float euclideanDistance(float[] v1, float[] v2) {
        float sum = 0.0f;
        for (int i = 0; i < v1.length; i++) {
            float diff = v1[i] - v2[i];
            sum += diff * diff;
        }
        return (float) Math.sqrt(sum);
    }

    private float[] normalizeVector(float[] vector) {
        float norm = 0.0f;
        for (float value : vector) {
            norm += value * value;
        }

        if (norm == 0.0f) {
            return vector.clone();
        }

        norm = (float) Math.sqrt(norm);
        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / norm;
        }

        return normalized;
    }

    public int size() {
        return vectors.size();
    }

    public boolean contains(String vectorId) {
        return vectors.containsKey(vectorId);
    }

    public float[] getVector(String vectorId) {
        return vectors.get(vectorId);
    }

    public Set<String> getVectorIds() {
        return vectors.keySet();
    }

    public void clear() {
        vectors.clear();
        normalizedVectors.clear();
    }

    // Внутренний класс для результатов поиска
    public static class SearchResult {
        private final String vectorId;
        private final float similarity;
        private final float distance;

        public SearchResult(String vectorId, float similarity, float distance) {
            this.vectorId = vectorId;
            this.similarity = similarity;
            this.distance = distance;
        }

        public String getVectorId() {
            return vectorId;
        }

        public float getSimilarity() {
            return similarity;
        }

        public float getDistance() {
            return distance;
        }

        @Override
        public String toString() {
            return String.format("SearchResult{id=%s, similarity=%.4f, distance=%.4f}",
                    vectorId, similarity, distance);
        }
    }
}