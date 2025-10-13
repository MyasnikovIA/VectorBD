package ru.miacomsoft.vectordb.demo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Socket клиент для взаимодействия с VectorDB Server
 * Поддерживает все основные операции с BinaryTreeNode, BinaryVectorData, BinaryVectorDatabase,
 * SemanticChunker, SQLParser, VectorIndex, VectorSearchResult
 */
public class VectorDBClient {
    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public VectorDBClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Подключение к серверу
     */
    public boolean connect() {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("✅ Connected to VectorDB Server at " + host + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("❌ Connection failed: " + e.getMessage());
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
            System.out.println("🔌 Disconnected from server");
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
            error.put("message", "Communication error: " + e.getMessage());
            return error;
        } catch (Exception e) {
            JSONObject error = new JSONObject();
            error.put("status", "error");
            error.put("message", "Error parsing response: " + e.getMessage());
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
     * Сохранить текст в базу данных
     */
    public JSONObject storeText(String text, String documentId, String[] path) {
        JSONObject command = new JSONObject();
        command.put("command", "store_text");
        command.put("text", text);
        command.put("document_id", documentId);

        JSONArray pathArray = new JSONArray();
        for (String p : path) {
            pathArray.put(p);
        }
        command.put("path", pathArray);

        return sendCommand(command);
    }

    /**
     * Семантический поиск
     */
    public JSONObject similaritySearch(String query, int limit, double threshold) {
        JSONObject command = new JSONObject();
        command.put("command", "similarity_search");
        command.put("query", query);
        command.put("limit", limit);
        command.put("threshold", threshold);

        return sendCommand(command);
    }

    /**
     * Семантический поиск (упрощенный)
     */
    public JSONObject similaritySearch(String query, int limit) {
        return similaritySearch(query, limit, 0.5);
    }

    /**
     * Точный поиск по тексту
     */
    public JSONObject exactSearch(String searchText) {
        JSONObject command = new JSONObject();
        command.put("command", "exact_search");
        command.put("search_text", searchText);

        return sendCommand(command);
    }

    /**
     * Семантический поиск через SQLParser
     */
    public JSONObject semanticSearch(String query, int limit) {
        JSONObject command = new JSONObject();
        command.put("command", "semantic_search");
        command.put("query", query);
        command.put("limit", limit);

        return sendCommand(command);
    }

    /**
     * Гибридный поиск
     */
    public JSONObject hybridSearch(String query, int limit) {
        JSONObject command = new JSONObject();
        command.put("command", "hybrid_search");
        command.put("query", query);
        command.put("limit", limit);

        return sendCommand(command);
    }

    /**
     * Получить статистику базы данных
     */
    public JSONObject getStats() {
        JSONObject command = new JSONObject();
        command.put("command", "get_stats");

        return sendCommand(command);
    }

    /**
     * Получить вектор по ID
     */
    public JSONObject getVector(String vectorId) {
        JSONObject command = new JSONObject();
        command.put("command", "get_vector");
        command.put("vector_id", vectorId);

        return sendCommand(command);
    }

    /**
     * Удалить вектор по ID
     */
    public JSONObject removeVector(String vectorId) {
        JSONObject command = new JSONObject();
        command.put("command", "remove_vector");
        command.put("vector_id", vectorId);

        return sendCommand(command);
    }

    /**
     * Выполнить SQL запрос
     */
    public JSONObject executeSQL(String sql) {
        JSONObject command = new JSONObject();
        command.put("command", "sql_query");
        command.put("sql", sql);

        return sendCommand(command);
    }

    /**
     * Получить BinaryTreeNode по ID
     */
    public JSONObject getTreeNode(String nodeId) {
        JSONObject command = new JSONObject();
        command.put("command", "get_tree_node");
        command.put("node_id", nodeId);

        return sendCommand(command);
    }

    /**
     * Поиск по пути
     */
    public JSONObject searchByPath(String pathPattern) {
        JSONObject command = new JSONObject();
        command.put("command", "search_by_path");
        command.put("path_pattern", pathPattern);

        return sendCommand(command);
    }

    /**
     * Получить все векторы
     */
    public JSONObject getAllVectors() {
        JSONObject command = new JSONObject();
        command.put("command", "get_all_vectors");

        return sendCommand(command);
    }

    /**
     * Демонстрация использования клиента
     */
    public static void main(String[] args) {
        VectorDBClient client = new VectorDBClient("localhost", 8080);

        if (!client.connect()) {
            return;
        }

        try {
            // Демонстрация работы с клиентом
            demonstrateClient(client);

        } finally {
            client.disconnect();
        }
    }

    /**
     * Демонстрация возможностей клиента
     */
    private static void demonstrateClient(VectorDBClient client) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n=== VectorDB Client Demo ===");
        System.out.println("Using BinaryVectorDatabase, BinaryTreeNode, BinaryVectorData,");
        System.out.println("SemanticChunker, SQLParser, VectorIndex, VectorSearchResult");

        while (true) {
            System.out.println("\nAvailable commands:");
            System.out.println("1.  Ping server");
            System.out.println("2.  Store text");
            System.out.println("3.  Similarity search");
            System.out.println("4.  Exact search");
            System.out.println("5.  Semantic search");
            System.out.println("6.  Hybrid search");
            System.out.println("7.  Get statistics");
            System.out.println("8.  Get vector by ID");
            System.out.println("9.  Remove vector");
            System.out.println("10. Execute SQL query");
            System.out.println("11. Get tree node");
            System.out.println("12. Search by path");
            System.out.println("13. Get all vectors");
            System.out.println("14. Interactive mode");
            System.out.println("0.  Exit");
            System.out.print("Choose command: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    demoPing(client);
                    break;
                case "2":
                    demoStoreText(client, scanner);
                    break;
                case "3":
                    demoSimilaritySearch(client, scanner);
                    break;
                case "4":
                    demoExactSearch(client, scanner);
                    break;
                case "5":
                    demoSemanticSearch(client, scanner);
                    break;
                case "6":
                    demoHybridSearch(client, scanner);
                    break;
                case "7":
                    demoGetStats(client);
                    break;
                case "8":
                    demoGetVector(client, scanner);
                    break;
                case "9":
                    demoRemoveVector(client, scanner);
                    break;
                case "10":
                    demoSQLQuery(client, scanner);
                    break;
                case "11":
                    demoGetTreeNode(client, scanner);
                    break;
                case "12":
                    demoSearchByPath(client, scanner);
                    break;
                case "13":
                    demoGetAllVectors(client);
                    break;
                case "14":
                    startInteractiveMode(client, scanner);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid choice");
            }
        }
    }

    private static void demoPing(VectorDBClient client) {
        JSONObject response = client.ping();
        printResponse(response);
    }

    private static void demoStoreText(VectorDBClient client, Scanner scanner) {
        System.out.print("Enter text to store: ");
        String text = scanner.nextLine();

        System.out.print("Enter document ID: ");
        String docId = scanner.nextLine();

        System.out.print("Enter path (comma separated): ");
        String pathInput = scanner.nextLine();
        String[] path = pathInput.split(",");

        JSONObject response = client.storeText(text, docId, path);
        printResponse(response);
    }

    private static void demoSimilaritySearch(VectorDBClient client, Scanner scanner) {
        System.out.print("Enter search query: ");
        String query = scanner.nextLine();

        System.out.print("Enter result limit (default 5): ");
        String limitInput = scanner.nextLine();
        int limit = limitInput.isEmpty() ? 5 : Integer.parseInt(limitInput);

        System.out.print("Enter similarity threshold (default 0.5): ");
        String thresholdInput = scanner.nextLine();
        double threshold = thresholdInput.isEmpty() ? 0.5 : Double.parseDouble(thresholdInput);

        JSONObject response = client.similaritySearch(query, limit, threshold);
        printResponse(response);
    }

    private static void demoExactSearch(VectorDBClient client, Scanner scanner) {
        System.out.print("Enter exact search text: ");
        String searchText = scanner.nextLine();

        JSONObject response = client.exactSearch(searchText);
        printResponse(response);
    }

    private static void demoSemanticSearch(VectorDBClient client, Scanner scanner) {
        System.out.print("Enter semantic search query: ");
        String query = scanner.nextLine();

        System.out.print("Enter result limit (default 5): ");
        String limitInput = scanner.nextLine();
        int limit = limitInput.isEmpty() ? 5 : Integer.parseInt(limitInput);

        JSONObject response = client.semanticSearch(query, limit);
        printResponse(response);
    }

    private static void demoHybridSearch(VectorDBClient client, Scanner scanner) {
        System.out.print("Enter hybrid search query: ");
        String query = scanner.nextLine();

        System.out.print("Enter result limit (default 5): ");
        String limitInput = scanner.nextLine();
        int limit = limitInput.isEmpty() ? 5 : Integer.parseInt(limitInput);

        JSONObject response = client.hybridSearch(query, limit);
        printResponse(response);
    }

    private static void demoGetStats(VectorDBClient client) {
        JSONObject response = client.getStats();
        printResponse(response);
    }

    private static void demoGetVector(VectorDBClient client, Scanner scanner) {
        System.out.print("Enter vector ID: ");
        String vectorId = scanner.nextLine();

        JSONObject response = client.getVector(vectorId);
        printResponse(response);
    }

    private static void demoRemoveVector(VectorDBClient client, Scanner scanner) {
        System.out.print("Enter vector ID to remove: ");
        String vectorId = scanner.nextLine();

        JSONObject response = client.removeVector(vectorId);
        printResponse(response);
    }

    private static void demoSQLQuery(VectorDBClient client, Scanner scanner) {
        System.out.print("Enter SQL query: ");
        String sql = scanner.nextLine();

        JSONObject response = client.executeSQL(sql);
        printResponse(response);
    }

    private static void demoGetTreeNode(VectorDBClient client, Scanner scanner) {
        System.out.print("Enter tree node ID: ");
        String nodeId = scanner.nextLine();

        JSONObject response = client.getTreeNode(nodeId);
        printResponse(response);
    }

    private static void demoSearchByPath(VectorDBClient client, Scanner scanner) {
        System.out.print("Enter path pattern: ");
        String pathPattern = scanner.nextLine();

        JSONObject response = client.searchByPath(pathPattern);
        printResponse(response);
    }

    private static void demoGetAllVectors(VectorDBClient client) {
        JSONObject response = client.getAllVectors();
        printResponse(response);
    }

    private static void startInteractiveMode(VectorDBClient client, Scanner scanner) {
        System.out.println("\n🔍 Interactive Mode - Enter JSON commands directly");
        System.out.println("Example: {\"command\": \"get_stats\"}");
        System.out.println("Type 'exit' to return to menu\n");

        while (true) {
            System.out.print("JSON Command > ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("exit")) {
                break;
            }

            try {
                JSONObject command = new JSONObject(input);
                JSONObject response = client.sendCommand(command);
                printResponse(response);
            } catch (Exception e) {
                System.out.println("❌ Invalid JSON: " + e.getMessage());
            }
        }
    }

    /**
     * Красивая печать JSON ответа
     */
    private static void printResponse(JSONObject response) {
        System.out.println("\n📋 Response:");
        System.out.println("Status: " + response.optString("status", "unknown"));

        if (response.has("message")) {
            System.out.println("Message: " + response.getString("message"));
        }

        if (response.has("vector_count")) {
            System.out.println("Vector count: " + response.getInt("vector_count"));
        }

        if (response.has("node_count")) {
            System.out.println("Tree node count: " + response.getInt("node_count"));
        }

        if (response.has("results")) {
            JSONArray results = response.getJSONArray("results");
            System.out.println("\n📊 Results (" + results.length() + " items):");

            for (int i = 0; i < results.length(); i++) {
                JSONObject result = results.getJSONObject(i);
                System.out.println("\n  " + (i + 1) + ". " + formatResult(result));
            }
        }

        if (response.has("vectors")) {
            JSONArray vectors = response.getJSONArray("vectors");
            System.out.println("\n🗂️  Vectors (" + vectors.length() + " items):");

            for (int i = 0; i < Math.min(5, vectors.length()); i++) {
                JSONObject vector = vectors.getJSONObject(i);
                System.out.println("  " + (i + 1) + ". " + formatVectorData(vector));
            }

            if (vectors.length() > 5) {
                System.out.println("  ... and " + (vectors.length() - 5) + " more");
            }
        }

        // Детальная информация для отладки
        if (response.has("status") && response.getString("status").equals("error")) {
            System.out.println("\n❌ Error details:");
            System.out.println(response.toString(2));
        }
    }

    private static String formatResult(JSONObject result) {
        StringBuilder sb = new StringBuilder();

        if (result.has("similarity")) {
            sb.append("Similarity: ").append(String.format("%.4f", result.getDouble("similarity"))).append(" | ");
        }

        if (result.has("vector_data")) {
            JSONObject vectorData = result.getJSONObject("vector_data");
            sb.append(formatVectorData(vectorData));
        } else {
            // Для SQL результатов
            if (result.has("text")) {
                String text = result.getString("text");
                sb.append("Text: ").append(text.substring(0, Math.min(50, text.length()))).append("...");
            }
            if (result.has("id")) {
                sb.append(" [ID: ").append(result.getString("id")).append("]");
            }
        }

        return sb.toString();
    }

    private static String formatVectorData(JSONObject vectorData) {
        StringBuilder sb = new StringBuilder();

        if (vectorData.has("text")) {
            String text = vectorData.getString("text");
            sb.append("Text: ").append(text.substring(0, Math.min(50, text.length()))).append("...");
        }

        if (vectorData.has("id")) {
            sb.append(" [ID: ").append(vectorData.getString("id")).append("]");
        }

        if (vectorData.has("document_id")) {
            sb.append(" [Doc: ").append(vectorData.getString("document_id")).append("]");
        }

        if (vectorData.has("chunk_index")) {
            sb.append(" [Chunk: ").append(vectorData.getInt("chunk_index")).append("]");
        }

        return sb.toString();
    }
}