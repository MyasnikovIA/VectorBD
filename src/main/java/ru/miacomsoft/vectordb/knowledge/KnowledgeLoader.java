package ru.miacomsoft.vectordb.knowledge;

import ru.miacomsoft.vectordb.core.SemanticChunker;
import ru.miacomsoft.vectordb.core.VectorDatabase;
import ru.miacomsoft.vectordb.core.SemanticChunker.Chunk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Загрузчик текстовых знаний с использованием SemanticChunker
 * для разбиения текста на семантические чанки и сохранения в векторную базу
 */
public class KnowledgeLoader {
    private final VectorDatabase database;
    private final SemanticChunker semanticChunker;
    private final KnowledgeConfig knowledgeConfig;

    public KnowledgeLoader(VectorDatabase database, KnowledgeConfig knowledgeConfig) {
        this.database = database;
        this.knowledgeConfig = knowledgeConfig;

        this.semanticChunker = new SemanticChunker(
                knowledgeConfig.getOllamaUrl(),
                "all-minilm:22m", // модель для эмбеддингов
                knowledgeConfig.getSimilarityThreshold()
        );

        configureFromConfig();
    }

    /**
     * Настройка KnowledgeLoader из конфигурации
     */
    private void configureFromConfig() {
        if (knowledgeConfig.isEnabled()) {
            System.out.println("KnowledgeLoader configured:");
            System.out.println("  - Model: " + knowledgeConfig.getModel());
            System.out.println("  - Ollama URL: " + knowledgeConfig.getOllamaUrl());
            System.out.println("  - Similarity Threshold: " + knowledgeConfig.getSimilarityThreshold());
            System.out.println("  - Save History: " + knowledgeConfig.isSaveHistoryEnabled());
        } else {
            System.out.println("Knowledge functionality is disabled in configuration");
        }
    }

    /**
     * Получить текущую конфигурацию Knowledge
     */
    public Map<String, Object> getKnowledgeConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", knowledgeConfig.isEnabled());
        config.put("ollamaUrl", knowledgeConfig.getOllamaUrl());
        config.put("model", knowledgeConfig.getModel());
        config.put("similarityThreshold", knowledgeConfig.getSimilarityThreshold());
        config.put("saveHistory", knowledgeConfig.isSaveHistoryEnabled());
        config.put("currentThreshold", getCurrentSimilarityThreshold());

