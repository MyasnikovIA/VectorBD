package ru.miacomsoft.vectordb.demo;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.miacomsoft.vectordb.core.BinaryVectorDBJsonManager;
import ru.miacomsoft.vectordb.core.BinaryVectorData;
import ru.miacomsoft.vectordb.core.BinaryVectorDatabase;
import ru.miacomsoft.vectordb.core.SemanticChunker;

public class JsonManagerExample {
    public static void main(String[] args) throws Exception {
        // Инициализация базы данных
        SemanticChunker chunker = new SemanticChunker("http://localhost:11434", "all-minilm:22m", 0.7);
        BinaryVectorDatabase database = new BinaryVectorDatabase("./data", chunker);
        BinaryVectorDBJsonManager jsonManager = new BinaryVectorDBJsonManager(database);

        // Экспорт данных в JSON
        JSONArray allVectors = jsonManager.exportAllVectorDataToJson();
        System.out.println("Exported " + allVectors.length() + " vectors");

        JSONArray allNodes = jsonManager.exportAllTreeNodesToJson();
        System.out.println("Exported " + allNodes.length() + " tree nodes");

        // Сохранение всей базы в JSON файлы
        jsonManager.saveDatabaseToJsonFiles("./export");

        // Поиск и экспорт результатов
        JSONArray searchResults = jsonManager.searchAndExportToJson("машинное обучение", 5);
        System.out.println("Search results: " + searchResults.length());

        // Экспорт статистики
        JSONObject stats = jsonManager.exportDatabaseStatsToJson();
        System.out.println("Database stats: " + stats.toString(2));

        // Работа с отдельными объектами
        BinaryVectorData vectorData = database.findVectorData("some_id");
        if (vectorData != null) {
            // Получение JSONObject
            JSONObject json = jsonManager.vectorDataToJsonObject(vectorData);

            // Получение JSON строки
            String jsonString = jsonManager.vectorDataToJsonString(vectorData);

            // Восстановление из JSON строки
            BinaryVectorData restored = jsonManager.vectorDataFromJsonString(jsonString);
        }

        // Пакетный экспорт
        jsonManager.batchExportToJsonFile("./batch_export.json");

        database.close();
    }
}