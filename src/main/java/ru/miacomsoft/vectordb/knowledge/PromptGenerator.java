package ru.miacomsoft.vectordb.knowledge;

import ru.miacomsoft.vectordb.core.SemanticChunker;
import ru.miacomsoft.vectordb.core.VectorDatabase;
import ru.miacomsoft.vectordb.core.VectorSearchResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Генератор промптов для AI с использованием семантического поиска
 * Адаптирован для работы с VectorDatabase
 */
public class PromptGenerator {
    private final VectorDatabase vectorDB;
    private final SemanticChunker semanticChunker;

    public PromptGenerator(VectorDatabase vectorDB, KnowledgeConfig knowledgeConfig) {
        this.vectorDB = vectorDB;
        this.semanticChunker = new SemanticChunker(
                knowledgeConfig.getOllamaUrl(),
                "all-minilm:22m",
                knowledgeConfig.getSimilarityThreshold()
        );
    }

    public PromptGenerator(VectorDatabase vectorDB) {
        this.vectorDB = vectorDB;
        // Используем дефолтные настройки для SemanticChunker
        this.semanticChunker = new SemanticChunker(
                "http://localhost:11434",
                "all-minilm:22m",
                0.7
        );
    }

    /**
     * Создать контекстный промпт для AI на основе семантического поиска
     * @param aiQuery запрос пользователя для AI
     * @param maxResultsPerChunk максимальное количество результатов на чанк
     * @param similarityThreshold порог схожести для поиска
     * @return сформированный промпт с контекстом
     */
    public String createContextPrompt(String aiQuery, int maxResultsPerChunk, double similarityThreshold) throws Exception {
        System.out.println("Creating context prompt for query: " + aiQuery);

        // Разбиваем запрос на семантические чанки
        List<SemanticChunker.Chunk> queryChunks = splitQueryIntoChunks(aiQuery);
        System.out.println("Query split into " + queryChunks.size() + " semantic chunks");

        // Для каждого чанка находим похожие документы
        List<VectorSearchResult> allSimilarResults = new ArrayList<>();

        for (int i = 0; i < queryChunks.size(); i++) {
            SemanticChunker.Chunk chunk = queryChunks.get(i);
            System.out.println("Processing query chunk " + (i + 1) + ": " +
                    chunk.getText().substring(0, Math.min(50, chunk.getText().length())) + "...");

            // Ищем похожие документы во всей базе данных
            List<VectorSearchResult> results = vectorDB.similaritySearch(
                    chunk.getText(),
                    maxResultsPerChunk
            );

            if (results != null) {
                // Фильтруем по порогу схожести
                for (VectorSearchResult result : results) {
                    if (result.getSimilarity() >= similarityThreshold) {
                        allSimilarResults.add(result);
                    }
                }
            }
        }

        // Убираем дубликаты (по тексту)
        List<VectorSearchResult> uniqueResults = removeDuplicateResults(allSimilarResults);
        System.out.println("Found " + uniqueResults.size() + " unique relevant documents");

        // Формируем финальный промпт
        return buildFinalPrompt(aiQuery, uniqueResults);
    }

    /**
     * Создать контекстный промпт с настройками по умолчанию
     */
    public String createContextPrompt(String aiQuery) throws Exception {
        return createContextPrompt(aiQuery, 3, 0.7);
    }

    /**
     * Разбить запрос на семантические чанки
     */
    private List<SemanticChunker.Chunk> splitQueryIntoChunks(String query) throws Exception {
        // Для запросов используем меньший размер чанка
        return semanticChunker.semanticChunking(query, 200);
    }

    /**
     * Удалить дублирующиеся результаты поиска
     */
    private List<VectorSearchResult> removeDuplicateResults(List<VectorSearchResult> results) {
        List<VectorSearchResult> uniqueResults = new ArrayList<>();
        List<String> seenTexts = new ArrayList<>();

        for (VectorSearchResult result : results) {
            String text = extractTextFromResult(result);
            if (text != null && !text.trim().isEmpty() && !seenTexts.contains(text)) {
                seenTexts.add(text);
                uniqueResults.add(result);
            }
        }

        return uniqueResults;
    }

    /**
     * Извлечь текст из результата поиска
     */
    private String extractTextFromResult(VectorSearchResult result) {
        if (result.getVectorData() != null && result.getVectorData().getText() != null) {
            return result.getVectorData().getText();
        }
        return null;
    }

    /**
     * Построить финальный промпт с контекстом
     */
    private String buildFinalPrompt(String originalQuery, List<VectorSearchResult> contextResults) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("На основе следующего контекста ответь на вопрос пользователя.\n\n");
        prompt.append("=== КОНТЕКСТ ===\n");

        if (contextResults.isEmpty()) {
            prompt.append("Релевантный контекст не найден. Ответь на вопрос используя свои знания.\n");
        } else {
            // Сортируем результаты по схожести (от высшей к низшей)
            contextResults.sort((r1, r2) -> Double.compare(r2.getSimilarity(), r1.getSimilarity()));

            for (int i = 0; i < contextResults.size(); i++) {
                VectorSearchResult result = contextResults.get(i);
                prompt.append("\n[Документ ").append(i + 1);
                prompt.append(", схожесть: ").append(String.format("%.2f", result.getSimilarity())).append("]\n");
                prompt.append(extractTextFromResult(result)).append("\n");
            }
        }

