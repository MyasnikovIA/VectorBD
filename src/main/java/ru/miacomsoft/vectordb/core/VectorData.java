package ru.miacomsoft.vectordb.core;

import org.json.JSONArray;
import org.json.JSONObject;

public class VectorData {
    private String id;
    private float[] vector;
    private Object originalData;
    private String text;
    private String nodePath;
    private String documentId;
    private int chunkIndex;
    private long timestamp;

    public VectorData() {
        this.timestamp = System.currentTimeMillis();
    }

    public VectorData(String id, float[] vector, Object originalData, String text,
                      String nodePath, String documentId, int chunkIndex) {
        this();
        this.id = id;
        this.vector = vector;
        this.originalData = originalData;
        this.text = text;
        this.nodePath = nodePath;
        this.documentId = documentId;
        this.chunkIndex = chunkIndex;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public float[] getVector() { return vector; }
    public void setVector(float[] vector) { this.vector = vector; }

    public Object getOriginalData() { return originalData; }
    public void setOriginalData(Object originalData) { this.originalData = originalData; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getNodePath() { return nodePath; }
    public void setNodePath(String nodePath) { this.nodePath = nodePath; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("text", text);
        json.put("nodePath", nodePath);
        json.put("documentId", documentId);
        json.put("chunkIndex", chunkIndex);
        json.put("timestamp", timestamp);

        // Сохранение вектора как массив
        JSONArray vectorArray = new JSONArray();
        if (vector != null) {
            for (float v : vector) {
                vectorArray.put(v);
            }
        }
        json.put("vector", vectorArray);

        // Сохранение originalData как строки
        if (originalData != null) {
            json.put("originalData", originalData.toString());
        }

        return json;
    }

    public static VectorData fromJson(JSONObject json) {
        VectorData vectorData = new VectorData();
        vectorData.setId(json.getString("id"));
        vectorData.setText(json.getString("text"));
        vectorData.setNodePath(json.getString("nodePath"));
        vectorData.setDocumentId(json.getString("documentId"));
        vectorData.setChunkIndex(json.getInt("chunkIndex"));
        vectorData.setTimestamp(json.getLong("timestamp"));

        // Загрузка вектора
        JSONArray vectorArray = json.getJSONArray("vector");
        float[] vector = new float[vectorArray.length()];
        for (int i = 0; i < vectorArray.length(); i++) {
            vector[i] = (float) vectorArray.getDouble(i);
        }
        vectorData.setVector(vector);

        // Загрузка originalData
        if (json.has("originalData")) {
            vectorData.setOriginalData(json.getString("originalData"));
        }

        return vectorData;
    }

    @Override
    public String toString() {
        return "VectorData{" +
                "id='" + id + '\'' +
                ", text='" + (text != null ? text.substring(0, Math.min(50, text.length())) + "..." : "null") + '\'' +
                ", nodePath='" + nodePath + '\'' +
                ", documentId='" + documentId + '\'' +
                ", chunkIndex=" + chunkIndex +
                '}';
    }
}