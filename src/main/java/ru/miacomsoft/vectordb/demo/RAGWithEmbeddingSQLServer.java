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
 * Сервер для RAGWithEmbeddingSQLDemo с поддержкой embedding поиска
 */
public class RAGWithEmbeddingSQLServer {
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private RAGWithEmbeddingSystem ragSystem;
    private boolean isRunning;
    private int port;

    public RAGWithEmbeddingSQLServer(int port, String databasePath) throws Exception {
        this.port = port;
        this.ragSystem = new RAGWithEmbeddingSystem(databasePath);
        this.ragSystem.createEmbeddingIndexes();
        this.ragSystem.initializeKnowledgeBase();

        this.serverSocket = new ServerSocket(port);
        this.threadPool = Executors.newFixedThreadPool(10);
        this.isRunning = true;

        System.out.println("=== RAGWithEmbeddingSQL Server ===");
        System.out.println("Port: " + port);
        System.out.println("Database: " + databasePath);
        System.out.println("Embedding Search: Enabled");
    }

    public void start() {
        System.out.println("🚀 Server listening on port " + port + "...");

        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("📡 New client: " + clientSocket.getInetAddress());
                threadPool.execute(new EmbeddingClientHandler(clientSocket, ragSystem));
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
            System.out.println("🛑 RAGWithEmbeddingSQL Server stopped");
        } catch (Exception e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }

    /**
     * Обработчик клиентских соединений для embedding RAG
     */
    private static class EmbeddingClientHandler implements Runnable {
        private Socket clientSocket;
        private RAGWithEmbeddingSystem ragSystem;
        private BufferedReader in;
        private PrintWriter out;

