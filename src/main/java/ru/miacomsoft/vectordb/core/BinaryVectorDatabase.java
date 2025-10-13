package ru.miacomsoft.vectordb.core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BinaryVectorDatabase {
    private final String databasePath;
    private final Map<String, BinaryVectorData> vectors;
    private final Map<String, BinaryTreeNode> treeNodes;
    private final SemanticChunker semanticChunker;
    private final VectorIndex vectorIndex;

    public BinaryVectorDatabase(String databasePath, SemanticChunker semanticChunker) {
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

            // Создание BinaryVectorData для чанка
            BinaryVectorData vectorData = new BinaryVectorData(
                    chunkId,
                    chunk.getEmbedding(),
                    chunk.getText(),
                    chunk.getText(),
                    Arrays.toString(path),
                    documentId
            );

            // Сохранение в базу данных
            storeVectorData(vectorData);

            // Создание BinaryTreeNode для чанка
            Object[] chunkPath = Arrays.copyOf(path, path.length + 1);
            chunkPath[path.length] = "chunk_" + i;

            BinaryTreeNode treeNode = new BinaryTreeNode(chunk.getText());
            storeTreeNode(chunkId, treeNode, chunkPath);
        }

        saveDatabase();
    }

    public void storeVectorData(BinaryVectorData vectorData) {
        vectors.put(vectorData.getId(), vectorData);
        vectorIndex.addVector(vectorData.getId(), vectorData.getVector());
    }

    public void storeTreeNode(String nodeId, BinaryTreeNode node, Object[] path) {
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
            BinaryVectorData vectorData = vectors.get(indexResult.getVectorId());
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

    public List<BinaryVectorData> searchByPath(String pathPattern) {
        List<BinaryVectorData> results = new ArrayList<>();
        String patternLower = pathPattern.toLowerCase();

        for (BinaryVectorData vectorData : vectors.values()) {
            if (vectorData.getNodePath() != null &&
                    vectorData.getNodePath().toLowerCase().contains(patternLower)) {
                results.add(vectorData);
            }
        }

        results.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        return results;
    }

    public BinaryTreeNode getTreeNode(String nodeId) {
        return treeNodes.get(nodeId);
    }

    public BinaryVectorData getVectorData(String vectorId) {
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
            Path dbDir = Paths.get(databasePath);
            if (!Files.exists(dbDir)) {
                return;
            }

            // Загрузка векторов
            Path vectorsFile = dbDir.resolve("vectors.bin");
            if (Files.exists(vectorsFile)) {
                byte[] vectorsData = Files.readAllBytes(vectorsFile);
                loadVectorsFromBinary(vectorsData);
            }

            // Загрузка treeNodes
            Path nodesFile = dbDir.resolve("treenodes.bin");
            if (Files.exists(nodesFile)) {
                byte[] nodesData = Files.readAllBytes(nodesFile);
                loadTreeNodesFromBinary(nodesData);
            }

            System.out.println("Database loaded successfully: " + vectors.size() + " vectors, " +
                    treeNodes.size() + " tree nodes");

        } catch (Exception e) {
            System.err.println("Error loading database: " + e.getMessage());
        }
    }

    public void saveDatabase() {
        try {
            Files.createDirectories(Paths.get(databasePath));

            // Сохранение векторов
            byte[] vectorsData = saveVectorsToBinary();
            Files.write(Paths.get(databasePath, "vectors.bin"), vectorsData);

            // Сохранение treeNodes
            byte[] nodesData = saveTreeNodesToBinary();
            Files.write(Paths.get(databasePath, "treenodes.bin"), nodesData);

            System.out.println("Database saved successfully: " + vectors.size() + " vectors, " +
                    treeNodes.size() + " tree nodes");

        } catch (Exception e) {
            System.err.println("Error saving database: " + e.getMessage());
        }
    }

    private byte[] saveVectorsToBinary() throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            dos.writeInt(1); // версия
            dos.writeInt(vectors.size());

            for (Map.Entry<String, BinaryVectorData> entry : vectors.entrySet()) {
                writeString(dos, entry.getKey());
                byte[] vectorData = entry.getValue().serialize();
                dos.writeInt(vectorData.length);
                dos.write(vectorData);
            }

            return baos.toByteArray();
        }
    }

    private void loadVectorsFromBinary(byte[] data) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             DataInputStream dis = new DataInputStream(bais)) {

            int version = dis.readInt();
            if (version != 1) {
                throw new IOException("Unsupported vectors format version: " + version);
            }

            int count = dis.readInt();
            for (int i = 0; i < count; i++) {
                String key = readString(dis);
                int vectorDataLength = dis.readInt();
                byte[] vectorDataBytes = new byte[vectorDataLength];
                dis.readFully(vectorDataBytes);

                BinaryVectorData vectorData = BinaryVectorData.deserialize(vectorDataBytes);
                vectors.put(key, vectorData);
                vectorIndex.addVector(key, vectorData.getVector());
            }
        }
    }

    private byte[] saveTreeNodesToBinary() throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            dos.writeInt(1); // версия
            dos.writeInt(treeNodes.size());

            for (Map.Entry<String, BinaryTreeNode> entry : treeNodes.entrySet()) {
                writeString(dos, entry.getKey());
                byte[] nodeData = entry.getValue().serialize();
                dos.writeInt(nodeData.length);
                dos.write(nodeData);
            }

            return baos.toByteArray();
        }
    }

    private void loadTreeNodesFromBinary(byte[] data) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             DataInputStream dis = new DataInputStream(bais)) {

            int version = dis.readInt();
            if (version != 1) {
                throw new IOException("Unsupported tree nodes format version: " + version);
            }

            int count = dis.readInt();
            for (int i = 0; i < count; i++) {
                String key = readString(dis);
                int nodeDataLength = dis.readInt();
                byte[] nodeDataBytes = new byte[nodeDataLength];
                dis.readFully(nodeDataBytes);

                BinaryTreeNode treeNode = BinaryTreeNode.deserialize(nodeDataBytes);
                treeNodes.put(key, treeNode);
            }
        }
    }

    private void writeString(DataOutputStream dos, String str) throws IOException {
        if (str == null) {
            dos.writeInt(-1);
        } else {
            byte[] bytes = str.getBytes("UTF-8");
            dos.writeInt(bytes.length);
            dos.write(bytes);
        }
    }

    private String readString(DataInputStream dis) throws IOException {
        int length = dis.readInt();
        if (length == -1) return null;
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        return new String(bytes, "UTF-8");
    }

    public void close() {
        saveDatabase();
    }



    /**
     * Найти все векторные данные
     */
    public List<BinaryVectorData> findAllVectorData() {
        return new ArrayList<>(vectors.values());
    }

    /**
     * Найти векторные данные по ID
     */
    public BinaryVectorData findVectorData(String id) {
        return vectors.get(id);
    }

    /**
     * Получить все ключи
     */
    public Set<String> getAllKeys() {
        return vectors.keySet();
    }

    /**
     * Создать индекс
     */
    public void createIndex(String indexName, String columnName) {
        // Реализация создания индекса
        System.out.println("Creating index: " + indexName + " on column: " + columnName);
    }

    /**
     * Удалить индекс
     */
    public void dropIndex(String indexName) {
        // Реализация удаления индекса
        System.out.println("Dropping index: " + indexName);
    }

    /**
     * Точный поиск по тексту
     */
    public List<BinaryVectorData> exactSearch(String searchText) {
        List<BinaryVectorData> results = new ArrayList<>();
        String searchLower = searchText.toLowerCase();

        for (BinaryVectorData vectorData : vectors.values()) {
            if (vectorData.getText() != null &&
                    vectorData.getText().toLowerCase().contains(searchLower)) {
                results.add(vectorData);
            }
        }

        results.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        return results;
    }
    /**
     * Поиск похожих текстов с возвратом результатов в формате Map
     */
    public List<Map<String, Object>> searchSimilarText(String query, int limit) {
        try {
            List<Map<String, Object>> results = new ArrayList<>();

            // Используем существующий метод similaritySearch
            List<VectorSearchResult> searchResults = similaritySearch(query, limit);

            for (VectorSearchResult result : searchResults) {
                BinaryVectorData vectorData = result.getVectorData();
                Map<String, Object> resultMap = new HashMap<>();

                resultMap.put("id", vectorData.getId());
                resultMap.put("content", vectorData.getText());
                resultMap.put("metadata", vectorData.getMetadata());
                resultMap.put("similarity", result.getSimilarity());
                resultMap.put("distance", result.getDistance());
                resultMap.put("documentId", vectorData.getDocumentId());
                resultMap.put("chunkIndex", vectorData.getChunkIndex());
                resultMap.put("nodePath", vectorData.getNodePath());

                results.add(resultMap);
            }

            return results;

        } catch (Exception e) {
            System.err.println("Error in searchSimilarText: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Перегруженная версия с порогом схожести
     */
    public List<Map<String, Object>> searchSimilarText(String query, int limit, double similarityThreshold) {
        try {
            List<Map<String, Object>> results = new ArrayList<>();

            List<VectorSearchResult> searchResults = similaritySearch(query, limit * 2); // Ищем больше, чтобы отфильтровать

            for (VectorSearchResult result : searchResults) {
                if (result.getSimilarity() >= similarityThreshold) {
                    BinaryVectorData vectorData = result.getVectorData();
                    Map<String, Object> resultMap = new HashMap<>();

                    resultMap.put("id", vectorData.getId());
                    resultMap.put("content", vectorData.getText());
                    resultMap.put("metadata", vectorData.getMetadata());
                    resultMap.put("similarity", result.getSimilarity());
                    resultMap.put("distance", result.getDistance());
                    resultMap.put("documentId", vectorData.getDocumentId());
                    resultMap.put("chunkIndex", vectorData.getChunkIndex());
                    resultMap.put("nodePath", vectorData.getNodePath());

                    results.add(resultMap);

                    // Останавливаемся когда достигли лимита
                    if (results.size() >= limit) {
                        break;
                    }
                }
            }

            return results;

        } catch (Exception e) {
            System.err.println("Error in searchSimilarText: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}