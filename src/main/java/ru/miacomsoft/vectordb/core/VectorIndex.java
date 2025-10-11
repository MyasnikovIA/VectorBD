package ru.miacomsoft.vectordb.core;

import java.util.*;

public class VectorIndex {
    private final Map<String, float[]> vectors;
    private final int maxSearchCandidates;

    public VectorIndex() {
        this.vectors = new HashMap<>();
        this.maxSearchCandidates = 1000;
    }

    public void addVector(String vectorId, float[] vector) {
        vectors.put(vectorId, vector);
    }

    public void removeVector(String vectorId) {
        vectors.remove(vectorId);
    }

    public List<SearchResult> search(float[] queryVector, int limit) {
        List<SearchResult> results = new ArrayList<>();

        for (Map.Entry<String, float[]> entry : vectors.entrySet()) {
            double similarity = cosineSimilarity(queryVector, entry.getValue());
            double distance = 1 - similarity; // Косинусное расстояние

            results.add(new SearchResult(entry.getKey(), similarity, distance));
        }

        // Сортировка по схожести (по убыванию)
        results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));

        // Ограничение количества результатов
        if (results.size() > limit) {
            return results.subList(0, limit);
        }

        return results;
    }

    private double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public int size() {
        return vectors.size();
    }

    public void clear() {
        vectors.clear();
    }

    public static class SearchResult {
        private final String vectorId;
        private final double similarity;
        private final double distance;

        public SearchResult(String vectorId, double similarity, double distance) {
            this.vectorId = vectorId;
            this.similarity = similarity;
            this.distance = distance;
        }

        public String getVectorId() { return vectorId; }
        public double getSimilarity() { return similarity; }
        public double getDistance() { return distance; }
    }
}