package ru.miacomsoft.vectordb.demo;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Клиент для RAGWithEmbeddingSQLDemo сервера
 */
public class RAGWithEmbeddingSQLClient {
    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public RAGWithEmbeddingSQLClient(String host, int port) {
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
            System.out.println("✅ Connected to RAGWithEmbeddingSQL Server at " + host + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("❌ Embedding Connection failed: " + e.getMessage());
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
            System.out.println("🔌 Disconnected from embedding server");
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
            error.put("message", "Embedding Communication error: " + e.getMessage());
            return error;
        } catch (Exception e) {
            JSONObject error = new JSONObject();
            error.put("status", "error");
            error.put("message", "Embedding Response parsing error: " + e.getMessage());
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
     * Embedding RAG запрос
     */
    public JSONObject embeddingRAG(String question, int topK, double threshold) {
        JSONObject command = new JSONObject();
        command.put("command", "embedding_rag");
        command.put("question", question);
        command.put("top_k", topK);
        command.put("threshold", threshold);

        return sendCommand(command);
    }

    /**
     * Гибридный RAG запрос
     */
    public JSONObject hybridRAG(String question, int topK, double embeddingThreshold, double keywordThreshold) {
        JSONObject command = new JSONObject();
        command.put("command", "hybrid_rag");
        command.put("question", question);
        command.put("top_k", topK);
        command.put("embedding_threshold", embeddingThreshold);
        command.put("keyword_threshold", keywordThreshold);

        return sendCommand(command);
    }

    /**
     * Weighted hybrid RAG запрос
     */
    public JSONObject weightedHybridRAG(String question, int topK, double embeddingWeight, double keywordWeight) {
        JSONObject command = new JSONObject();
        command.put("command", "weighted_hybrid_rag");
        command.put("question", question);
        command.put("top_k", topK);
        command.put("embedding_weight", embeddingWeight);
        command.put("keyword_weight", keywordWeight);

        return sendCommand(command);
    }

    /**
     * Multi-stage RAG запрос
     */
    public JSONObject multiStageRAG(String question, int topK) {
        JSONObject command = new JSONObject();
        command.put("command", "multi_stage_rag");
        command.put("question", question);
        command.put("top_k", topK);

        return sendCommand(command);
    }

    /**
     * Strategic RAG запрос
     */
    public JSONObject strategicRAG(String question, String strategyType, JSONObject strategyParams) {
        JSONObject command = new JSONObject();
        command.put("command", "strategic_rag");
        command.put("question", question);
        command.put("strategy_type", strategyType);

        if (strategyParams != null) {
            for (String key : strategyParams.keySet()) {
                command.put(key, strategyParams.get(key));
            }
        }

        return sendCommand(command);
    }

    /**
     * Тест производительности
     */
    public JSONObject performanceTest(String question, int iterations) {
        JSONObject command = new JSONObject();
        command.put("command", "performance_test");
        command.put("question", question);
        command.put("iterations", iterations);

        return sendCommand(command);
    }

    /**
     * Получить статистику embedding системы
     */
    public JSONObject getEmbeddingStats() {
        JSONObject command = new JSONObject();
        command.put("command", "get_embedding_stats");

        return sendCommand(command);
    }

    /**
     * Демонстрация работы клиента
     */
    public static void main(String[] args) {
        RAGWithEmbeddingSQLClient client = new RAGWithEmbeddingSQLClient("localhost", 9091);

        if (!client.connect()) {
            return;
        }

        try {
            demonstrateEmbeddingClient(client);

        } finally {
            client.disconnect();
        }
    }

    /**
     * Демонстрация возможностей embedding клиента
     */
    private static void demonstrateEmbeddingClient(RAGWithEmbeddingSQLClient client) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n=== RAGWithEmbeddingSQL Client Demo ===");
        System.out.println("Advanced RAG with embedding search and SQL");

        while (true) {
            System.out.println("\nAvailable Embedding RAG commands:");
            System.out.println("1.  Ping server");
            System.out.println("2.  Embedding RAG search");
            System.out.println("3.  Hybrid RAG search");
            System.out.println("4.  Weighted hybrid RAG");
            System.out.println("5.  Multi-stage RAG");
            System.out.println("6.  Strategic RAG (auto)");
            System.out.println("7.  Strategic RAG (custom)");
            System.out.println("8.  Performance test");
            System.out.println("9.  Get embedding stats");
            System.out.println("10. Interactive embedding chat");
            System.out.println("0.  Exit");
            System.out.print("Choose command: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    demoPing(client);
                    break;
                case "2":
                    demoEmbeddingRAG(client, scanner);
                    break;
                case "3":
                    demoHybridRAG(client, scanner);
                    break;
                case "4":
                    demoWeightedHybridRAG(client, scanner);
                    break;
                case "5":
                    demoMultiStageRAG(client, scanner);
                    break;
                case "6":
                    demoStrategicRAGAuto(client, scanner);
                    break;
                case "7":
                    demoStrategicRAGCustom(client, scanner);
                    break;
                case "8":
                    demoPerformanceTest(client, scanner);
                    break;
                case "9":
                    demoGetEmbeddingStats(client);
                    break;
                case "10":
                    startInteractiveEmbeddingChat(client, scanner);
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Invalid choice");
            }
        }
    }

    private static void demoPing(RAGWithEmbeddingSQLClient client) {
        JSONObject response = client.ping();
        printEmbeddingResponse(response);
    }

    private static void demoEmbeddingRAG(RAGWithEmbeddingSQLClient client, Scanner scanner) {
        System.out.print("Enter question for embedding RAG: ");
        String question = scanner.nextLine();

        System.out.print("Enter top K (default 4): ");
        String topKInput = scanner.nextLine();
        int topK = topKInput.isEmpty() ? 4 : Integer.parseInt(topKInput);

        System.out.print("Enter similarity threshold (default 0.7): ");
        String thresholdInput = scanner.nextLine();
        double threshold = thresholdInput.isEmpty() ? 0.7 : Double.parseDouble(thresholdInput);

        JSONObject response = client.embeddingRAG(question, topK, threshold);
        printEmbeddingResponse(response);
    }

    private static void demoHybridRAG(RAGWithEmbeddingSQLClient client, Scanner scanner) {
        System.out.print("Enter question for hybrid RAG: ");
        String question = scanner.nextLine();

        System.out.print("Enter top K (default 6): ");
        String topKInput = scanner.nextLine();
        int topK = topKInput.isEmpty() ? 6 : Integer.parseInt(topKInput);

        System.out.print("Enter embedding threshold (default 0.7): ");
        String embInput = scanner.nextLine();
        double embThreshold = embInput.isEmpty() ? 0.7 : Double.parseDouble(embInput);

        System.out.print("Enter keyword threshold (default 0.5): ");
        String kwInput = scanner.nextLine();
        double kwThreshold = kwInput.isEmpty() ? 0.5 : Double.parseDouble(kwInput);

        JSONObject response = client.hybridRAG(question, topK, embThreshold, kwThreshold);
        printEmbeddingResponse(response);
    }

    private static void demoWeightedHybridRAG(RAGWithEmbeddingSQLClient client, Scanner scanner) {
        System.out.print("Enter question for weighted hybrid RAG: ");
        String question = scanner.nextLine();

        System.out.print("Enter top K (default 5): ");
        String topKInput = scanner.nextLine();
        int topK = topKInput.isEmpty() ? 5 : Integer.parseInt(topKInput);

        System.out.print("Enter embedding weight (default 0.7): ");
        String embInput = scanner.nextLine();
        double embWeight = embInput.isEmpty() ? 0.7 : Double.parseDouble(embInput);

        System.out.print("Enter keyword weight (default 0.3): ");
        String kwInput = scanner.nextLine();
        double kwWeight = kwInput.isEmpty() ? 0.3 : Double.parseDouble(kwInput);

        JSONObject response = client.weightedHybridRAG(question, topK, embWeight, kwWeight);
        printEmbeddingResponse(response);
    }

    private static void demoMultiStageRAG(RAGWithEmbeddingSQLClient client, Scanner scanner) {
        System.out.print("Enter question for multi-stage RAG: ");
        String question = scanner.nextLine();

        System.out.print("Enter top K (default 4): ");
        String topKInput = scanner.nextLine();
        int topK = topKInput.isEmpty() ? 4 : Integer.parseInt(topKInput);

        JSONObject response = client.multiStageRAG(question, topK);
        printEmbeddingResponse(response);
    }

    private static void demoStrategicRAGAuto(RAGWithEmbeddingSQLClient client, Scanner scanner) {
        System.out.print("Enter question for strategic RAG (auto): ");
        String question = scanner.nextLine();

        JSONObject response = client.strategicRAG(question, "auto", null);
        printEmbeddingResponse(response);
    }

    private static void demoStrategicRAGCustom(RAGWithEmbeddingSQLClient client, Scanner scanner) {
        System.out.print("Enter question for strategic RAG: ");
        String question = scanner.nextLine();

        System.out.print("Enter strategy type (embedding/hybrid/weighted_hybrid/multi_stage): ");
        String strategyType = scanner.nextLine();

        JSONObject params = new JSONObject();
        System.out.print("Enter top K (default 3): ");
        String topKInput = scanner.nextLine();
        if (!topKInput.isEmpty()) params.put("top_k", Integer.parseInt(topKInput));

        System.out.print("Enter similarity threshold (default 0.7): ");
        String thresholdInput = scanner.nextLine();
        if (!thresholdInput.isEmpty()) params.put("similarity_threshold", Double.parseDouble(thresholdInput));

        JSONObject response = client.strategicRAG(question, strategyType, params);
        printEmbeddingResponse(response);
    }

    private static void demoPerformanceTest(RAGWithEmbeddingSQLClient client, Scanner scanner) {
        System.out.print("Enter question for performance test: ");
        String question = scanner.nextLine();

        System.out.print("Enter iterations (default 3): ");
        String iterInput = scanner.nextLine();
        int iterations = iterInput.isEmpty() ? 3 : Integer.parseInt(iterInput);

        JSONObject response = client.performanceTest(question, iterations);
        printEmbeddingResponse(response);
    }

    private static void demoGetEmbeddingStats(RAGWithEmbeddingSQLClient client) {
        JSONObject response = client.getEmbeddingStats();
        printEmbeddingResponse(response);
    }

    private static void startInteractiveEmbeddingChat(RAGWithEmbeddingSQLClient client, Scanner scanner) {
        System.out.println("\n💬 Embedding RAG Interactive Chat Mode");
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

            System.out.print("Embedding RAG: ");
            JSONObject response = client.strategicRAG(question, "auto", null);

            if (response.getString("status").equals("success")) {
                System.out.println(response.getString("generated_response"));

                // Дополнительная информация о стратегии
                String strategy = response.getString("strategy_used");
                String params = response.getString("strategy_parameters");
                int docCount = response.getJSONArray("retrieved_documents").length();

                System.out.printf("🔍 [Strategy: %s, Params: %s, Documents: %d]\n",
                        strategy, params, docCount);
            } else {
                System.out.println("❌ Error: " + response.getString("message"));
            }
        }
    }

