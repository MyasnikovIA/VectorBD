package ru.miacomsoft.vectordb.core;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Класс для загрузки, поиска и выгрузки данных BinaryVectorDatabase в JSON формате
 */
public class BinaryVectorDBJsonManager {
    private final BinaryVectorDatabase database;

    public BinaryVectorDBJsonManager(BinaryVectorDatabase database) {
        this.database = database;
    }

    /**
     * Экспорт всех векторных данных в JSON массив
     */
    public JSONArray exportAllVectorDataToJson() {
        JSONArray jsonArray = new JSONArray();
        List<BinaryVectorData> allData = database.findAllVectorData();

        for (BinaryVectorData vectorData : allData) {
            jsonArray.put(vectorDataToJson(vectorData));
        }

        return jsonArray;
    }

    /**
     * Экспорт векторных данных по ID в JSON
     */
    public JSONObject exportVectorDataToJson(String vectorId) {
        BinaryVectorData vectorData = database.findVectorData(vectorId);
        if (vectorData == null) {
            throw new IllegalArgumentException("Vector data with ID '" + vectorId + "' not found");
        }
        return vectorDataToJson(vectorData);
    }

    /**
     * Экспорт всех tree nodes в JSON массив
     */
    public JSONArray exportAllTreeNodesToJson() {
        JSONArray jsonArray = new JSONArray();

        // Получаем все ключи tree nodes
        Set<String> allKeys = database.getAllKeys();

        for (String key : allKeys) {
            BinaryTreeNode treeNode = database.getTreeNode(key);
            if (treeNode != null) {
                jsonArray.put(treeNodeToJson(treeNode, key));
            }
        }

        return jsonArray;
    }

    /**
     * Экспорт tree node по ID в JSON
     */
    public JSONObject exportTreeNodeToJson(String nodeId) {
        BinaryTreeNode treeNode = database.getTreeNode(nodeId);
        if (treeNode == null) {
            throw new IllegalArgumentException("Tree node with ID '" + nodeId + "' not found");
        }
        return treeNodeToJson(treeNode, nodeId);
    }

    /**
     * Поиск векторных данных и экспорт результатов в JSON
     */
    public JSONArray searchAndExportToJson(String query, int limit) {
        try {
            JSONArray results = new JSONArray();
            List<VectorSearchResult> searchResults = database.similaritySearch(query, limit);

            for (VectorSearchResult result : searchResults) {
                JSONObject item = new JSONObject();
                item.put("vectorData", vectorDataToJson(result.getVectorData()));
                item.put("similarity", result.getSimilarity());
                item.put("distance", result.getDistance());
                results.put(item);
            }

            return results;
        } catch (Exception e) {
            throw new RuntimeException("Error during search and export: " + e.getMessage(), e);
        }
    }

    /**
     * Поиск по пути и экспорт в JSON
     */
    public JSONArray searchByPathAndExportToJson(String pathPattern) {
        JSONArray results = new JSONArray();
        List<BinaryVectorData> searchResults = database.searchByPath(pathPattern);

        for (BinaryVectorData vectorData : searchResults) {
            results.put(vectorDataToJson(vectorData));
        }

        return results;
    }