        prompt.append("\n=== ВОПРОС ===\n");
        prompt.append(originalQuery).append("\n\n");
        prompt.append("=== ОТВЕТ ===\n");

        return prompt.toString();
    }

    /**
     * Создать промпт для генерации вопросов на основе контекста
     */
    public String createQuestionGenerationPrompt(String topic, int numQuestions, double similarityThreshold) throws Exception {
        System.out.println("Creating question generation prompt for topic: " + topic);

        // Находим релевантные документы по теме
        List<VectorSearchResult> relevantDocs = vectorDB.similaritySearch(topic, 5);

        // Фильтруем по порогу схожести
        List<VectorSearchResult> filteredDocs = new ArrayList<>();
        for (VectorSearchResult result : relevantDocs) {
            if (result.getSimilarity() >= similarityThreshold) {
                filteredDocs.add(result);
            }
        }

        List<VectorSearchResult> uniqueDocs = removeDuplicateResults(filteredDocs);
        System.out.println("Found " + uniqueDocs.size() + " relevant documents for question generation");

        return buildQuestionGenerationPrompt(topic, uniqueDocs, numQuestions);
    }

    /**
     * Построить промпт для генерации вопросов
     */
    private String buildQuestionGenerationPrompt(String topic, List<VectorSearchResult> contextDocs, int numQuestions) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("На основе следующего контекста сгенерируй ").append(numQuestions);
        prompt.append(" тестовых вопросов по теме: ").append(topic).append("\n\n");

        prompt.append("=== КОНТЕКСТ ===\n");
        if (contextDocs.isEmpty()) {
            prompt.append("Контекст не предоставлен. Сгенерируй общие вопросы по теме.\n");
        } else {
            for (int i = 0; i < contextDocs.size(); i++) {
                VectorSearchResult doc = contextDocs.get(i);
                prompt.append("\n[Документ ").append(i + 1).append("]\n");
                prompt.append(extractTextFromResult(doc)).append("\n");
            }
        }

        prompt.append("\n=== ТРЕБОВАНИЯ К ВОПРОСАМ ===\n");
        prompt.append("- Вопросы должны быть разного уровня сложности\n");
        prompt.append("- Вопросы должны охватывать разные аспекты темы\n");
        prompt.append("- Формат: нумерованный список вопросов\n");
        prompt.append("- Каждый вопрос должен быть четким и конкретным\n\n");

        prompt.append("=== СГЕНЕРИРОВАННЫЕ ВОПРОСЫ ===\n");

        return prompt.toString();
    }

    /**
     * Создать промпт для суммаризации контекста
     */
    public String createSummarizationPrompt(String focusTopic, int maxContextItems, double similarityThreshold) throws Exception {
        System.out.println("Creating summarization prompt for topic: " + focusTopic);

        // Находим релевантные документы
        List<VectorSearchResult> relevantDocs = vectorDB.similaritySearch(focusTopic, maxContextItems);

        // Фильтруем по порогу схожести
        List<VectorSearchResult> filteredDocs = new ArrayList<>();
        for (VectorSearchResult result : relevantDocs) {
            if (result.getSimilarity() >= similarityThreshold) {
                filteredDocs.add(result);
            }
        }

        List<VectorSearchResult> uniqueDocs = removeDuplicateResults(filteredDocs);
        System.out.println("Found " + uniqueDocs.size() + " documents for summarization");

        return buildSummarizationPrompt(focusTopic, uniqueDocs);
    }

    /**
     * Построить промпт для суммаризации
     */
    private String buildSummarizationPrompt(String topic, List<VectorSearchResult> contextDocs) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Суммаризируй следующую информацию по теме: ").append(topic).append("\n\n");

        prompt.append("=== ИСХОДНАЯ ИНФОРМАЦИЯ ===\n");
        if (contextDocs.isEmpty()) {
            prompt.append("Информация для суммаризации не предоставлена.\n");
        } else {
            for (int i = 0; i < contextDocs.size(); i++) {
                VectorSearchResult doc = contextDocs.get(i);
                prompt.append("\n[Документ ").append(i + 1).append("]\n");
                prompt.append(extractTextFromResult(doc)).append("\n");
            }
        }

        prompt.append("\n=== ТРЕБОВАНИЯ К СУММАРИЗАЦИИ ===\n");
        prompt.append("- Выдели основные идеи и ключевые моменты\n");
        prompt.append("- Сохрани важные детали и факты\n");
        prompt.append("- Используй четкий и структурированный формат\n");
        prompt.append("- Объем: 1-2 абзаца\n\n");

        prompt.append("=== СУММАРИЗАЦИЯ ===\n");

        return prompt.toString();
    }

    /**
     * Получить базовую статистику по используемым знаниям
     */
    public void printKnowledgeStats() {
        System.out.println("=== KNOWLEDGE BASE STATISTICS ===");
        System.out.println("Total vectors: " + vectorDB.getVectorCount());
        System.out.println("Total tree nodes: " + vectorDB.getTreeNodeCount());
    }
}