        return config;
    }

    /**
     * Установить порог схожести для семантического чанкинга
     * @param threshold порог схожести (0.0 - 1.0)
     */
    public void setSimilarityThreshold(double threshold) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Similarity threshold must be between 0.0 and 1.0");
        }
        semanticChunker.setSimilarityThreshold(threshold);
        System.out.println("Similarity threshold set to: " + threshold);
    }

    /**
     * Получить текущий порог схожести
     */
    public double getCurrentSimilarityThreshold() {
        return semanticChunker.getSimilarityThreshold();
    }

    /**
     * Получить информацию о конфигурации SemanticChunker
     */
    public String getSemanticChunkerConfig() {
        return semanticChunker.getConfigInfo();
    }

    /**
     * Загрузить текстовый файл и сохранить чанки в базу
     * @param filePath путь к файлу
     * @param documentId идентификатор документа
     * @param path путь в базе данных
     * @param maxChunkSize максимальный размер чанка в символах
     * @return количество загруженных чанков
     */
    public int loadTextFile(String filePath, String documentId, Object[] path, int maxChunkSize) throws Exception {
        System.out.println("Loading text file: " + filePath);
        System.out.println("Using similarity threshold: " + getCurrentSimilarityThreshold());

        String content = readFileContent(filePath);
        return processAndStoreText(content, documentId, path, maxChunkSize, getFileName(filePath));
    }

    /**
     * Загрузить текстовую строку и сохранить чанки в базу
     * @param text текст для обработки
     * @param documentId идентификатор документа
     * @param path путь в базе данных
     * @param maxChunkSize максимальный размер чанка в символах
     * @param sourceName имя источника (для метаданных)
     * @return количество загруженных чанков
     */
    public int loadText(String text, String documentId, Object[] path, int maxChunkSize, String sourceName) throws Exception {
        System.out.println("Loading text from: " + sourceName);
        System.out.println("Using similarity threshold: " + getCurrentSimilarityThreshold());
        return processAndStoreText(text, documentId, path, maxChunkSize, sourceName);
    }

    /**
     * Загрузить все текстовые файлы из директории
     * @param directoryPath путь к директории
     * @param baseDocumentId базовый идентификатор документов
     * @param basePath базовый путь в базе данных
     * @param maxChunkSize максимальный размер чанка в символах
     * @param fileExtensions расширения файлов для обработки (например, [".txt", ".md"])
     * @return общее количество загруженных чанков
     */
    public int loadTextDirectory(String directoryPath, String baseDocumentId, Object[] basePath,
                                 int maxChunkSize, String[] fileExtensions) throws Exception {
        System.out.println("Loading text files from directory: " + directoryPath);
        System.out.println("Using similarity threshold: " + getCurrentSimilarityThreshold());

        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            throw new IllegalArgumentException("Directory does not exist: " + directoryPath);
        }

        int totalChunks = 0;
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && hasValidExtension(file, fileExtensions)) {
                    try {
                        String documentId = baseDocumentId + "_" + file.getName();
                        Object[] filePath = createFilePath(basePath, file.getName());

                        int chunks = loadTextFile(file.getAbsolutePath(), documentId, filePath, maxChunkSize);
                        totalChunks += chunks;
                        System.out.println("Processed " + file.getName() + " - " + chunks + " chunks");
                    } catch (Exception e) {
                        System.err.println("Error processing file " + file.getName() + ": " + e.getMessage());
                    }
                }
            }
        }

        return totalChunks;
    }

    /**
     * Обработать текст и сохранить чанки в базу данных
     */
    private int processAndStoreText(String text, String documentId, Object[] path,
                                    int maxChunkSize, String sourceName) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        System.out.println("Processing text (" + text.length() + " characters)...");
        System.out.println("Using similarity threshold: " + getCurrentSimilarityThreshold());

        // Разбиваем текст на семантические чанки
        List<Chunk> chunks = semanticChunker.semanticChunking(text, maxChunkSize);

        System.out.println("Created " + chunks.size() + " semantic chunks");

        // Сохраняем каждый чанк в базу данных
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            saveChunk(chunk, documentId, path, i, sourceName);
        }

        // Сохраняем метаданные о загрузке
        saveMetadata(documentId, path, sourceName, text.length(), chunks.size());

        // Сохраняем базу данных
        database.saveDatabase();

        System.out.println("Successfully stored " + chunks.size() + " chunks for " + documentId);
        return chunks.size();
    }

    /**
     * Сохранить чанк в базу данных
     */
    private void saveChunk(Chunk chunk, String documentId, Object[] basePath,
                           int chunkIndex, String sourceName) throws Exception {
        Object[] chunkPath = createChunkPath(basePath, chunkIndex);

        // Сохраняем текст с чанкингом в векторную базу
        database.storeTextWithChunking(
                chunk.getText(),
                documentId + "_chunk_" + chunkIndex,
                chunkPath
        );
    }

    /**
     * Сохранить метаданные о загрузке
     */
    private void saveMetadata(String documentId, Object[] path, String sourceName,
                              int textLength, int chunkCount) {
        // Можно сохранить метаданные в отдельный узел базы данных
        Object[] metadataPath = createMetadataPath(path);
        // Реализация сохранения метаданных зависит от структуры вашей базы данных
    }

    /**
     * Создать путь для чанка
     */
    private Object[] createChunkPath(Object[] basePath, int chunkIndex) {
        Object[] chunkPath = new Object[basePath.length + 2];
        System.arraycopy(basePath, 0, chunkPath, 0, basePath.length);
        chunkPath[basePath.length] = "chunks";
        chunkPath[basePath.length + 1] = "chunk_" + chunkIndex;
        return chunkPath;
    }

    /**
     * Создать путь для метаданных
     */
    private Object[] createMetadataPath(Object[] basePath) {
        Object[] metadataPath = new Object[basePath.length + 1];
        System.arraycopy(basePath, 0, metadataPath, 0, basePath.length);
        metadataPath[basePath.length] = "metadata";
        return metadataPath;
    }

    /**
     * Создать путь для файла
     */
    private Object[] createFilePath(Object[] basePath, String fileName) {
        Object[] filePath = new Object[basePath.length + 1];
        System.arraycopy(basePath, 0, filePath, 0, basePath.length);
        filePath[basePath.length] = fileName;
        return filePath;
    }

    /**
     * Прочитать содержимое файла
     */
    private String readFileContent(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        byte[] bytes = Files.readAllBytes(path);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Получить имя файла из пути
     */
    private String getFileName(String filePath) {
        return Paths.get(filePath).getFileName().toString();
    }

    /**
     * Проверить расширение файла
     */
    private boolean hasValidExtension(File file, String[] extensions) {
        if (extensions == null || extensions.length == 0) {
            return true; // Если расширения не указаны, принимаем все файлы
        }

        String fileName = file.getName().toLowerCase();
        for (String ext : extensions) {
            if (fileName.endsWith(ext.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Получить статистику по загруженным знаниям
     */
    public void printKnowledgeStats() {
        try {
            System.out.println("=== Knowledge Statistics ===");
            System.out.println("Total vectors in database: " + database.getVectorCount());
            System.out.println("Total tree nodes in database: " + database.getTreeNodeCount());
            System.out.println("Current similarity threshold: " + getCurrentSimilarityThreshold());
            System.out.println("SemanticChunker config: " + getSemanticChunkerConfig());
        } catch (Exception e) {
            System.err.println("Error getting knowledge stats: " + e.getMessage());
        }
    }
}