    /**
     * Импорт векторных данных из JSON массива
     */
    public int importVectorDataFromJson(JSONArray jsonArray) {
        int importedCount = 0;

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject json = jsonArray.getJSONObject(i);
                BinaryVectorData vectorData = vectorDataFromJson(json);
                database.storeVectorData(vectorData);
                importedCount++;
            } catch (JSONException e) {
                System.err.println("Error importing vector data at index " + i + ": " + e.getMessage());
            }
        }

        return importedCount;
    }

    /**
     * Импорт tree nodes из JSON массива
     */
    public int importTreeNodesFromJson(JSONArray jsonArray) {
        int importedCount = 0;

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject json = jsonArray.getJSONObject(i);
                BinaryTreeNode treeNode = treeNodeFromJson(json);
                String nodeId = json.optString("nodeId", "imported_node_" + System.currentTimeMillis() + "_" + i);
                Object[] path = parsePathFromJson(json.optString("path", "[]"));

                database.storeTreeNode(nodeId, treeNode, path);
                importedCount++;
            } catch (JSONException e) {
                System.err.println("Error importing tree node at index " + i + ": " + e.getMessage());
            }
        }

        return importedCount;
    }

    /**
     * Сохранение всей базы данных в JSON файлы
     */
    public void saveDatabaseToJsonFiles(String directoryPath) throws IOException {
        Path dir = Paths.get(directoryPath);
        Files.createDirectories(dir);

        // Сохранение векторных данных
        JSONArray vectorsJson = exportAllVectorDataToJson();
        Files.write(dir.resolve("vectors.json"), vectorsJson.toString(2).getBytes());

        // Сохранение tree nodes
        JSONArray nodesJson = exportAllTreeNodesToJson();
        Files.write(dir.resolve("treenodes.json"), nodesJson.toString(2).getBytes());

        // Сохранение метаданных базы данных
        JSONObject metadata = new JSONObject();
        metadata.put("exportTimestamp", System.currentTimeMillis());
        metadata.put("totalVectors", database.getVectorCount());
        metadata.put("totalTreeNodes", database.getTreeNodeCount());
        metadata.put("version", "1.0");

        Files.write(dir.resolve("metadata.json"), metadata.toString(2).getBytes());

        System.out.println("Database exported to JSON files in: " + directoryPath);
        System.out.println("Vectors: " + database.getVectorCount() + ", TreeNodes: " + database.getTreeNodeCount());
    }

    /**
     * Загрузка базы данных из JSON файлов
     */
    public void loadDatabaseFromJsonFiles(String directoryPath) throws IOException {
        Path dir = Paths.get(directoryPath);

        if (!Files.exists(dir)) {
            throw new IOException("Directory does not exist: " + directoryPath);
        }

        int vectorsLoaded = 0;
        int nodesLoaded = 0;

        // Загрузка векторных данных
        Path vectorsFile = dir.resolve("vectors.json");
        if (Files.exists(vectorsFile)) {
            String vectorsJsonStr = new String(Files.readAllBytes(vectorsFile));
            JSONArray vectorsJson = new JSONArray(vectorsJsonStr);
            vectorsLoaded = importVectorDataFromJson(vectorsJson);
        }

        // Загрузка tree nodes
        Path nodesFile = dir.resolve("treenodes.json");
        if (Files.exists(nodesFile)) {
            String nodesJsonStr = new String(Files.readAllBytes(nodesFile));
            JSONArray nodesJson = new JSONArray(nodesJsonStr);
            nodesLoaded = importTreeNodesFromJson(nodesJson);
        }

        System.out.println("Database loaded from JSON files: " + vectorsLoaded + " vectors, " + nodesLoaded + " tree nodes");
    }

    /**
     * Экспорт статистики базы данных в JSON
     */
    public JSONObject exportDatabaseStatsToJson() {
        JSONObject stats = new JSONObject();

        stats.put("totalVectors", database.getVectorCount());
        stats.put("totalTreeNodes", database.getTreeNodeCount());
        stats.put("databasePath", database.toString()); // Используем toString для базовой информации

        // Информация о памяти, если доступна
        try {
            Map<String, Object> memoryStats = database.getMemoryStats();
            stats.put("memoryStats", new JSONObject(memoryStats));
        } catch (Exception e) {
            // Игнорируем, если метод getMemoryStats не доступен
        }

        // Информация об индексах, если доступна
        try {
            Map<String, Object> indexesInfo = database.getIndexesInfo();
            stats.put("indexes", new JSONObject(indexesInfo));
        } catch (Exception e) {
            // Игнорируем, если метод getIndexesInfo не доступен
        }

        stats.put("exportTimestamp", System.currentTimeMillis());

        return stats;
    }

    /**
     * Пакетный импорт данных из JSON файла
     */
    public Map<String, Integer> batchImportFromJsonFile(String filePath) throws IOException {
        Map<String, Integer> results = new HashMap<>();

        String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)));
        JSONObject importData = new JSONObject(jsonContent);

        if (importData.has("vectors")) {
            JSONArray vectorsArray = importData.getJSONArray("vectors");
            int vectorsCount = importVectorDataFromJson(vectorsArray);
            results.put("vectors", vectorsCount);
        }

        if (importData.has("treeNodes")) {
            JSONArray nodesArray = importData.getJSONArray("treeNodes");
            int nodesCount = importTreeNodesFromJson(nodesArray);
            results.put("treeNodes", nodesCount);
        }

        return results;
    }

    /**
     * Пакетный экспорт данных в JSON файл
     */
    public void batchExportToJsonFile(String filePath) throws IOException {
        JSONObject exportData = new JSONObject();

        exportData.put("vectors", exportAllVectorDataToJson());
        exportData.put("treeNodes", exportAllTreeNodesToJson());
        exportData.put("metadata", exportDatabaseStatsToJson());
        exportData.put("exportTimestamp", System.currentTimeMillis());

        Files.write(Paths.get(filePath), exportData.toString(2).getBytes());
        System.out.println("Batch export completed: " + filePath);
    }

    // Вспомогательные методы

    private JSONObject vectorDataToJson(BinaryVectorData vectorData) {
        JSONObject json = new JSONObject();

        json.put("id", vectorData.getId());
        json.put("text", vectorData.getText());
        json.put("metadata", vectorData.getMetadata());
        json.put("nodePath", vectorData.getNodePath());
        json.put("documentId", vectorData.getDocumentId());
        json.put("chunkIndex", vectorData.getChunkIndex());
        json.put("timestamp", vectorData.getTimestamp());

        // Вектор как массив чисел
        float[] vector = vectorData.getVector();
        if (vector != null) {
            JSONArray vectorArray = new JSONArray();
            for (float value : vector) {
                vectorArray.put(value);
            }
            json.put("vector", vectorArray);
            json.put("vectorDimensions", vector.length);
        }

        return json;
    }

    private BinaryVectorData vectorDataFromJson(JSONObject json) {
        BinaryVectorData vectorData = new BinaryVectorData();

        vectorData.setId(json.getString("id"));
        vectorData.setText(json.optString("text", ""));
        vectorData.setMetadata(json.optString("metadata", ""));
        vectorData.setNodePath(json.optString("nodePath", ""));
        vectorData.setDocumentId(json.optString("documentId", ""));
        vectorData.setChunkIndex(json.optInt("chunkIndex", -1));
        vectorData.setTimestamp(json.optLong("timestamp", System.currentTimeMillis()));

        // Восстановление вектора
        if (json.has("vector")) {
            JSONArray vectorArray = json.getJSONArray("vector");
            float[] vector = new float[vectorArray.length()];
            for (int i = 0; i < vectorArray.length(); i++) {
                vector[i] = (float) vectorArray.getDouble(i);
            }
            vectorData.setVector(vector);
        }

        return vectorData;
    }

    private JSONObject treeNodeToJson(BinaryTreeNode treeNode, String nodeId) {
        JSONObject json = new JSONObject();

        json.put("nodeId", nodeId);
        json.put("content", treeNode.getContent());

        // Метаданные
        Map<String, Object> metadata = treeNode.getMetadata();
        if (metadata != null && !metadata.isEmpty()) {
            JSONObject metadataJson = new JSONObject();
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                metadataJson.put(entry.getKey(), entry.getValue().toString());
            }
            json.put("metadata", metadataJson);
        }

        // Путь из метаданных
        Object pathObj = treeNode.getMetadata("path");
        if (pathObj != null) {
            json.put("path", pathObj.toString());
        }

        return json;
    }

    private BinaryTreeNode treeNodeFromJson(JSONObject json) {
        BinaryTreeNode treeNode = new BinaryTreeNode();

        treeNode.setContent(json.optString("content", ""));

        // Восстановление метаданных
        if (json.has("metadata")) {
            JSONObject metadataJson = json.getJSONObject("metadata");
            for (String key : metadataJson.keySet()) {
                treeNode.setMetadata(key, metadataJson.get(key));
            }
        }

        return treeNode;
    }

    private Object[] parsePathFromJson(String pathStr) {
        if (pathStr == null || pathStr.trim().isEmpty() || pathStr.equals("[]")) {
            return new Object[0];
        }

        try {
            // Упрощенный парсинг пути вида "[element1, element2, element3]"
            String cleanPath = pathStr.replace("[", "").replace("]", "").trim();
            if (cleanPath.isEmpty()) {
                return new Object[0];
            }

            String[] parts = cleanPath.split(",\\s*");
            return Arrays.stream(parts)
                    .map(String::trim)
                    .toArray(Object[]::new);

        } catch (Exception e) {
            System.err.println("Error parsing path: " + pathStr + ", using empty path");
            return new Object[0];
        }
    }

    /**
     * Создание JSON строки из VectorData
     */
    public String vectorDataToJsonString(BinaryVectorData vectorData) {
        return vectorDataToJson(vectorData).toString();
    }

    /**
     * Создание VectorData из JSON строки
     */
    public BinaryVectorData vectorDataFromJsonString(String jsonString) {
        JSONObject json = new JSONObject(jsonString);
        return vectorDataFromJson(json);
    }

    /**
     * Создание JSON строки из TreeNode
     */
    public String treeNodeToJsonString(BinaryTreeNode treeNode, String nodeId) {
        return treeNodeToJson(treeNode, nodeId).toString();
    }

    /**
     * Создание TreeNode из JSON строки
     */
    public BinaryTreeNode treeNodeFromJsonString(String jsonString) {
        JSONObject json = new JSONObject(jsonString);
        return treeNodeFromJson(json);
    }

    /**
     * Получить JSONObject из VectorData
     */
    public JSONObject vectorDataToJsonObject(BinaryVectorData vectorData) {
        return vectorDataToJson(vectorData);
    }

    /**
     * Получить JSONObject из TreeNode
     */
    public JSONObject treeNodeToJsonObject(BinaryTreeNode treeNode, String nodeId) {
        return treeNodeToJson(treeNode, nodeId);
    }

    /**
     * Фильтрация и экспорт векторных данных по критериям
     */
    public JSONArray filterAndExportVectorData(Map<String, Object> filters) {
        JSONArray results = new JSONArray();
        List<BinaryVectorData> allData = database.findAllVectorData();

        for (BinaryVectorData vectorData : allData) {
            if (matchesFilters(vectorData, filters)) {
                results.put(vectorDataToJson(vectorData));
            }
        }

        return results;
    }

    private boolean matchesFilters(BinaryVectorData vectorData, Map<String, Object> filters) {
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            String key = filter.getKey();
            Object value = filter.getValue();

            switch (key) {
                case "documentId":
                    if (!vectorData.getDocumentId().equals(value)) return false;
                    break;
                case "chunkIndex":
                    if (vectorData.getChunkIndex() != (Integer) value) return false;
                    break;
                case "textContains":
                    if (!vectorData.getText().contains((String) value)) return false;
                    break;
                case "minTimestamp":
                    if (vectorData.getTimestamp() < (Long) value) return false;
                    break;
                case "maxTimestamp":
                    if (vectorData.getTimestamp() > (Long) value) return false;
                    break;
            }
        }

        return true;
    }
}