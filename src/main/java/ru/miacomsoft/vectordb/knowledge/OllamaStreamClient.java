package ru.miacomsoft.vectordb.knowledge;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.Scanner;

/**
 * Клиент для работы с Ollama API с поддержкой потоковой генерации
 */
public class OllamaStreamClient {
    private final String ollamaUrl;
    private final HttpClient httpClient;

    public OllamaStreamClient() {
        this("http://localhost:11434");
    }

    public OllamaStreamClient(String ollamaUrl) {
        this.ollamaUrl = ollamaUrl.endsWith("/") ?
                ollamaUrl.substring(0, ollamaUrl.length() - 1) : ollamaUrl;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Получить URL Ollama сервера
     */
    public String getOllamaUrl() {
        return this.ollamaUrl;
    }

    /**
     * Проверить доступность сервера Ollama
     */
    public boolean isServerAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaUrl + "/api/tags"))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Получить список доступных моделей
     */
    public List<String> getAvailableModels() {
        List<String> models = new ArrayList<>();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaUrl + "/api/tags"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                // Простой парсинг JSON без библиотек
                models = parseModelsFromJson(responseBody);
            }
        } catch (Exception e) {
            System.err.println("Error getting models: " + e.getMessage());
        }
        return models;
    }

    /**
     * Простой парсинг JSON для извлечения моделей
     */
    private List<String> parseModelsFromJson(String json) {
        List<String> models = new ArrayList<>();
        try {
            // Ищем блок "models" в JSON
            int modelsIndex = json.indexOf("\"models\"");
            if (modelsIndex == -1) return models;

            // Ищем начало массива моделей
            int arrayStart = json.indexOf('[', modelsIndex);
            if (arrayStart == -1) return models;

            int arrayEnd = json.indexOf(']', arrayStart);
            if (arrayEnd == -1) return models;

            String modelsArray = json.substring(arrayStart, arrayEnd + 1);

            // Извлекаем имена моделей
            int pos = 0;
            while ((pos = modelsArray.indexOf("\"name\"", pos)) != -1) {
                int nameStart = modelsArray.indexOf('"', pos + 6) + 1;
                int nameEnd = modelsArray.indexOf('"', nameStart);
                if (nameStart > 0 && nameEnd > nameStart) {
                    String modelName = modelsArray.substring(nameStart, nameEnd);
                    models.add(modelName);
                }
                pos = nameEnd + 1;
            }
        } catch (Exception e) {
            System.err.println("Error parsing models JSON: " + e.getMessage());
        }
        return models;
    }

    /**
     * Потоковая генерация ответа
     */
    public Iterator<String> generateResponseStream(String model, String prompt, boolean stream) {
        return new OllamaResponseIterator(model, prompt, stream);
    }

    /**
     * Генерация ответа с callback (для обратной совместимости)
     */
    public void generateStreamResponse(String model, String prompt, Consumer<String> tokenConsumer) {
        try {
            Iterator<String> stream = generateResponseStream(model, prompt, true);
            while (stream.hasNext()) {
                String token = stream.next();
                if (token != null && !token.isEmpty()) {
                    tokenConsumer.accept(token);
                }
            }
        } catch (Exception e) {
            System.err.println("Error in stream response: " + e.getMessage());
        }
    }

    /**
     * Итератор для потокового ответа от Ollama
     */
    private class OllamaResponseIterator implements Iterator<String> {
        private final String model;
        private final String prompt;
        private final boolean stream;
        private Scanner scanner;
        private boolean initialized = false;

        public OllamaResponseIterator(String model, String prompt, boolean stream) {
            this.model = model;
            this.prompt = prompt;
            this.stream = stream;
        }

        private void initialize() {
            try {
                // Формируем JSON вручную
                String requestBody = String.format(
                        "{\"model\":\"%s\",\"prompt\":\"%s\",\"stream\":%b}",
                        escapeJson(model), escapeJson(prompt), stream
                );

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ollamaUrl + "/api/generate"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<java.io.InputStream> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofInputStream());

                this.scanner = new Scanner(response.body());
                this.initialized = true;

            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize stream: " + e.getMessage(), e);
            }
        }

        @Override
        public boolean hasNext() {
            if (!initialized) {
                initialize();
            }
            return scanner.hasNextLine();
        }

        @Override
        public String next() {
            if (!initialized) {
                initialize();
            }
            try {
                String line = scanner.nextLine();
                if (line != null && !line.trim().isEmpty()) {
                    // Простой парсинг JSON строки для извлечения response
                    return parseResponseFromJson(line);
                }
                return "";
            } catch (Exception e) {
                return "";
            }
        }

        private String parseResponseFromJson(String jsonLine) {
            try {
                // Ищем поле "response" в JSON
                int responseIndex = jsonLine.indexOf("\"response\"");
                if (responseIndex == -1) return "";

                int valueStart = jsonLine.indexOf('"', responseIndex + 10) + 1;
                int valueEnd = jsonLine.indexOf('"', valueStart);
                if (valueStart > 0 && valueEnd > valueStart) {
                    return jsonLine.substring(valueStart, valueEnd);
                }
            } catch (Exception e) {
                // Игнорируем ошибки парсинга
            }
            return "";
        }
    }

    /**
     * Экранирование строк для JSON
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}