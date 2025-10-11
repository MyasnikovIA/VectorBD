package ru.miacomsoft.vectordb.knowledge;

import java.util.Map;
import java.util.HashMap;

/**
 * Класс для управления конфигурацией Knowledge функциональности
 */
public class KnowledgeConfig {
    private final String ollamaUrl;
    private final String model;
    private final double similarityThreshold;
    private final boolean saveHistory;
    private final boolean enabled;

    public KnowledgeConfig(String ollamaUrl, String model, double similarityThreshold,
                           boolean saveHistory, boolean enabled) {
        this.ollamaUrl = ollamaUrl;
        this.model = model;
        this.similarityThreshold = similarityThreshold;
        this.saveHistory = saveHistory;
        this.enabled = enabled;
    }

    /**
     * Проверить, включена ли функциональность Knowledge
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Получить URL Ollama для Knowledge
     */
    public String getOllamaUrl() {
        return ollamaUrl;
    }

    /**
     * Получить модель для Knowledge
     */
    public String getModel() {
        return model;
    }

    /**
     * Получить порог схожести для Knowledge
     */
    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    /**
     * Проверить, включено ли сохранение истории
     */
    public boolean isSaveHistoryEnabled() {
        return saveHistory;
    }

    /**
     * Получить все настройки Knowledge в виде Map
     */
    public Map<String, Object> getAllSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("enabled", isEnabled());
        settings.put("ollamaUrl", getOllamaUrl());
        settings.put("model", getModel());
        settings.put("similarityThreshold", getSimilarityThreshold());
        settings.put("saveHistory", isSaveHistoryEnabled());
        return settings;
    }

    /**
     * Вывести текущую конфигурацию
     */
    public void printConfig() {
        System.out.println("=== Knowledge Configuration ===");
        System.out.println("Enabled: " + isEnabled());
        if (isEnabled()) {
            System.out.println("Ollama URL: " + getOllamaUrl());
            System.out.println("Model: " + getModel());
            System.out.println("Similarity Threshold: " + getSimilarityThreshold());
            System.out.println("Save History: " + isSaveHistoryEnabled());
        }
        System.out.println("===============================");
    }

    /**
     * Проверить валидность конфигурации
     */
    public boolean isValid() {
        if (!isEnabled()) {
            return true; // Если отключено, конфигурация всегда валидна
        }

        // Проверяем обязательные параметры
        if (getOllamaUrl() == null || getOllamaUrl().trim().isEmpty()) {
            return false;
        }

        if (getModel() == null || getModel().trim().isEmpty()) {
            return false;
        }

        double threshold = getSimilarityThreshold();
        if (threshold < 0.0 || threshold > 1.0) {
            return false;
        }

        return true;
    }
}