package ru.miacomsoft.vectordb.core;

public class VectorSearchResult {
    private VectorData vectorData;
    private double similarity;
    private double distance;

    public VectorSearchResult(VectorData vectorData, double similarity, double distance) {
        this.vectorData = vectorData;
        this.similarity = similarity;
        this.distance = distance;
    }

    // Getters and setters
    public VectorData getVectorData() { return vectorData; }
    public void setVectorData(VectorData vectorData) { this.vectorData = vectorData; }

    public double getSimilarity() { return similarity; }
    public void setSimilarity(double similarity) { this.similarity = similarity; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    @Override
    public String toString() {
        return String.format("VectorSearchResult{similarity=%.4f, distance=%.4f, data=%s}",
                similarity, distance, vectorData);
    }
}