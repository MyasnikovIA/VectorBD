package ru.miacomsoft.vectordb.core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class BinaryVectorDatabase {
    private final String databasePath;
    private final Map<String, BinaryVectorData> vectors;
    private final Map<String, BinaryTreeNode> treeNodes;
    private final SemanticChunker semanticChunker;
    private final VectorIndex vectorIndex;

    // Новые поля для управления индексами и памятью
    private final Map<String, Map<String, Set<String>>> indexes;
    private final ExecutorService backgroundExecutor;
    private final long maxMemoryBytes;
    private final AtomicLong currentMemoryUsage;
    private final Runtime runtime;

    // Константы для управления памятью
    private static final long DEFAULT_MAX_MEMORY_BYTES = 500 * 1024 * 1024; // 500 MB
    private static final long MEMORY_CHECK_THRESHOLD = 50 * 1024 * 1024; // 50 MB
    private static final int BACKGROUND_THREADS = 2;

    // Конструктор по умолчанию (500 MB памяти)
    public BinaryVectorDatabase(String databasePath, SemanticChunker semanticChunker) {
        this(databasePath, semanticChunker, DEFAULT_MAX_MEMORY_BYTES);
    }

    // Новый конструктор с настройкой памяти
    public BinaryVectorDatabase(String databasePath, SemanticChunker semanticChunker, long maxMemoryBytes) {
        this.databasePath = databasePath;
        this.semanticChunker = semanticChunker;
        this.vectors = new ConcurrentHashMap<>();
        this.treeNodes = new ConcurrentHashMap<>();
        this.vectorIndex = new VectorIndex();
        this.indexes = new ConcurrentHashMap<>();

        // Управление памятью
        this.maxMemoryBytes = maxMemoryBytes > 0 ? maxMemoryBytes : DEFAULT_MAX_MEMORY_BYTES;
        this.currentMemoryUsage = new AtomicLong(0);
        this.runtime = Runtime.getRuntime();

        // Фоновый исполнитель для оптимизации
        this.backgroundExecutor = Executors.newFixedThreadPool(BACKGROUND_THREADS);

        loadDatabase();
        startMemoryMonitor();

        System.out.println("BinaryVectorDatabase initialized with " +
                (maxMemoryBytes / (1024 * 1024)) + " MB memory limit");
    }

    /**
     * Создание индекса для ускорения поиска по BinaryTreeNode
     * @param indexName имя индекса
     * @param fieldName поле для индексации (поддерживаются: "content", "metadata.key")
     */
    public void createIndex(String indexName, String fieldName) {
        if (indexName == null || fieldName == null || indexName.trim().isEmpty() || fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("Index name and field name cannot be null or empty");
        }

        if (indexes.containsKey(indexName)) {
            System.out.println("Index '" + indexName + "' already exists. Recreating...");
            indexes.remove(indexName);
        }

        System.out.println("Creating index '" + indexName + "' for field '" + fieldName + "'");

        Map<String, Set<String>> index = new ConcurrentHashMap<>();
        long startTime = System.currentTimeMillis();
        int indexedNodes = 0;

        try {
            for (Map.Entry<String, BinaryTreeNode> entry : treeNodes.entrySet()) {
                String nodeId = entry.getKey();
                BinaryTreeNode node = entry.getValue();

                Object fieldValue = extractFieldValue(node, fieldName);
                if (fieldValue != null) {
                    String valueKey = fieldValue.toString().toLowerCase();

                    index.computeIfAbsent(valueKey, k -> ConcurrentHashMap.newKeySet())
                            .add(nodeId);
                    indexedNodes++;
                }

                // Периодическая проверка памяти
                if (indexedNodes % 1000 == 0) {
                    checkMemoryUsage();
                }
            }

            indexes.put(indexName, index);
            long endTime = System.currentTimeMillis();

            System.out.println("Index '" + indexName + "' created successfully. " +
                    "Indexed " + indexedNodes + " nodes in " + (endTime - startTime) + " ms. " +
                    "Index size: " + index.size() + " unique keys.");

        } catch (Exception e) {
            System.err.println("Error creating index '" + indexName + "': " + e.getMessage());
            throw new RuntimeException("Failed to create index", e);
        }
    }

    /**
     * Удаление индекса
     * @param indexName имя индекса для удаления
     */
    public void dropIndex(String indexName) {
        if (indexName == null || indexName.trim().isEmpty()) {
            throw new IllegalArgumentException("Index name cannot be null or empty");
        }

        if (indexes.containsKey(indexName)) {
            Map<String, Set<String>> removedIndex = indexes.remove(indexName);
            long memoryFreed = estimateIndexMemoryUsage(removedIndex);
            currentMemoryUsage.addAndGet(-memoryFreed);

            System.out.println("Index '" + indexName + "' dropped successfully. " +
                    "Freed approximately " + (memoryFreed / 1024) + " KB of memory.");
        } else {
            System.out.println("Index '" + indexName + "' does not exist.");
        }
    }

    /**
     * Поиск по индексу
     * @param indexName имя индекса
     * @param fieldValue значение для поиска
     * @return список ID узлов, соответствующих значению
     */
    public List<String> searchByIndex(String indexName, String fieldValue) {
        if (!indexes.containsKey(indexName)) {
            throw new IllegalArgumentException("Index '" + indexName + "' does not exist");
        }

        Map<String, Set<String>> index = indexes.get(indexName);
        String searchKey = fieldValue.toLowerCase();

        Set<String> result = index.get(searchKey);
        if (result != null) {
            return new ArrayList<>(result);
        }

        // Поиск по частичному совпадению
        List<String> partialMatches = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : index.entrySet()) {
            if (entry.getKey().contains(searchKey)) {
                partialMatches.addAll(entry.getValue());
            }
        }

        return partialMatches;
    }

    /**
     * Получить информацию обо всех индексах
     */
    public Map<String, Object> getIndexesInfo() {
        Map<String, Object> info = new HashMap<>();
        for (Map.Entry<String, Map<String, Set<String>>> entry : indexes.entrySet()) {
            Map<String, Object> indexInfo = new HashMap<>();
            indexInfo.put("size", entry.getValue().size());
            indexInfo.put("estimatedMemoryKB", estimateIndexMemoryUsage(entry.getValue()) / 1024);
            info.put(entry.getKey(), indexInfo);
        }
        return info;
    }

    // Вспомогательные методы для работы с индексами

    private Object extractFieldValue(BinaryTreeNode node, String fieldName) {
        if (fieldName.equals("content")) {
            return node.getContent();
        } else if (fieldName.startsWith("metadata.")) {
            String metadataKey = fieldName.substring("metadata.".length());
            return node.getMetadata(metadataKey);
        } else {
            // Поддержка других полей при необходимости
            return null;
        }
    }

    private long estimateIndexMemoryUsage(Map<String, Set<String>> index) {
        long size = 0;
        for (Map.Entry<String, Set<String>> entry : index.entrySet()) {
            // Приблизительный расчет размера: ключ + значения
            size += entry.getKey().length() * 2L; // String в UTF-16
            for (String nodeId : entry.getValue()) {
                size += nodeId.length() * 2L; // String в UTF-16
                size += 16; // Примерный размер записи в HashSet
            }
        }
        return size;
    }

    // Методы для управления памятью

    private void startMemoryMonitor() {
        backgroundExecutor.submit(() -> {
            while (!backgroundExecutor.isShutdown()) {
                try {
                    checkMemoryUsage();
                    Thread.sleep(30000); // Проверка каждые 30 секунд
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Error in memory monitor: " + e.getMessage());
                }
            }
        });
    }

    private void checkMemoryUsage() {
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long estimatedUsage = currentMemoryUsage.get();

        if (usedMemory > maxMemoryBytes * 0.9 || estimatedUsage > maxMemoryBytes * 0.8) {
            System.out.println("Memory usage high: " +
                    (usedMemory / (1024 * 1024)) + "MB used, " +
                    (estimatedUsage / (1024 * 1024)) + "MB estimated. " +
                    "Limit: " + (maxMemoryBytes / (1024 * 1024)) + "MB");

            // Автоматическая оптимизация при нехватке памяти
            optimizeMemory();
        }
    }

    private void optimizeMemory() {
        System.out.println("Performing memory optimization...");

        // 1. Сохранение базы данных на диск
        saveDatabase();

        // 2. Очистка кэшей, если они есть
        runtime.gc();

        // 3. Уведомление о необходимости ручной оптимизации
        System.out.println("Memory optimization completed. Consider:");
        System.out.println("  - Increasing memory limit in constructor");
        System.out.println("  - Removing unused indexes");
        System.out.println("  - Archiving old data");
    }

    private void updateMemoryUsage(long delta) {
        currentMemoryUsage.addAndGet(delta);

        // Периодическая проверка при значительных изменениях
        if (Math.abs(delta) > MEMORY_CHECK_THRESHOLD) {
            checkMemoryUsage();
        }
    }

    // Модифицированные методы для учета использования памяти


    public void storeVectorData(BinaryVectorData vectorData) {
        String vectorId = vectorData.getId();
        BinaryVectorData oldData = vectors.put(vectorId, vectorData);

        // Обновление использования памяти
        if (oldData != null) {
            updateMemoryUsage(-estimateVectorDataMemoryUsage(oldData));
        }
        updateMemoryUsage(estimateVectorDataMemoryUsage(vectorData));

        vectorIndex.addVector(vectorId, vectorData.getVector());
    }


    public void storeTreeNode(String nodeId, BinaryTreeNode node, Object[] path) {
        BinaryTreeNode oldNode = treeNodes.put(nodeId, node);

        // Обновление использования памяти
        if (oldNode != null) {
            updateMemoryUsage(-estimateTreeNodeMemoryUsage(oldNode));
        }
        updateMemoryUsage(estimateTreeNodeMemoryUsage(node));

        // Сохраняем информацию о пути для быстрого поиска
        node.setMetadata("path", Arrays.toString(path));
        node.setMetadata("nodeId", nodeId);

        // Обновление индексов в фоновом режиме
        updateIndexesForNode(nodeId, node, oldNode);
    }


    public void removeVectorData(String vectorId) {
        BinaryVectorData removed = vectors.remove(vectorId);
        if (removed != null) {
            updateMemoryUsage(-estimateVectorDataMemoryUsage(removed));
            vectorIndex.removeVector(vectorId);
        }
    }


    public void removeTreeNode(String nodeId) {
        BinaryTreeNode removed = treeNodes.remove(nodeId);
        if (removed != null) {
            updateMemoryUsage(-estimateTreeNodeMemoryUsage(removed));
            removeNodeFromIndexes(nodeId, removed);
        }
    }

    // Вспомогательные методы для оценки использования памяти

    private long estimateVectorDataMemoryUsage(BinaryVectorData data) {
        long size = 0;
        if (data.getId() != null) size += data.getId().length() * 2L;
        if (data.getText() != null) size += data.getText().length() * 2L;
        if (data.getMetadata() != null) size += data.getMetadata().length() * 2L;
        if (data.getNodePath() != null) size += data.getNodePath().length() * 2L;
        if (data.getDocumentId() != null) size += data.getDocumentId().length() * 2L;
        if (data.getVector() != null) size += data.getVector().length * 4L; // float = 4 bytes
        size += 32; // базовый размер объекта
        return size;
    }

    private long estimateTreeNodeMemoryUsage(BinaryTreeNode node) {
        long size = 0;
        if (node.getContent() != null) size += node.getContent().length() * 2L;
        if (node.getMetadata() != null) {
            for (Map.Entry<String, Object> entry : node.getMetadata().entrySet()) {
                size += entry.getKey().length() * 2L;
                if (entry.getValue() != null) {
                    size += entry.getValue().toString().length() * 2L;
                }
            }
        }
        size += 48; // базовый размер объекта + ссылки на left/right
        return size;
    }

    // Методы для обновления индексов

    private void updateIndexesForNode(String nodeId, BinaryTreeNode newNode, BinaryTreeNode oldNode) {
        backgroundExecutor.submit(() -> {
            try {
                for (Map.Entry<String, Map<String, Set<String>>> indexEntry : indexes.entrySet()) {
                    String indexName = indexEntry.getKey();
                    Map<String, Set<String>> index = indexEntry.getValue();
                    String fieldName = extractFieldNameFromIndexName(indexName);

                    // Удаление старого значения из индекса
                    if (oldNode != null) {
                        Object oldValue = extractFieldValue(oldNode, fieldName);
                        if (oldValue != null) {
                            String oldKey = oldValue.toString().toLowerCase();
                            Set<String> oldSet = index.get(oldKey);
                            if (oldSet != null) {
                                oldSet.remove(nodeId);
                                if (oldSet.isEmpty()) {
                                    index.remove(oldKey);
                                }
                            }
                        }
                    }

                    // Добавление нового значения в индекс
                    Object newValue = extractFieldValue(newNode, fieldName);
                    if (newValue != null) {
                        String newKey = newValue.toString().toLowerCase();
                        index.computeIfAbsent(newKey, k -> ConcurrentHashMap.newKeySet())
                                .add(nodeId);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error updating indexes for node " + nodeId + ": " + e.getMessage());
            }
        });
    }

    private void removeNodeFromIndexes(String nodeId, BinaryTreeNode node) {
        backgroundExecutor.submit(() -> {
            try {
                for (Map.Entry<String, Map<String, Set<String>>> indexEntry : indexes.entrySet()) {
                    Map<String, Set<String>> index = indexEntry.getValue();
                    String fieldName = extractFieldNameFromIndexName(indexEntry.getKey());

                    Object value = extractFieldValue(node, fieldName);
                    if (value != null) {
                        String key = value.toString().toLowerCase();
                        Set<String> set = index.get(key);
                        if (set != null) {
                            set.remove(nodeId);
                            if (set.isEmpty()) {
                                index.remove(key);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error removing node " + nodeId + " from indexes: " + e.getMessage());
            }
        });
    }

    private String extractFieldNameFromIndexName(String indexName) {
        // Простая логика извлечения имени поля из имени индекса
        // В реальной реализации может быть сложнее
        if (indexName.contains("_content")) {
            return "content";
        } else if (indexName.contains("_metadata_")) {
            return "metadata." + indexName.split("_metadata_")[1];
        }
        return "content"; // fallback
    }

    // Методы для управления жизненным циклом

    /**
     * Грациозное завершение работы базы данных
     */
    public void close() {
        try {
            // Остановка фоновых задач
            backgroundExecutor.shutdown();
            if (!backgroundExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                backgroundExecutor.shutdownNow();
            }

            // Сохранение базы данных
            saveDatabase();

            System.out.println("BinaryVectorDatabase closed successfully. " +
                    "Final memory usage: " + (currentMemoryUsage.get() / (1024 * 1024)) + " MB");

        } catch (InterruptedException e) {
            backgroundExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Получить текущую статистику использования памяти
     */
    public Map<String, Object> getMemoryStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("maxMemoryMB", maxMemoryBytes / (1024 * 1024));
        stats.put("estimatedUsageMB", currentMemoryUsage.get() / (1024 * 1024));
        stats.put("jvmUsedMB", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        stats.put("jvmFreeMB", runtime.freeMemory() / (1024 * 1024));
        stats.put("jvmTotalMB", runtime.totalMemory() / (1024 * 1024));
        stats.put("jvmMaxMB", runtime.maxMemory() / (1024 * 1024));
        return stats;
    }

    // Остальной существующий функционал остается без изменений...

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

                // Обновление использования памяти
                updateMemoryUsage(estimateVectorDataMemoryUsage(vectorData));
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

                // Обновление использования памяти
                updateMemoryUsage(estimateTreeNodeMemoryUsage(treeNode));
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
    public List<Map<String, Object>> searchSimilarText(String query, int limit) throws Exception {
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