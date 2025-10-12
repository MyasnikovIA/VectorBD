package ru.miacomsoft.vectordb.core;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SQLParser {
    private final BinaryVectorDatabase database;

    public SQLParser(BinaryVectorDatabase database) {
        this.database = database;
    }

    public List<JSONObject> execute(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new DatabaseException("SQL statement cannot be empty");
        }

        String normalizedSql = sql.trim().toLowerCase();

        try {
            if (normalizedSql.startsWith("select")) {
                return parseSelect(sql);
            } else if (normalizedSql.startsWith("insert")) {
                return parseInsert(sql);
            } else if (normalizedSql.startsWith("update")) {
                return parseUpdate(sql);
            } else if (normalizedSql.startsWith("delete")) {
                return parseDelete(sql);
            } else if (normalizedSql.startsWith("create index")) {
                parseCreateIndex(sql);
                return Collections.singletonList(new JSONObject().put("status", "INDEX_CREATED"));
            } else if (normalizedSql.startsWith("drop index")) {
                parseDropIndex(sql);
                return Collections.singletonList(new JSONObject().put("status", "INDEX_DROPPED"));
            } else if (normalizedSql.startsWith("create table")) {
                // Таблицы в VectorDB не поддерживаются, возвращаем успех для совместимости
                return Collections.singletonList(new JSONObject().put("status", "TABLE_CREATED"));
            } else if (normalizedSql.startsWith("drop table")) {
                // Таблицы в VectorDB не поддерживаются, возвращаем успех для совместимости
                return Collections.singletonList(new JSONObject().put("status", "TABLE_DROPPED"));
            } else {
                throw new DatabaseException("Unsupported SQL statement: " + sql);
            }
        } catch (Exception e) {
            throw new DatabaseException("Error executing SQL: " + sql, e);
        }
    }

    private List<JSONObject> parseSelect(String sql) {
        // Упрощенный парсинг SELECT запросов
        Pattern pattern = Pattern.compile(
                "select\\s+(.*?)(?:\\s+from\\s+(\\w+))?(?:\\s+where\\s+(.*?))?(?:\\s+order by\\s+(.*?))?(?:\\s+limit\\s+(\\d+))?",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(sql);
        if (!matcher.find()) {
            throw new DatabaseException("Invalid SELECT statement: " + sql);
        }

        String columnsPart = matcher.group(1);
        String tableName = matcher.group(2); // Может быть null
        String whereClause = matcher.group(3);
        String orderBy = matcher.group(4);
        String limitStr = matcher.group(5);

        // Получаем все векторные данные
        List<BinaryVectorData> allData = database.findAllVectorData();

        // Конвертируем в JSONObject для совместимости
        List<JSONObject> results = allData.stream()
                .map(this::vectorDataToJson)
                .collect(Collectors.toList());

        // Применяем WHERE условие
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            results = filterResults(results, whereClause);
        }

        // Выбираем только нужные колонки
        if (!columnsPart.equals("*")) {
            results = projectColumns(results, columnsPart);
        }

        // Применяем ORDER BY
        if (orderBy != null && !orderBy.trim().isEmpty()) {
            results = sortResults(results, orderBy);
        }

        // Применяем LIMIT
        if (limitStr != null) {
            int limit = Integer.parseInt(limitStr);
            results = results.stream().limit(limit).collect(Collectors.toList());
        }

        return results;
    }

    private List<JSONObject> parseInsert(String sql) {
        Pattern pattern = Pattern.compile(
                "insert\\s+into\\s+(\\w+)\\s*\\(([^)]+)\\)\\s*values\\s*\\(([^)]+)\\)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(sql);
        if (!matcher.find()) {
            throw new DatabaseException("Invalid INSERT statement: " + sql);
        }

        String tableName = matcher.group(1);
        String columnsPart = matcher.group(2);
        String valuesPart = matcher.group(3);

        String[] columns = Arrays.stream(columnsPart.split(","))
                .map(String::trim)
                .toArray(String[]::new);

        String[] values = parseValues(valuesPart);

        if (columns.length != values.length) {
            throw new DatabaseException("Column count doesn't match value count");
        }

        JSONObject record = new JSONObject();
        for (int i = 0; i < columns.length; i++) {
            record.put(columns[i], parseValue(values[i]));
        }

         // Создаем BinaryVectorData
        BinaryVectorData vectorData = new BinaryVectorData();
        vectorData.setId(tableName + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8));
        vectorData.setText(record.toString());
        vectorData.setOriginalData(record.toString()); // Теперь этот метод будет работать
        vectorData.setNodePath("/" + tableName);
        vectorData.setDocumentId(tableName);
        vectorData.setChunkIndex(0); // Теперь этот метод будет работать

        // Сохраняем в базу данных
        database.storeVectorData(vectorData);

        return Collections.singletonList(new JSONObject()
                .put("status", "INSERTED")
                .put("id", vectorData.getId())
                .put("record", record));
    }

    private List<JSONObject> parseUpdate(String sql) {
        Pattern pattern = Pattern.compile(
                "update\\s+(\\w+)\\s+set\\s+(.*?)(?:\\s+where\\s+(.*?))?",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(sql);
        if (!matcher.find()) {
            throw new DatabaseException("Invalid UPDATE statement: " + sql);
        }

        String tableName = matcher.group(1);
        String setClause = matcher.group(2);
        String whereClause = matcher.group(3);

        // Получаем все векторные данные
        List<BinaryVectorData> allData = database.findAllVectorData();

        // Конвертируем в JSONObject для фильтрации
        List<JSONObject> allRecords = allData.stream()
                .map(this::vectorDataToJson)
                .collect(Collectors.toList());

        // Применяем WHERE условие для поиска записей для обновления
        List<JSONObject> recordsToUpdate = whereClause != null ?
                filterResults(allRecords, whereClause) : allRecords;

        // Парсим SET clause
        Map<String, Object> updates = parseSetClause(setClause);

        List<JSONObject> updatedRecords = new ArrayList<>();
        for (JSONObject record : recordsToUpdate) {
            // Находим ID записи
            String id = record.optString("id", null);
            if (id != null) {
                // Обновляем запись
                JSONObject updatedRecord = new JSONObject(record.toString());
                updates.forEach(updatedRecord::put);

                // Обновляем в базе данных
                BinaryVectorData existingData = database.findVectorData(id);
                if (existingData != null) {
                    existingData.setText(updatedRecord.toString());
                    existingData.setOriginalData(updatedRecord.toString());
                    database.storeVectorData(existingData);
                    updatedRecords.add(updatedRecord);
                }
            }
        }

        return updatedRecords;
    }

    private List<JSONObject> parseDelete(String sql) {
        Pattern pattern = Pattern.compile(
                "delete\\s+from\\s+(\\w+)(?:\\s+where\\s+(.*?))?",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(sql);
        if (!matcher.find()) {
            throw new DatabaseException("Invalid DELETE statement: " + sql);
        }

        String tableName = matcher.group(1);
        String whereClause = matcher.group(2);

        // Получаем все векторные данные
        List<BinaryVectorData> allData = database.findAllVectorData();

        // Конвертируем в JSONObject для фильтрации
        List<JSONObject> allRecords = allData.stream()
                .map(this::vectorDataToJson)
                .collect(Collectors.toList());

        // Применяем WHERE условие для поиска записей для удаления
        List<JSONObject> recordsToDelete = whereClause != null ?
                filterResults(allRecords, whereClause) : allRecords;

        List<JSONObject> deletedRecords = new ArrayList<>();
        for (JSONObject record : recordsToDelete) {
            String id = record.optString("id", null);
            if (id != null) {
                database.removeVectorData(id);
                deletedRecords.add(record);
            }
        }

        return deletedRecords;
    }

    private void parseCreateIndex(String sql) {
        Pattern pattern = Pattern.compile(
                "create\\s+index\\s+(\\w+)\\s+on\\s+(\\w+)\\s*\\(([^)]+)\\)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(sql);
        if (!matcher.find()) {
            throw new DatabaseException("Invalid CREATE INDEX statement: " + sql);
        }

        String indexName = matcher.group(1);
        String tableName = matcher.group(2);
        String columnName = matcher.group(3).trim();

        // Создаем индекс в базе данных
        database.createIndex(indexName, columnName);
    }

    private void parseDropIndex(String sql) {
        Pattern pattern = Pattern.compile(
                "drop\\s+index\\s+(\\w+)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(sql);
        if (!matcher.find()) {
            throw new DatabaseException("Invalid DROP INDEX statement: " + sql);
        }

        String indexName = matcher.group(1);

        // Удаляем индекс из базы данных
        database.dropIndex(indexName);
    }

    // Вспомогательные методы

    private List<JSONObject> filterResults(List<JSONObject> results, String whereClause) {
        return results.stream()
                .filter(record -> evaluateWhereCondition(record, whereClause))
                .collect(Collectors.toList());
    }

    private boolean evaluateWhereCondition(JSONObject record, String whereClause) {
        // Упрощенная оценка условий WHERE
        String[] conditions = whereClause.split("\\s+and\\s+|\\s+or\\s+", Pattern.CASE_INSENSITIVE);

        boolean finalResult = true;

        for (String condition : conditions) {
            condition = condition.trim();
            boolean conditionResult = false;

            // Обработка простых условий сравнения
            if (condition.contains("=")) {
                String[] parts = condition.split("=");
                if (parts.length == 2) {
                    String field = parts[0].trim();
                    String value = parts[1].trim().replace("'", "").replace("\"", "");

                    if (record.has(field) && record.get(field).toString().equals(value)) {
                        conditionResult = true;
                    }
                }
            } else if (condition.contains(">")) {
                String[] parts = condition.split(">");
                if (parts.length == 2) {
                    String field = parts[0].trim();
                    String valueStr = parts[1].trim().replace("'", "").replace("\"", "");

                    if (record.has(field)) {
                        Object fieldValue = record.get(field);
                        if (fieldValue instanceof Number) {
                            double fieldNum = ((Number) fieldValue).doubleValue();
                            double valueNum = Double.parseDouble(valueStr);
                            conditionResult = fieldNum > valueNum;
                        }
                    }
                }
            } else if (condition.contains("<")) {
                String[] parts = condition.split("<");
                if (parts.length == 2) {
                    String field = parts[0].trim();
                    String valueStr = parts[1].trim().replace("'", "").replace("\"", "");

                    if (record.has(field)) {
                        Object fieldValue = record.get(field);
                        if (fieldValue instanceof Number) {
                            double fieldNum = ((Number) fieldValue).doubleValue();
                            double valueNum = Double.parseDouble(valueStr);
                            conditionResult = fieldNum < valueNum;
                        }
                    }
                }
            } else if (condition.contains(" like ")) {
                String[] parts = condition.split(" like ", Pattern.CASE_INSENSITIVE);
                if (parts.length == 2) {
                    String field = parts[0].trim();
                    String pattern = parts[1].trim().replace("'", "").replace("\"", "").replace("%", ".*");

                    if (record.has(field) && record.get(field).toString().matches(pattern)) {
                        conditionResult = true;
                    }
                }
            }

            // Обработка логических операторов (упрощенная)
            if (condition.toLowerCase().contains(" or ")) {
                finalResult = finalResult || conditionResult;
            } else {
                finalResult = finalResult && conditionResult;
            }
        }

        return finalResult;
    }

    private List<JSONObject> projectColumns(List<JSONObject> results, String columnsPart) {
        String[] columns = Arrays.stream(columnsPart.split(","))
                .map(String::trim)
                .toArray(String[]::new);

        return results.stream()
                .map(record -> {
                    JSONObject projected = new JSONObject();
                    for (String column : columns) {
                        if (record.has(column)) {
                            projected.put(column, record.get(column));
                        }
                    }
                    return projected;
                })
                .collect(Collectors.toList());
    }

    private List<JSONObject> sortResults(List<JSONObject> results, String orderBy) {
        String[] orderParts = orderBy.split("\\s+");
        String field = orderParts[0].trim();
        boolean ascending = true;

        if (orderParts.length > 1 && orderParts[1].equalsIgnoreCase("desc")) {
            ascending = false;
        }

        final boolean finalAscending = ascending;
        results.sort((a, b) -> {
            Object valA = a.has(field) ? a.get(field) : null;
            Object valB = b.has(field) ? b.get(field) : null;

            if (valA == null && valB == null) return 0;
            if (valA == null) return finalAscending ? -1 : 1;
            if (valB == null) return finalAscending ? 1 : -1;

            int comparison = valA.toString().compareTo(valB.toString());
            return finalAscending ? comparison : -comparison;
        });

        return results;
    }

    private String[] parseValues(String valuesPart) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : valuesPart.toCharArray()) {
            if (c == '\'' || c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            values.add(current.toString().trim());
        }

        return values.toArray(new String[0]);
    }

    private Object parseValue(String value) {
        value = value.trim();

        if ((value.startsWith("'") && value.endsWith("'")) ||
                (value.startsWith("\"") && value.endsWith("\""))) {
            return value.substring(1, value.length() - 1);
        } else if (value.equalsIgnoreCase("true")) {
            return true;
        } else if (value.equalsIgnoreCase("false")) {
            return false;
        } else if (value.equalsIgnoreCase("null")) {
            return JSONObject.NULL;
        } else {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e1) {
                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException e2) {
                    return value;
                }
            }
        }
    }

    private Map<String, Object> parseSetClause(String setClause) {
        Map<String, Object> updates = new HashMap<>();
        String[] assignments = setClause.split(",");

        for (String assignment : assignments) {
            String[] parts = assignment.split("=");
            if (parts.length == 2) {
                String field = parts[0].trim();
                Object value = parseValue(parts[1].trim());
                updates.put(field, value);
            }
        }

        return updates;
    }

    private JSONObject vectorDataToJson(BinaryVectorData vectorData) {
        JSONObject json = new JSONObject();
        json.put("id", vectorData.getId());
        json.put("text", vectorData.getText());
        json.put("nodePath", vectorData.getNodePath());
        json.put("documentId", vectorData.getDocumentId());
        json.put("chunkIndex", vectorData.getChunkIndex()); // Используем метод getChunkIndex()
        json.put("timestamp", vectorData.getTimestamp());
        json.put("metadata", vectorData.getMetadata()); // Добавляем metadata

        // Пытаемся разобрать текст как JSON, если это возможно
        try {
            JSONObject parsedData = new JSONObject(vectorData.getText());
            for (String key : parsedData.keySet()) {
                json.put(key, parsedData.get(key));
            }
        } catch (Exception e) {
            // Если не JSON, просто используем текстовое поле
            json.put("content", vectorData.getText());
        }

        return json;
    }

    // Метод для выполнения семантического поиска через SQL-подобный интерфейс
    public List<JSONObject> semanticSearch(String query, int limit) throws Exception {
        List<VectorSearchResult> results = database.similaritySearch(query, limit);
        return results.stream()
                .map(result -> vectorDataToJson(result.getVectorData()))
                .collect(Collectors.toList());
    }

    // Метод для выполнения гибридного поиска через SQL-подобный интерфейс
    public List<JSONObject> hybridSearch(String query, int limit) throws Exception {
        // В VectorDB гибридный поиск может быть реализован как комбинация семантического и точного поиска
        List<VectorSearchResult> semanticResults = database.similaritySearch(query, limit);
        List<BinaryVectorData> exactResults = database.exactSearch(query);

        Set<String> seenIds = new HashSet<>();
        List<JSONObject> results = new ArrayList<>();

        // Добавляем семантические результаты
        for (VectorSearchResult result : semanticResults) {
            if (seenIds.add(result.getVectorData().getId())) {
                results.add(vectorDataToJson(result.getVectorData()));
            }
        }

        // Добавляем точные результаты (если еще не добавлены)
        for (BinaryVectorData data : exactResults) {
            if (seenIds.add(data.getId())) {
                results.add(vectorDataToJson(data));
            }
        }

        return results.stream().limit(limit).collect(Collectors.toList());
    }

    // Вспомогательный класс для исключений
    public static class DatabaseException extends RuntimeException {
        public DatabaseException(String message) {
            super(message);
        }

        public DatabaseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}