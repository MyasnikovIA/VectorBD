package ru.miacomsoft.vectordb.demo;

import ru.miacomsoft.vectordb.core.*;
import ru.miacomsoft.vectordb.knowledge.KnowledgeConfig;
import ru.miacomsoft.vectordb.knowledge.KnowledgeLoader;
import ru.miacomsoft.vectordb.knowledge.OllamaKnowledgeClient;
import org.json.JSONObject;
import org.json.JSONArray;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Сервер для RAGSQLDemo с поддержкой всех RAG операций
 */
public class RAGSQLServer {
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private RAGSystem ragSystem;
    private boolean isRunning;
    private int port;

    public RAGSQLServer(int port, String databasePath) throws Exception {
        this.port = port;
        this.ragSystem = new RAGSystem(databasePath);
        this.ragSystem.initializeKnowledgeBase();

        this.serverSocket = new ServerSocket(port);
        this.threadPool = Executors.newFixedThreadPool(10);
        this.isRunning = true;

        System.out.println("=== RAGSQL Server ===");
        System.out.println("Port: " + port);
        System.out.println("Database: " + databasePath);
        System.out.println("RAG System: Initialized");
    }

    public void start() {
        System.out.println("🚀 Server listening on port " + port + "...");

        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("📡 New client: " + clientSocket.getInetAddress());
                threadPool.execute(new RAGSQLClientHandler(clientSocket, ragSystem));
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("❌ Client connection error: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        isRunning = false;
        try {
            serverSocket.close();
            threadPool.shutdown();
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
            ragSystem.close();
            System.out.println("🛑 RAGSQL Server stopped");
        } catch (Exception e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }

    /**
     * Обработчик клиентских соединений для RAGSQL
     */
    private static class RAGSQLClientHandler implements Runnable {
        private Socket clientSocket;
        private RAGSystem ragSystem;
        private BufferedReader in;
        private PrintWriter out;

        public RAGSQLClientHandler(Socket socket, RAGSystem ragSystem) {
            this.clientSocket = socket;
            this.ragSystem = ragSystem;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                String request;
                while ((request = in.readLine()) != null) {
                    System.out.println("📨 Received RAGSQL request: " +
                            request.substring(0, Math.min(100, request.length())) + "...");
                    processRAGSQLRequest(request);
                }
            } catch (IOException e) {
                System.err.println("❌ RAGSQL handler error: " + e.getMessage());
            } finally {
                closeConnection();
            }
        }

        private void processRAGSQLRequest(String request) {
            try {
                JSONObject requestJson = new JSONObject(request);
                String command = requestJson.getString("command");
                JSONObject response = new JSONObject();

                switch (command.toLowerCase()) {
                    case "ping":
                        handlePing(response);
                        break;
                    case "basic_rag":
                        handleBasicRAG(requestJson, response);
                        break;
                    case "complex_rag":
                        handleComplexRAG(requestJson, response);
                        break;
                    case "filtered_rag":
                        handleFilteredRAG(requestJson, response);
                        break;
                    case "rag_evaluation":
                        handleRAGEvaluation(requestJson, response);
                        break;
                    case "get_rag_stats":
                        handleGetRAGStats(response);
                        break;
                    case "interactive_rag":
                        handleInteractiveRAG(requestJson, response);
                        break;
                    default:
                        response.put("status", "error");
                        response.put("message", "Unknown RAGSQL command: " + command);
                }

                out.println(response.toString());
                System.out.println("📤 Sent RAGSQL response for: " + command);

            } catch (Exception e) {
                JSONObject errorResponse = new JSONObject();
                errorResponse.put("status", "error");
                errorResponse.put("message", "RAGSQL processing error: " + e.getMessage());
                out.println(errorResponse.toString());
                System.err.println("❌ RAGSQL processing error: " + e.getMessage());
            }
        }

        private void handlePing(JSONObject response) {
            response.put("status", "success");
            response.put("message", "RAGSQL Server is running");
            response.put("timestamp", System.currentTimeMillis());
            response.put("server_type", "RAGSQL");
        }

        private void handleBasicRAG(JSONObject request, JSONObject response) throws Exception {
            String question = request.getString("question");
            int topK = request.optInt("top_k", 3);
            double threshold = request.optDouble("threshold", 0.7);

            RAGResultV1 result = ragSystem.executeRAGQuery(question, topK, threshold);

            response.put("status", "success");
            response.put("question", question);
            response.put("generated_response", result.getGeneratedResponse());
            response.put("retrieved_documents", new JSONArray(result.getRetrievedDocuments()));
            response.put("total_documents", result.getRetrievedDocuments().size());
        }

        private void handleComplexRAG(JSONObject request, JSONObject response) throws Exception {
            String question = request.getString("question");
            int topK = request.optInt("top_k", 5);
            double threshold = request.optDouble("threshold", 0.6);

            RAGResultV1 result = ragSystem.executeComplexRAGQuery(question, topK, threshold);

            response.put("status", "success");
            response.put("question", question);
            response.put("generated_response", result.getGeneratedResponse());
            response.put("retrieved_documents", new JSONArray(result.getRetrievedDocuments()));
            response.put("search_type", "complex_multi_stage");
        }

        private void handleFilteredRAG(JSONObject request, JSONObject response) throws Exception {
            String question = request.getString("question");
            String sourceFilter = request.optString("source_filter", null);
            int topK = request.optInt("top_k", 4);
            double threshold = request.optDouble("threshold", 0.7);

            RAGResultV1 result = ragSystem.executeFilteredRAGQuery(question, sourceFilter, topK, threshold);

            response.put("status", "success");
            response.put("question", question);
            response.put("source_filter", sourceFilter);
            response.put("generated_response", result.getGeneratedResponse());
            response.put("retrieved_documents", new JSONArray(result.getRetrievedDocuments()));
        }

        private void handleRAGEvaluation(JSONObject request, JSONObject response) throws Exception {
            String question = request.getString("question");
            int topK = request.optInt("top_k", 3);
            double threshold = request.optDouble("threshold", 0.7);

            RAGResultV1 result = ragSystem.executeRAGQuery(question, topK, threshold);
            RAGEvaluationMetrics metrics = ragSystem.evaluateRAGResult(result, question);

            response.put("status", "success");
            response.put("question", question);
            response.put("evaluation_metrics", new JSONObject()
                    .put("retrieved_count", metrics.getRetrievedCount())
                    .put("average_similarity", metrics.getAverageSimilarity())
                    .put("max_similarity", metrics.getMaxSimilarity())
                    .put("response_length", metrics.getResponseLength())
                    .put("context_utilization", metrics.getContextUtilization())
            );
        }

        private void handleGetRAGStats(JSONObject response) {
            response.put("status", "success");
            response.put("server_type", "RAGSQL");
            response.put("rag_capabilities", new JSONArray()
                    .put("basic_rag")
                    .put("complex_rag")
                    .put("filtered_rag")
                    .put("evaluation")
                    .put("interactive")
            );
        }

        private void handleInteractiveRAG(JSONObject request, JSONObject response) throws Exception {
            String question = request.getString("question");

            // Автоматическое определение параметров
            RAGParameters params = ragSystem.autoDetectParameters(question);
            RAGResultV1 result = ragSystem.executeRAGQuery(question, params.getTopK(), params.getSimilarityThreshold());

            response.put("status", "success");
            response.put("question", question);
            response.put("auto_parameters", new JSONObject()
                    .put("top_k", params.getTopK())
                    .put("similarity_threshold", params.getSimilarityThreshold())
            );
            response.put("generated_response", result.getGeneratedResponse());
            response.put("retrieved_documents_count", result.getRetrievedDocuments().size());
        }

        private void closeConnection() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
                System.out.println("🔌 Client disconnected: " + clientSocket.getInetAddress());
            } catch (IOException e) {
                System.err.println("Error closing RAGSQL connection: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        try {
            int port = args.length > 0 ? Integer.parseInt(args[0]) : 9090;
            String databasePath = args.length > 1 ? args[1] : "./data/ragsql_server";

            RAGSQLServer server = new RAGSQLServer(port, databasePath);

            // Graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n🛑 Shutting down RAGSQL Server...");
                server.stop();
            }));

            server.start();

        } catch (Exception e) {
            System.err.println("❌ Failed to start RAGSQL Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}