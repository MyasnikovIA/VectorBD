package ru.miacomsoft.vectordb.demo;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Клиент для RAGSQLDemo сервера
 */
public class RAGSQLClient {
    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public RAGSQLClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Подключение к RAGSQL серверу
     */
    public boolean connect() {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("✅ Connected to RAGSQL Server at " + host + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("❌ RAGSQL Connection failed: " + e.getMessage());
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
            System.out.println("🔌 Disconnected from RAGSQL server");
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
            error.put("message", "RAGSQL Communication error: " + e.getMessage());
            return error;
        } catch (Exception e) {
            JSONObject error = new JSONObject();
            error.put("status", "error");
            error.put("message", "RAGSQL Response parsing error: " + e.getMessage());
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
     * Базовый RAG запрос
     */
    public JSONObject basicRAG(String question, int topK, double threshold) {
        JSONObject command = new JSONObject();
        command.put("command", "basic_rag");
        command.put("question", question);
        command.put("top_k", topK);
        command.put("threshold", threshold);

        return sendCommand(command);
    }

    /**
     * Базовый RAG запрос (упрощенный)
     */
    public JSONObject basicRAG(String question) {
        return basicRAG(question, 3, 0.7);
    }

    /**
     * Комплексный RAG запрос
     */
    public JSONObject complexRAG(String question, int topK, double threshold) {
        JSONObject command = new JSONObject();
        command.put("command", "complex_rag");
        command.put("question", question);
        command.put("top_k", topK);
        command.put("threshold", threshold);

        return sendCommand(command);
    }

    /**
     * Фильтрованный RAG запрос
     */
    public JSONObject filteredRAG(String question, String sourceFilter, int topK, double threshold) {
        JSONObject command = new JSONObject();
        command.put("command", "filtered_rag");
        command.put("question", question);
        command.put("source_filter", sourceFilter);
        command.put("top_k", topK);
        command.put("threshold", threshold);

        return sendCommand(command);
    }

    /**
     * Оценка RAG результата
     */
    public JSONObject evaluateRAG(String question, int topK, double threshold) {
        JSONObject command = new JSONObject();
        command.put("command", "rag_evaluation");
        command.put("question", question);
        command.put("top_k", topK);
        command.put("threshold", threshold);

        return sendCommand(command);
    }

    /**
     * Получить статистику RAG системы
     */
    public JSONObject getRAGStats() {
        JSONObject command = new JSONObject();
        command.put("command", "get_rag_stats");

        return sendCommand(command);
    }

    /**
     * Интерактивный RAG запрос с авто-параметрами
     */
    public JSONObject interactiveRAG(String question) {
        JSONObject command = new JSONObject();
        command.put("command", "interactive_rag");
        command.put("question", question);

        return sendCommand(command);
    }

    /**
     * Демонстрация работы клиента
     */
    public static void main(String[] args) {
        RAGSQLClient client = new RAGSQLClient("localhost", 9090);

        if (!client.connect()) {
            return;
        }

        try {
            demonstrateRAGSQLClient(client);

        } finally {
            client.disconnect();
        }
    }

    /**
     * Демонстрация возможностей RAGSQL клиента
     */
    private static void demonstrateRAGSQLClient(RAGSQLClient client) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n=== RAGSQL Client Demo ===");
        System.out.println("Advanced RAG with SQL integration");

        while (true) {
            System.out.println("\nAvailable RAGSQL commands:");
            System.out.println("1.  Ping server");
            System.out.println("2.  Basic RAG query");
            System.out.println("3.  Complex RAG query");
            System.out.println("4.  Filtered RAG query");
            System.out.println("5.  RAG evaluation");
            System.out.println("6.  Get RAG stats");
            System.out.println("7.  Interactive RAG");
            System.out.println("8.  Interactive chat mode");
            System.out.println("0.  Exit");
            System.out.print("Choose command: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    demoPing(client);
                    break;
                case "2":
                    demoBasicRAG(client, scanner);
                    break;
                case "3":
                    demoComplexRAG(client, scanner);
                    break;
                case "4":
                    demoFilteredRAG(client, scanner);
                    break;
                case "5":
                    demoRAGEvaluation(client, scanner);
                    break;
                case "6":
                    demoGetRAGStats(client);
                    break;
                case "7":
                    demoInteractiveRAG(client, scanner);
                    break;
                case "8":
                    startInteractiveChat(client, scanner);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid choice");
            }
        }
    }

    private static void demoPing(RAGSQLClient client) {
        JSONObject response = client.ping();
        printRAGSQLResponse(response);
    }

    private static void demoBasicRAG(RAGSQLClient client, Scanner scanner) {
        System.out.print("Enter question for basic RAG: ");
        String question = scanner.nextLine();

        System.out.print("Enter top K (default 3): ");
        String topKInput = scanner.nextLine();
        int topK = topKInput.isEmpty() ? 3 : Integer.parseInt(topKInput);

        System.out.print("Enter similarity threshold (default 0.7): ");
        String thresholdInput = scanner.nextLine();
        double threshold = thresholdInput.isEmpty() ? 0.7 : Double.parseDouble(thresholdInput);

        JSONObject response = client.basicRAG(question, topK, threshold);
        printRAGSQLResponse(response);
    }

    private static void demoComplexRAG(RAGSQLClient client, Scanner scanner) {
        System.out.print("Enter question for complex RAG: ");
        String question = scanner.nextLine();

        System.out.print("Enter top K (default 5): ");
        String topKInput = scanner.nextLine();
        int topK = topKInput.isEmpty() ? 5 : Integer.parseInt(topKInput);

        System.out.print("Enter similarity threshold (default 0.6): ");
        String thresholdInput = scanner.nextLine();
        double threshold = thresholdInput.isEmpty() ? 0.6 : Double.parseDouble(thresholdInput);

        JSONObject response = client.complexRAG(question, topK, threshold);
        printRAGSQLResponse(response);
    }

    private static void demoFilteredRAG(RAGSQLClient client, Scanner scanner) {
        System.out.print("Enter question for filtered RAG: ");
        String question = scanner.nextLine();

        System.out.print("Enter source filter (optional): ");
        String sourceFilter = scanner.nextLine();

        System.out.print("Enter top K (default 4): ");
        String topKInput = scanner.nextLine();
        int topK = topKInput.isEmpty() ? 4 : Integer.parseInt(topKInput);

        System.out.print("Enter similarity threshold (default 0.7): ");
        String thresholdInput = scanner.nextLine();
        double threshold = thresholdInput.isEmpty() ? 0.7 : Double.parseDouble(thresholdInput);

        JSONObject response = client.filteredRAG(question, sourceFilter, topK, threshold);
        printRAGSQLResponse(response);
    }

    private static void demoRAGEvaluation(RAGSQLClient client, Scanner scanner) {
        System.out.print("Enter question for RAG evaluation: ");
        String question = scanner.nextLine();

        System.out.print("Enter top K (default 3): ");
        String topKInput = scanner.nextLine();
        int topK = topKInput.isEmpty() ? 3 : Integer.parseInt(topKInput);

        System.out.print("Enter similarity threshold (default 0.7): ");
        String thresholdInput = scanner.nextLine();
        double threshold = thresholdInput.isEmpty() ? 0.7 : Double.parseDouble(thresholdInput);

        JSONObject response = client.evaluateRAG(question, topK, threshold);
        printRAGSQLResponse(response);
    }

    private static void demoGetRAGStats(RAGSQLClient client) {
        JSONObject response = client.getRAGStats();
        printRAGSQLResponse(response);
    }

    private static void demoInteractiveRAG(RAGSQLClient client, Scanner scanner) {
        System.out.print("Enter question for interactive RAG: ");
        String question = scanner.nextLine();

        JSONObject response = client.interactiveRAG(question);
        printRAGSQLResponse(response);
    }

    private static void startInteractiveChat(RAGSQLClient client, Scanner scanner) {
        System.out.println("\n💬 RAGSQL Interactive Chat Mode");
        System.out.println("Type your questions (or 'quit' to exit):");

        while (true) {
            System.out.print("\nYou: ");
            String question = scanner.nextLine().trim();

            if (question.equalsIgnoreCase("quit") || question.equalsIgnoreCase("exit")) {
                break;
            }

            if (question.isEmpty()) {
                continue;
            }

            System.out.print("RAGSQL: ");
            JSONObject response = client.interactiveRAG(question);

            if (response.getString("status").equals("success")) {
                System.out.println(response.getString("generated_response"));

                // Дополнительная информация
                int docCount = response.getInt("retrieved_documents_count");
                JSONObject autoParams = response.getJSONObject("auto_parameters");
                System.out.printf("📊 [Documents: %d, TopK: %d, Threshold: %.2f]\n",
                        docCount, autoParams.getInt("top_k"), autoParams.getDouble("similarity_threshold"));
            } else {
                System.out.println("❌ Error: " + response.getString("message"));
            }
        }
    }

    /**
     * Красивая печать RAGSQL ответа
     */
    private static void printRAGSQLResponse(JSONObject response) {
        System.out.println("\n🎯 RAGSQL Response:");
        System.out.println("Status: " + response.optString("status", "unknown"));

        if (response.has("message")) {
            System.out.println("Message: " + response.getString("message"));
        }

        if (response.has("generated_response")) {
            System.out.println("\n🤖 Generated Response:");
            System.out.println("----------------------");
            System.out.println(response.getString("generated_response"));
        }

        if (response.has("retrieved_documents")) {
            JSONArray documents = response.getJSONArray("retrieved_documents");
            System.out.println("\n📚 Retrieved Documents: " + documents.length());

            for (int i = 0; i < Math.min(3, documents.length()); i++) {
                JSONObject doc = documents.getJSONObject(i);
                System.out.printf("  %d. ", i + 1);

                if (doc.has("text")) {
                    String text = doc.getString("text");
                    System.out.println(text.substring(0, Math.min(80, text.length())) + "...");
                }

                if (doc.has("similarity")) {
                    System.out.printf("    [Similarity: %.3f]\n", doc.getDouble("similarity"));
                }
            }
        }

        if (response.has("evaluation_metrics")) {
            JSONObject metrics = response.getJSONObject("evaluation_metrics");
            System.out.println("\n📊 Evaluation Metrics:");
            System.out.printf("  - Retrieved Documents: %d\n", metrics.getInt("retrieved_count"));
            System.out.printf("  - Average Similarity: %.3f\n", metrics.getDouble("average_similarity"));
            System.out.printf("  - Max Similarity: %.3f\n", metrics.getDouble("max_similarity"));
            System.out.printf("  - Response Length: %d chars\n", metrics.getInt("response_length"));
            System.out.printf("  - Context Utilization: %.1f%%\n", metrics.getDouble("context_utilization") * 100);
        }

        if (response.has("auto_parameters")) {
            JSONObject params = response.getJSONObject("auto_parameters");
            System.out.println("\n⚙️  Auto-detected Parameters:");
            System.out.printf("  - Top K: %d\n", params.getInt("top_k"));
            System.out.printf("  - Similarity Threshold: %.2f\n", params.getDouble("similarity_threshold"));
        }

        if (response.has("rag_capabilities")) {
            JSONArray capabilities = response.getJSONArray("rag_capabilities");
            System.out.println("\n🔧 RAG Capabilities:");
            for (int i = 0; i < capabilities.length(); i++) {
                System.out.println("  - " + capabilities.getString(i));
            }
        }

        // Информация об ошибках
        if (response.has("status") && response.getString("status").equals("error")) {
            System.out.println("\n❌ Error details:");
            System.out.println(response.toString(2));
        }
    }
}