        public EmbeddingClientHandler(Socket socket, RAGWithEmbeddingSystem ragSystem) {
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
                    System.out.println("📨 Received Embedding RAG request");
                    processEmbeddingRequest(request);
                }
            } catch (IOException e) {
                System.err.println("❌ Embedding handler error: " + e.getMessage());
            } finally {
                closeConnection();
            }
        }

        private void processEmbeddingRequest(String request) {
            try {
                JSONObject requestJson = new JSONObject(request);
                String command = requestJson.getString("command");
                JSONObject response = new JSONObject();

                switch (command.toLowerCase()) {
                    case "ping":
                        handlePing(response);
                        break;
                    case "embedding_rag":
                        handleEmbeddingRAG(requestJson, response);
                        break;
                    case "hybrid_rag":
                        handleHybridRAG(requestJson, response);
                        break;
                    case "weighted_hybrid_rag":
                        handleWeightedHybridRAG(requestJson, response);
                        break;
                    case "multi_stage_rag":
                        handleMultiStageRAG(requestJson, response);
                        break;
                    case "strategic_rag":
                        handleStrategicRAG(requestJson, response);
                        break;
                    case "performance_test":
                        handlePerformanceTest(requestJson, response);
                        break;
                    case "get_embedding_stats":
                        handleGetEmbeddingStats(response);
                        break;
                    default:
                        response.put("status", "error");
                        response.put("message", "Unknown embedding command: " + command);
                }

                out.println(response.toString());
                System.out.println("📤 Sent Embedding RAG response for: " + command);

            } catch (Exception e) {
                JSONObject errorResponse = new JSONObject();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Embedding processing error: " + e.getMessage());
                out.println(errorResponse.toString());
                System.err.println("❌ Embedding processing error: " + e.getMessage());
            }
        }

        private void handlePing(JSONObject response) {
            response.put("status", "success");
            response.put("message", "RAGWithEmbeddingSQL Server is running");
            response.put("timestamp", System.currentTimeMillis());
            response.put("server_type", "RAGWithEmbeddingSQL");
            response.put("embedding_search", true);
        }

        private void handleEmbeddingRAG(JSONObject request, JSONObject response) throws Exception {
            String question = request.getString("question");
            int topK = request.optInt("top_k", 4);
            double threshold = request.optDouble("threshold", 0.7);

            RAGResult result = ragSystem.executeEmbeddingRAGQuery(question, topK, threshold);

            response.put("status", "success");
            response.put("question", question);
            response.put("generated_response", result.getGeneratedResponse());
            response.put("retrieved_documents", new JSONArray(result.getRetrievedDocuments()));
            response.put("search_strategy", result.getMetadata().optString("search_strategy", "embedding"));
            response.put("search_time_ms", result.getMetadata().optLong("search_time_ms", 0));
        }

        private void handleHybridRAG(JSONObject request, JSONObject response) throws Exception {
            String question = request.getString("question");
            int topK = request.optInt("top_k", 6);
            double embeddingThreshold = request.optDouble("embedding_threshold", 0.7);
            double keywordThreshold = request.optDouble("keyword_threshold", 0.5);

            RAGResult result = ragSystem.executeHybridRAGQuery(question, topK, embeddingThreshold, keywordThreshold);

            response.put("status", "success");
            response.put("question", question);
            response.put("generated_response", result.getGeneratedResponse());
            response.put("retrieved_documents", new JSONArray(result.getRetrievedDocuments()));
            response.put("search_strategy", "hybrid");
            response.put("embedding_results", result.getMetadata().optInt("embedding_results", 0));
            response.put("keyword_results", result.getMetadata().optInt("keyword_results", 0));
        }

        private void handleWeightedHybridRAG(JSONObject request, JSONObject response) throws Exception {
            String question = request.getString("question");
            int topK = request.optInt("top_k", 5);
            double embeddingWeight = request.optDouble("embedding_weight", 0.7);
            double keywordWeight = request.optDouble("keyword_weight", 0.3);

            RAGResult result = ragSystem.executeWeightedHybridRAGQuery(question, topK, embeddingWeight, keywordWeight);

            response.put("status", "success");
            response.put("question", question);
            response.put("generated_response", result.getGeneratedResponse());
            response.put("retrieved_documents", new JSONArray(result.getRetrievedDocuments()));
            response.put("search_strategy", "weighted_hybrid");
            response.put("embedding_weight", embeddingWeight);
            response.put("keyword_weight", keywordWeight);
        }

        private void handleMultiStageRAG(JSONObject request, JSONObject response) throws Exception {
            String question = request.getString("question");
            int topK = request.optInt("top_k", 4);

            RAGResult result = ragSystem.executeMultiStageRAGQuery(question, topK);

            response.put("status", "success");
            response.put("question", question);
            response.put("generated_response", result.getGeneratedResponse());
            response.put("retrieved_documents", new JSONArray(result.getRetrievedDocuments()));
            response.put("search_strategy", "multi_stage");
            response.put("stages", result.getMetadata().optInt("stages", 3));
        }

        private void handleStrategicRAG(JSONObject request, JSONObject response) throws Exception {
            String question = request.getString("question");
            String strategyType = request.optString("strategy_type", "auto");

            SearchStrategy strategy;
            if ("auto".equals(strategyType)) {
                strategy = ragSystem.analyzeQuestion(question);
            } else {
                strategy = createStrategyFromRequest(request);
            }

            RAGResult result = ragSystem.executeStrategicRAGQuery(question, strategy);

            response.put("status", "success");
            response.put("question", question);
            response.put("generated_response", result.getGeneratedResponse());
            response.put("retrieved_documents", new JSONArray(result.getRetrievedDocuments()));
            response.put("strategy_used", strategy.getName());
            response.put("strategy_parameters", strategy.getParameters());
        }

        private void handlePerformanceTest(JSONObject request, JSONObject response) throws Exception {
            String question = request.getString("question");
            int iterations = request.optInt("iterations", 3);

            JSONArray performanceResults = new JSONArray();

            for (int i = 0; i < iterations; i++) {
                long startTime = System.currentTimeMillis();
                RAGResult result = ragSystem.executeEmbeddingRAGQuery(question, 5, 0.7);
                long endTime = System.currentTimeMillis();

                performanceResults.put(new JSONObject()
                        .put("iteration", i + 1)
                        .put("search_time_ms", endTime - startTime)
                        .put("documents_found", result.getRetrievedDocuments().size())
                        .put("strategy", result.getMetadata().optString("search_strategy", "embedding"))
                );
            }

            response.put("status", "success");
            response.put("question", question);
            response.put("iterations", iterations);
            response.put("performance_results", performanceResults);
        }

        private void handleGetEmbeddingStats(JSONObject response) {
            response.put("status", "success");
            response.put("server_type", "RAGWithEmbeddingSQL");
            response.put("embedding_capabilities", new JSONArray()
                    .put("embedding_search")
                    .put("hybrid_search")
                    .put("weighted_hybrid_search")
                    .put("multi_stage_search")
                    .put("strategic_search")
                    .put("performance_testing")
            );
            response.put("indexes_created", true);
        }

        private SearchStrategy createStrategyFromRequest(JSONObject request) {
            SearchStrategy strategy = new SearchStrategy();
            strategy.setType(request.getString("strategy_type"));
            strategy.setTopK(request.optInt("top_k", 3));
            strategy.setSimilarityThreshold(request.optDouble("similarity_threshold", 0.7));
            strategy.setEmbeddingThreshold(request.optDouble("embedding_threshold", 0.7));
            strategy.setKeywordThreshold(request.optDouble("keyword_threshold", 0.5));
            strategy.setEmbeddingWeight(request.optDouble("embedding_weight", 0.7));
            strategy.setKeywordWeight(request.optDouble("keyword_weight", 0.3));
            return strategy;
        }

        private void closeConnection() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
                System.out.println("🔌 Client disconnected: " + clientSocket.getInetAddress());
            } catch (IOException e) {
                System.err.println("Error closing embedding connection: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        try {
            int port = args.length > 0 ? Integer.parseInt(args[0]) : 9091;
            String databasePath = args.length > 1 ? args[1] : "./data/rag_embedding_server";

            RAGWithEmbeddingSQLServer server = new RAGWithEmbeddingSQLServer(port, databasePath);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n🛑 Shutting down RAGWithEmbeddingSQL Server...");
                server.stop();
            }));

            server.start();

        } catch (Exception e) {
            System.err.println("❌ Failed to start RAGWithEmbeddingSQL Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}