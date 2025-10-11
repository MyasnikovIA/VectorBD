package ru.miacomsoft.vectordb.core;

public class VectorSearchResult {
    private BinaryVectorData vectorData;
    private double similarity;
    private double distance;

    public VectorSearchResult(BinaryVectorData vectorData, double similarity, double distance) {
        this.vectorData = vectorData;
        this.similarity = similarity;
        this.distance = distance;
    }

    // Getters and setters
    public BinaryVectorData getVectorData() { return vectorData; }
    public void setVectorData(BinaryVectorData vectorData) { this.vectorData = vectorData; }

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