    /**
     * Красивая печать embedding ответа
     */
    private static void printEmbeddingResponse(JSONObject response) {
        System.out.println("\n🎯 Embedding RAG Response:");
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
                    System.out.println(text.substring(0, Math.min(70, text.length())) + "...");
                }

                if (doc.has("similarity")) {
                    System.out.printf("    [Similarity: %.3f", doc.getDouble("similarity"));
                }
                if (doc.has("embedding_score")) {
                    System.out.printf(", Embedding: %.3f", doc.getDouble("embedding_score"));
                }
                if (doc.has("search_type")) {
                    System.out.printf(", Type: %s", doc.getString("search_type"));
                }
                System.out.println("]");
            }
        }

        if (response.has("search_strategy")) {
            System.out.println("\n🎯 Search Strategy: " + response.getString("search_strategy"));
        }

        if (response.has("search_time_ms")) {
            System.out.println("⏱️  Search Time: " + response.getLong("search_time_ms") + " ms");
        }

        if (response.has("strategy_used")) {
            System.out.println("\n⚡ Strategy Used: " + response.getString("strategy_used"));
            System.out.println("⚙️  Parameters: " + response.getString("strategy_parameters"));
        }

        if (response.has("performance_results")) {
            JSONArray results = response.getJSONArray("performance_results");
            System.out.println("\n📊 Performance Results (" + results.length() + " iterations):");

            long totalTime = 0;
            for (int i = 0; i < results.length(); i++) {
                JSONObject result = results.getJSONObject(i);
                long time = result.getLong("search_time_ms");
                totalTime += time;
                System.out.printf("  %d. %d ms, %d docs, %s\n",
                        result.getInt("iteration"), time,
                        result.getInt("documents_found"),
                        result.getString("strategy"));
            }
            System.out.printf("  Average: %.1f ms\n", (double) totalTime / results.length());
        }

        if (response.has("embedding_capabilities")) {
            JSONArray capabilities = response.getJSONArray("embedding_capabilities");
            System.out.println("\n🔧 Embedding Capabilities:");
            for (int i = 0; i < capabilities.length(); i++) {
                System.out.println("  - " + capabilities.getString(i));
            }
        }

        if (response.has("status") && response.getString("status").equals("error")) {
            System.out.println("\n❌ Error details:");
            System.out.println(response.toString(2));
        }
    }
}