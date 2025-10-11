package ru.miacomsoft.vectordb.core;

import org.json.JSONObject;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VectorDatabase {
    private final String databasePath;
    private final Map<String, VectorData> vectors;
    private final Map<String, TreeNode> treeNodes;
    private final SemanticChunker semanticChunker;
    private final VectorIndex vectorIndex;

    public VectorDatabase(String databasePath, SemanticChunker semanticChunker) {
        this.databasePath = databasePath;
        this.semanticChunker = semanticChunker;
        this.vectors = new ConcurrentHashMap<>();
        this.treeNodes = new ConcurrentHashMap<>();
        this.vectorIndex = new VectorIndex();

        loadDatabase();
    }

    public void storeTextWithChunking(String text, String documentId, Object[] path) throws Exception {
        // Семантическое разбиение текста на чанки
        List<SemanticChunker.Chunk> chunks = semanticChunker.semanticChunking(text, 1000);

        for (int i = 0; i < chunks.size(); i++) {
            SemanticChunker.Chunk chunk = chunks.get(i);
            String chunkId = documentId + "_chunk_" + i;

            // Создание VectorData для чанка
            VectorData vectorData = new VectorData(
                    chunkId,
                    chunk.getEmbedding(),
                    chunk.getText(),
                    chunk.getText(),
                    Arrays.toString(path),
                    documentId,
                    i
            );

            // Сохранение в базу данных
            storeVectorData(vectorData);

            // Создание TreeNode для чанка
            Object[] chunkPath = Arrays.copyOf(path, path.length + 1);
            chunkPath[path.length] = "chunk_" + i;

            TreeNode treeNode = new TreeNode(chunk.getText());
            storeTreeNode(chunkId, treeNode, chunkPath);
        }

        saveDatabase();
    }

    public void storeVectorData(VectorData vectorData) {
        vectors.put(vectorData.getId(), vectorData);
        vectorIndex.addVector(vectorData.getId(), vectorData.getVector());
    }

    public void storeTreeNode(String nodeId, TreeNode node, Object[] path) {
        treeNodes.put(nodeId, node);

        // Сохраняем информацию о пути для быстрого поиска
        node.setMetadata("path", Arrays.toString(path));
        node.setMetadata("nodeId", nodeId);
    }

    public List<VectorSearchResult> similaritySearch(String queryText, int limit) throws Exception {
        // Получаем эмбеддинг для запроса
        float[] queryEmbedding = semanticChunker.getEmbedding(queryText);
        return similaritySearch(queryEmbedding, limit);
    }

    public List<VectorSearchResult> similaritySearch(float[] queryVector, int limit) {
        List<VectorIndex.SearchResult> indexResults = vectorIndex.search(queryVector, limit);
        List<VectorSearchResult> results = new ArrayList<>();

        for (VectorIndex.SearchResult indexResult : indexResults) {
            VectorData vectorData = vectors.get(indexResult.getVectorId());
            if (vectorData != null) {
                results.add(new VectorSearchResult(
                        vectorData,
                        indexResult.getSimilarity(),
                        indexResult.getDistance()
                ));
            }
        }

        return results;
    }

    public List<VectorData> exactSearch(String searchText) {
        List<VectorData> results = new ArrayList<>();
        String searchLower = searchText.toLowerCase();

        for (VectorData vectorData : vectors.values()) {
            if (vectorData.getText() != null &&
                    vectorData.getText().toLowerCase().contains(searchLower)) {
                results.add(vectorData);
            }
        }

        results.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        return results;
    }

    public List<VectorData> searchByPath(String pathPattern) {
        List<VectorData> results = new ArrayList<>();
        String patternLower = pathPattern.toLowerCase();

        for (VectorData vectorData : vectors.values()) {
            if (vectorData.getNodePath() != null &&
                    vectorData.getNodePath().toLowerCase().contains(patternLower)) {
                results.add(vectorData);
            }
        }

        results.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        return results;
    }

    public TreeNode getTreeNode(String nodeId) {
        return treeNodes.get(nodeId);
    }

    public VectorData getVectorData(String vectorId) {
        return vectors.get(vectorId);
    }

    public void removeVectorData(String vectorId) {
        vectors.remove(vectorId);
        vectorIndex.removeVector(vectorId);
        saveDatabase();
    }

    public void removeTreeNode(String nodeId) {
        treeNodes.remove(nodeId);
        saveDatabase();
    }

    public int getVectorCount() {
        return vectors.size();
    }

    public int getTreeNodeCount() {
        return treeNodes.size();
    }

    private void loadDatabase() {
        try {
            File dbFile = new File(databasePath, "vectordb.json");
            if (dbFile.exists()) {
                String content = new String(Files.readAllBytes(dbFile.toPath()));
                JSONObject dbJson = new JSONObject(content);

                // Загрузка векторов
                if (dbJson.has("vectors")) {
                    JSONObject vectorsJson = dbJson.getJSONObject("vectors");
                    for (String key : vectorsJson.keySet()) {
                        JSONObject vectorJson = vectorsJson.getJSONObject(key);
                        VectorData vectorData = VectorData.fromJson(vectorJson);
                        vectors.put(key, vectorData);
                        vectorIndex.addVector(key, vectorData.getVector());
                    }
                }

                // Загрузка treeNodes
                if (dbJson.has("treeNodes")) {
                    JSONObject nodesJson = dbJson.getJSONObject("treeNodes");
                    for (String key : nodesJson.keySet()) {
                        JSONObject nodeJson = nodesJson.getJSONObject(key);
                        TreeNode treeNode = TreeNode.fromJson(nodeJson);
                        treeNodes.put(key, treeNode);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading database: " + e.getMessage());
        }
    }

    public void saveDatabase() {
        try {
            Files.createDirectories(Paths.get(databasePath));
            File dbFile = new File(databasePath, "vectordb.json");

            JSONObject dbJson = new JSONObject();

            // Сохранение векторов
            JSONObject vectorsJson = new JSONObject();
            for (Map.Entry<String, VectorData> entry : vectors.entrySet()) {
                vectorsJson.put(entry.getKey(), entry.getValue().toJson());
            }
            dbJson.put("vectors", vectorsJson);

            // Сохранение treeNodes
            JSONObject nodesJson = new JSONObject();
            for (Map.Entry<String, TreeNode> entry : treeNodes.entrySet()) {
                nodesJson.put(entry.getKey(), entry.getValue().toJson());
            }
            dbJson.put("treeNodes", nodesJson);

            Files.write(dbFile.toPath(), dbJson.toString(2).getBytes());

        } catch (Exception e) {
            System.err.println("Error saving database: " + e.getMessage());
        }
    }

    public void close() {
        saveDatabase();
    }
}