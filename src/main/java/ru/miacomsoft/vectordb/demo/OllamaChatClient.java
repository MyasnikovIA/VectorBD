package ru.miacomsoft.vectordb.demo;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Клиент для OllamaChatDemo сервера
 * 
 * # RAGSQL Client
 * java ru.miacomsoft.vectordb.demo.RAGSQLClient
 *
 * # RAGWithEmbeddingSQL Client
 * java ru.miacomsoft.vectordb.demo.RAGWithEmbeddingSQLClient
 *
 * # OllamaChat Client
 * java ru.miacomsoft.vectordb.demo.OllamaChatClient
 *
 */
public class OllamaChatClient {
    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public OllamaChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Подключение к чат-серверу
     */
    public boolean connect() {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("✅ Connected to OllamaChat Server at " + host + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("❌ Chat Connection failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Отключение от сервера
     */
    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            System.out.println("🔌 Disconnected from chat server");
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }

    /**
     * Отправка команды и получение ответа
     */
    public JSONObject sendCommand(JSONObject command) {
        try {
            out.println(command.toString());
            String response = in.readLine();
            return new JSONObject(response);
        } catch (IOException e) {
            JSONObject error = new JSONObject();
            error.put("status", "error");
            error.put("message", "Chat Communication error: " + e.getMessage());
            return error;
        } catch (Exception e) {
            JSONObject error = new JSONObject();
            error.put("status", "error");
            error.put("message", "Chat Response parsing error: " + e.getMessage());
            return error;
        }
    }

    /**
     * Проверка соединения
     */
    public JSONObject ping() {
        JSONObject command = new JSONObject();
        command.put("command", "ping");
        return sendCommand(command);
    }

    /**
     * Простой чат
     */
    public JSONObject chatMessage(String message) {
        JSONObject command = new JSONObject();
        command.put("command", "chat_message");
        command.put("message", message);

        return sendCommand(command);
    }

    /**
     * Чат с использованием знаний
     */
    public JSONObject chatWithKnowledge(String message, int maxContext, double threshold) {
        JSONObject command = new JSONObject();
        command.put("command", "chat_with_knowledge");
        command.put("message", message);
        command.put("max_context", maxContext);
        command.put("threshold", threshold);

        return sendCommand(command);
    }

    /**
     * Потоковый чат (симулированный)
     */
    public JSONObject streamChat(String message, boolean useKnowledge) {
        JSONObject command = new JSONObject();
        command.put("command", "stream_chat");
        command.put("message", message);
        command.put("use_knowledge", useKnowledge);

        return sendCommand(command);
    }

    /**
     * Добавление знаний
     */
    public JSONObject addKnowledge(String text, String documentId, String category) {
        JSONObject command = new JSONObject();
        command.put("command", "add_knowledge");
        command.put("text", text);
        command.put("document_id", documentId);
        command.put("category", category);

        return sendCommand(command);
    }

    /**
     * Поиск знаний
     */
    public JSONObject searchKnowledge(String query, int maxResults) {
        JSONObject command = new JSONObject();
        command.put("command", "search_knowledge");
        command.put("query", query);
        command.put("max_results", maxResults);

        return sendCommand(command);
    }

    /**
     * Получить статистику чата
     */
    public JSONObject getChatStats() {
        JSONObject command = new JSONObject();
        command.put("command", "get_chat_stats");

        return sendCommand(command);
    }

    /**
     * Тестирование подключений
     */
    public JSONObject testConnections() {
        JSONObject command = new JSONObject();
        command.put("command", "test_connections");

        return sendCommand(command);
    }

    /**
     * Список доступных моделей
     */
    public JSONObject listModels() {
        JSONObject command = new JSONObject();
        command.put("command", "list_models");

        return sendCommand(command);
    }

    /**
     * Установка модели
     */
    public JSONObject setModel(String model) {
        JSONObject command = new JSONObject();
        command.put("command", "set_model");
        command.put("model", model);

        return sendCommand(command);
    }

    /**
     * Демонстрация работы клиента
     */
    public static void main(String[] args) {
        OllamaChatClient client = new OllamaChatClient("localhost", 9092);

        if (!client.connect()) {
            return;
        }

        try {
            demonstrateChatClient(client);

        } finally {
            client.disconnect();
        }
    }

    /**
     * Демонстрация возможностей чат-клиента
     */
    private static void demonstrateChatClient(OllamaChatClient client) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n=== OllamaChat Client Demo ===");
        System.out.println("Interactive AI chat with knowledge base");

        while (true) {
            System.out.println("\nAvailable Chat commands:");
            System.out.println("1.  Ping server");
            System.out.println("2.  Simple chat");
            System.out.println("3.  Knowledge-enhanced chat");
            System.out.println("4.  Stream chat (simulated)");
            System.out.println("5.  Add knowledge");
            System.out.println("6.  Search knowledge");
            System.out.println("7.  Get chat stats");
            System.out.println("8.  Test connections");
            System.out.println("9.  List models");
            System.out.println("10. Set model");
            System.out.println("11. Interactive chat mode");
            System.out.println("12. Knowledge-enhanced chat mode");
            System.out.println("0.  Exit");
            System.out.print("Choose command: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    demoPing(client);
                    break;
                case "2":
                    demoSimpleChat(client, scanner);
                    break;
                case "3":
                    demoKnowledgeChat(client, scanner);
                    break;
                case "4":
                    demoStreamChat(client, scanner);
                    break;
                case "5":
                    demoAddKnowledge(client, scanner);
                    break;
                case "6":
                    demoSearchKnowledge(client, scanner);
                    break;
                case "7":
                    demoGetChatStats(client);
                    break;
                case "8":
                    demoTestConnections(client);
                    break;
                case "9":
                    demoListModels(client);
                    break;
                case "10":
                    demoSetModel(client, scanner);
                    break;
                case "11":
                    startInteractiveChat(client, scanner, false);
                    break;
                case "12":
                    startInteractiveChat(client, scanner, true);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid choice");
            }
        }
    }

    private static void demoPing(OllamaChatClient client) {
        JSONObject response = client.ping();
        printChatResponse(response);
    }

    private static void demoSimpleChat(OllamaChatClient client, Scanner scanner) {
        System.out.print("Enter your message: ");
        String message = scanner.nextLine();

        JSONObject response = client.chatMessage(message);
        printChatResponse(response);
    }

    private static void demoKnowledgeChat(OllamaChatClient client, Scanner scanner) {
        System.out.print("Enter your message: ");
        String message = scanner.nextLine();

        System.out.print("Enter max context facts (default 3): ");
        String contextInput = scanner.nextLine();
        int maxContext = contextInput.isEmpty() ? 3 : Integer.parseInt(contextInput);

        System.out.print("Enter similarity threshold (default 0.7): ");
        String thresholdInput = scanner.nextLine();
        double threshold = thresholdInput.isEmpty() ? 0.7 : Double.parseDouble(thresholdInput);

        JSONObject response = client.chatWithKnowledge(message, maxContext, threshold);
        printChatResponse(response);
    }

    private static void demoStreamChat(OllamaChatClient client, Scanner scanner) {
        System.out.print("Enter your message: ");
        String message = scanner.nextLine();

        System.out.print("Use knowledge? (y/n, default y): ");
        String useKnowledgeInput = scanner.nextLine();
        boolean useKnowledge = !useKnowledgeInput.equalsIgnoreCase("n");

        JSONObject response = client.streamChat(message, useKnowledge);
        printChatResponse(response);
    }

    private static void demoAddKnowledge(OllamaChatClient client, Scanner scanner) {
        System.out.print("Enter knowledge text: ");
        String text = scanner.nextLine();

        System.out.print("Enter document ID (optional): ");
        String docId = scanner.nextLine();

        System.out.print("Enter category (optional): ");
        String category = scanner.nextLine();

        JSONObject response = client.addKnowledge(text,
                docId.isEmpty() ? null : docId,
                category.isEmpty() ? null : category);
        printChatResponse(response);
    }

    private static void demoSearchKnowledge(OllamaChatClient client, Scanner scanner) {
        System.out.print("Enter search query: ");
        String query = scanner.nextLine();

        System.out.print("Enter max results (default 5): ");
        String maxInput = scanner.nextLine();
        int maxResults = maxInput.isEmpty() ? 5 : Integer.parseInt(maxInput);

        JSONObject response = client.searchKnowledge(query, maxResults);
        printChatResponse(response);
    }

    private static void demoGetChatStats(OllamaChatClient client) {
        JSONObject response = client.getChatStats();
        printChatResponse(response);
    }

    private static void demoTestConnections(OllamaChatClient client) {
        JSONObject response = client.testConnections();
        printChatResponse(response);
    }

    private static void demoListModels(OllamaChatClient client) {
        JSONObject response = client.listModels();
        printChatResponse(response);
    }

    private static void demoSetModel(OllamaChatClient client, Scanner scanner) {
        System.out.print("Enter model name: ");
        String model = scanner.nextLine();

        JSONObject response = client.setModel(model);
        printChatResponse(response);
    }

    private static void startInteractiveChat(OllamaChatClient client, Scanner scanner, boolean useKnowledge) {
        String mode = useKnowledge ? "Knowledge-Enhanced" : "Simple";
        System.out.println("\n💬 " + mode + " Interactive Chat Mode");
        System.out.println("Type your messages (or 'quit' to exit):");

        while (true) {
            System.out.print("\nYou: ");
            String message = scanner.nextLine().trim();

            if (message.equalsIgnoreCase("quit") || message.equalsIgnoreCase("exit")) {
                break;
            }

            if (message.isEmpty()) {
                continue;
            }

            System.out.print("AI: ");
            JSONObject response;

            if (useKnowledge) {
                response = client.chatWithKnowledge(message, 3, 0.7);
            } else {
                response = client.chatMessage(message);
            }

            if (response.getString("status").equals("success")) {
                String aiResponse = response.getString("ai_response");

                // Эмуляция потокового вывода
                String[] words = aiResponse.split(" ");
                for (String word : words) {
                    System.out.print(word + " ");
                    try {
                        Thread.sleep(50); // Небольшая задержка для эффекта потоковости
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                System.out.println();

                // Дополнительная информация для knowledge-enhanced чата
                if (useKnowledge && response.has("context_facts")) {
                    int factCount = response.getInt("total_facts");
                    System.out.printf("📚 [Used %d knowledge facts]\n", factCount);
                }
            } else {
                System.out.println("❌ Error: " + response.getString("message"));
            }
        }
    }

    /**
     * Красивая печать чат-ответа
     */
    private static void printChatResponse(JSONObject response) {
        System.out.println("\n💬 Chat Response:");
        System.out.println("Status: " + response.optString("status", "unknown"));

        if (response.has("message")) {
            System.out.println("Message: " + response.getString("message"));
        }

        if (response.has("ai_response")) {
            System.out.println("\n🤖 AI Response:");
            System.out.println("--------------");
            System.out.println(response.getString("ai_response"));
        }

        if (response.has("user_message")) {
            System.out.println("\n👤 Your Message: " + response.getString("user_message"));
        }

        if (response.has("response_type")) {
            System.out.println("🔧 Response Type: " + response.getString("response_type"));
        }

        if (response.has("context_facts")) {
            JSONArray facts = response.getJSONArray("context_facts");
            System.out.println("\n📚 Context Facts Used: " + facts.length());

            for (int i = 0; i < Math.min(3, facts.length()); i++) {
                System.out.printf("  %d. %s\n", i + 1,
                        facts.getString(i).substring(0, Math.min(80, facts.getString(i).length())) + "...");
            }
        }

        if (response.has("results")) {
            JSONArray results = response.getJSONArray("results");
            System.out.println("\n🔍 Knowledge Search Results: " + results.length());

            for (int i = 0; i < Math.min(3, results.length()); i++) {
                JSONObject result = results.getJSONObject(i);
                System.out.printf("  %d. ", i + 1);

                if (result.has("text")) {
                    String text = result.getString("text");
                    System.out.println(text.substring(0, Math.min(70, text.length())) + "...");
                }

                if (result.has("similarity")) {
                    System.out.printf("    [Similarity: %.3f]\n", result.getDouble("similarity"));
                }
            }
        }

        if (response.has("database_stats")) {
            JSONObject stats = response.getJSONObject("database_stats");
            System.out.println("\n📊 Database Statistics:");
            for (String key : stats.keySet()) {
                System.out.printf("  - %s: %s\n", key, stats.get(key));
            }
        }

        if (response.has("connection_tests")) {
            JSONObject tests = response.getJSONObject("connection_tests");
            System.out.println("\n🔌 Connection Tests:");
            for (String key : tests.keySet()) {
                String status = tests.getBoolean(key) ? "✅" : "❌";
                System.out.printf("  %s %s: %s\n", status, key, tests.getBoolean(key));
            }
        }

        if (response.has("available_models")) {
            JSONArray models = response.getJSONArray("available_models");
            String currentModel = response.optString("current_model", "unknown");

            System.out.println("\n🤖 Available Models (" + models.length() + "):");
            System.out.println("  Current: " + currentModel);

            for (int i = 0; i < Math.min(5, models.length()); i++) {
                String model = models.getString(i);
                String indicator = model.equals(currentModel) ? "⭐" : "  ";
                System.out.printf("  %s %s\n", indicator, model);
            }

            if (models.length() > 5) {
                System.out.println("  ... and " + (models.length() - 5) + " more");
            }
        }

        if (response.has("vector_count")) {
            System.out.println("\n🗂️  Vector Count: " + response.getInt("vector_count"));
        }

        if (response.has("status") && response.getString("status").equals("error")) {
            System.out.println("\n❌ Error details:");
            System.out.println(response.toString(2));
        }
    }
}