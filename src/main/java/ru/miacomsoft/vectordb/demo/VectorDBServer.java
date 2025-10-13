package ru.miacomsoft.vectordb.demo;

import ru.miacomsoft.vectordb.core.*;
import org.json.JSONObject;
import org.json.JSONArray;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Socket сервер для VectorBD с поддержкой всех основных классов
 */
public class VectorDBServer {
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private BinaryVectorDatabase vectorDB;
    private SemanticChunker semanticChunker;
    private SQLParser sqlParser;
    private boolean isRunning;
    private int port;

    public VectorDBServer(int port, String databasePath) throws Exception {
        this.port = port;

        // Инициализация SemanticChunker
        this.semanticChunker = new SemanticChunker(
                "http://localhost:11434",
                "all-minilm:22m",
                0.8
        );

        // Инициализация бинарной векторной базы данных
        this.vectorDB = new BinaryVectorDatabase(databasePath, semanticChunker);

        // Инициализация SQL парсера
        this.sqlParser = new SQLParser(vectorDB);

        this.serverSocket = new ServerSocket(port);
        this.threadPool = Executors.newFixedThreadPool(10);
        this.isRunning = true;

        System.out.println("VectorDB Server started on port " + port);
        System.out.println("Database path: " + databasePath);
        System.out.println("Vector count: " + vectorDB.getVectorCount());
        System.out.println("Tree nodes count: " + vectorDB.getTreeNodeCount());
    }

    public void start() {
        System.out.println("Server listening on port " + port + "...");

        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                threadPool.execute(new ClientHandler(clientSocket, vectorDB, semanticChunker, sqlParser));
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
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
            vectorDB.close();
            System.out.println("VectorDB Server stopped gracefully");
        } catch (Exception e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }

    /**
     * Обработчик клиентских соединений
     */
    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BinaryVectorDatabase vectorDB;
        private SemanticChunker semanticChunker;
        private SQLParser sqlParser;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket socket, BinaryVectorDatabase vectorDB,
                             SemanticChunker semanticChunker, SQLParser sqlParser) {
            this.clientSocket = socket;
            this.vectorDB = vectorDB;
            this.semanticChunker = semanticChunker;
            this.sqlParser = sqlParser;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                String request;
                while ((request = in.readLine()) != null) {
                    System.out.println("Received request: " + request.substring(0, Math.min(100, request.length())) + "...");
                    processRequest(request);
                }
            } catch (IOException e) {
                System.err.println("Client handler error: " + e.getMessage());
            } finally {
                closeConnection();
            }
        }

        private void processRequest(String request) {
            try {
                JSONObject requestJson = new JSONObject(request);
                String command = requestJson.getString("command");
                JSONObject response = new JSONObject();

                switch (command.toLowerCase()) {
                    case "ping":
                        handlePing(response);
                        break;
                    case "store_text":
                        handleStoreText(requestJson, response);
                        break;
                    case "similarity_search":
                        handleSimilaritySearch(requestJson, response);
                        break;
                    case "exact_search":
                        handleExactSearch(requestJson, response);
                        break;
                    case "semantic_search":
                        handleSemanticSearch(requestJson, response);
                        break;
                    case "hybrid_search":
                        handleHybridSearch(requestJson, response);
                        break;
                    case "get_stats":
                        handleGetStats(response);
                        break;
                    case "get_vector":
                        handleGetVector(requestJson, response);
                        break;
                    case "remove_vector":
                        handleRemoveVector(requestJson, response);
                        break;
                    case "sql_query":
                        handleSQLQuery(requestJson, response);
                        break;
                    case "get_tree_node":
                        handleGetTreeNode(requestJson, response);
                        break;
                    case "search_by_path":
                        handleSearchByPath(requestJson, response);
                        break;
                    case "get_all_vectors":
                        handleGetAllVectors(response);
                        break;
                    default:
                        response.put("status", "error");
                        response.put("message", "Unknown command: " + command);
                }

                out.println(response.toString());
                System.out.println("Sent response for command: " + command);

            } catch (Exception e) {
                JSONObject errorResponse = new JSONObject();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Error processing request: " + e.getMessage());
                out.println(errorResponse.toString());
                System.err.println("Error processing request: " + e.getMessage());
            }
        }

        private void handlePing(JSONObject response) {
            response.put("status", "success");
            response.put("message", "pong");
            response.put("timestamp", System.currentTimeMillis());
            response.put("vector_count", vectorDB.getVectorCount());
            response.put("node_count", vectorDB.getTreeNodeCount());
        }

        private void handleStoreText(JSONObject request, JSONObject response) throws Exception {
            String text = request.getString("text");
            String documentId = request.getString("document_id");
            JSONArray pathArray = request.getJSONArray("path");

            Object[] path = new Object[pathArray.length()];
            for (int i = 0; i < pathArray.length(); i++) {
                path[i] = pathArray.get(i);
            }

            vectorDB.storeTextWithChunking(text, documentId, path);

            response.put("status", "success");
            response.put("message", "Text stored successfully");
            response.put("vector_count", vectorDB.getVectorCount());
            response.put("node_count", vectorDB.getTreeNodeCount());
            response.put("document_id", documentId);
        }

        private void handleSimilaritySearch(JSONObject request, JSONObject response) throws Exception {
            String query = request.getString("query");
            int limit = request.optInt("limit", 5);
            double threshold = request.optDouble("threshold", 0.5);

            List<VectorSearchResult> results = vectorDB.similaritySearch(query, limit);
            JSONArray resultsArray = new JSONArray();

            for (VectorSearchResult result : results) {
                if (result.getSimilarity() >= threshold) {
                    JSONObject resultJson = new JSONObject();
                    resultJson.put("similarity", result.getSimilarity());
                    resultJson.put("distance", result.getDistance());

                    BinaryVectorData vectorData = result.getVectorData();
                    JSONObject vectorJson = vectorDataToJson(vectorData);
                    resultJson.put("vector_data", vectorJson);

                    resultsArray.put(resultJson);
                }
            }

            response.put("status", "success");
            response.put("results", resultsArray);
            response.put("total_found", resultsArray.length());
        }

        private void handleExactSearch(JSONObject request, JSONObject response) {
            String searchText = request.getString("search_text");

            List<BinaryVectorData> results = vectorDB.exactSearch(searchText);
            JSONArray resultsArray = new JSONArray();

            for (BinaryVectorData vectorData : results) {
                resultsArray.put(vectorDataToJson(vectorData));
            }

            response.put("status", "success");
            response.put("results", resultsArray);
            response.put("total_found", resultsArray.length());
        }

        private void handleSemanticSearch(JSONObject request, JSONObject response) throws Exception {
            String query = request.getString("query");
            int limit = request.optInt("limit", 5);

            List<JSONObject> results = sqlParser.semanticSearch(query, limit);

            response.put("status", "success");
            response.put("results", new JSONArray(results));
            response.put("total_found", results.size());
        }

        private void handleHybridSearch(JSONObject request, JSONObject response) throws Exception {
            String query = request.getString("query");
            int limit = request.optInt("limit", 5);

            List<JSONObject> results = sqlParser.hybridSearch(query, limit);

            response.put("status", "success");
            response.put("results", new JSONArray(results));
            response.put("total_found", results.size());
        }

        private void handleGetStats(JSONObject response) {
            response.put("status", "success");
            response.put("vector_count", vectorDB.getVectorCount());
            response.put("tree_node_count", vectorDB.getTreeNodeCount());
            response.put("database_type", "BinaryVectorDatabase");

            // Информация о VectorIndex
            response.put("index_size", "available");

            // Информация о SemanticChunker
            response.put("chunker_model", semanticChunker.getEmbeddingModel());
            response.put("similarity_threshold", semanticChunker.getSimilarityThreshold());
        }

        private void handleGetVector(JSONObject request, JSONObject response) {
            String vectorId = request.getString("vector_id");
            BinaryVectorData vectorData = vectorDB.getVectorData(vectorId);

            if (vectorData != null) {
                response.put("status", "success");
                response.put("vector_data", vectorDataToJson(vectorData));
            } else {
                response.put("status", "error");
                response.put("message", "Vector not found: " + vectorId);
            }
        }

        private void handleRemoveVector(JSONObject request, JSONObject response) {
            String vectorId = request.getString("vector_id");
            vectorDB.removeVectorData(vectorId);

            response.put("status", "success");
            response.put("message", "Vector removed: " + vectorId);
            response.put("vector_count", vectorDB.getVectorCount());
        }

        private void handleSQLQuery(JSONObject request, JSONObject response) {
            String sql = request.getString("sql");

            try {
                List<JSONObject> results = sqlParser.execute(sql);

                response.put("status", "success");
                response.put("results", new JSONArray(results));
                response.put("row_count", results.size());
            } catch (Exception e) {
                response.put("status", "error");
                response.put("message", "SQL execution error: " + e.getMessage());
            }
        }

        private void handleGetTreeNode(JSONObject request, JSONObject response) {
            String nodeId = request.getString("node_id");
            BinaryTreeNode node = vectorDB.getTreeNode(nodeId);

            if (node != null) {
                response.put("status", "success");
                response.put("node_content", node.getContent());
                response.put("node_metadata", new JSONObject(node.getMetadata()));

                // Информация о дочерних узлах
                response.put("has_left", node.getLeft() != null);
                response.put("has_right", node.getRight() != null);
            } else {
                response.put("status", "error");
                response.put("message", "TreeNode not found: " + nodeId);
            }
        }

        private void handleSearchByPath(JSONObject request, JSONObject response) {
            String pathPattern = request.getString("path_pattern");
            List<BinaryVectorData> results = vectorDB.searchByPath(pathPattern);
            JSONArray resultsArray = new JSONArray();

            for (BinaryVectorData vectorData : results) {
                resultsArray.put(vectorDataToJson(vectorData));
            }

            response.put("status", "success");
            response.put("results", resultsArray);
            response.put("total_found", resultsArray.length());
        }

        private void handleGetAllVectors(JSONObject response) {
            List<BinaryVectorData> allVectors = vectorDB.findAllVectorData();
            JSONArray vectorsArray = new JSONArray();

            for (BinaryVectorData vectorData : allVectors) {
                vectorsArray.put(vectorDataToJson(vectorData));
            }

            response.put("status", "success");
            response.put("vectors", vectorsArray);
            response.put("total_vectors", vectorsArray.length());
        }

        private JSONObject vectorDataToJson(BinaryVectorData vectorData) {
            JSONObject json = new JSONObject();
            json.put("id", vectorData.getId());
            json.put("text", vectorData.getText());
            json.put("metadata", vectorData.getMetadata());
            json.put("node_path", vectorData.getNodePath());
            json.put("document_id", vectorData.getDocumentId());
            json.put("chunk_index", vectorData.getChunkIndex());
            json.put("timestamp", vectorData.getTimestamp());

            // Информация о векторе
            float[] vector = vectorData.getVector();
            if (vector != null) {
                json.put("vector_dimensions", vector.length);
                // Для экономии места не отправляем весь вектор
                json.put("has_vector", true);
            } else {
                json.put("has_vector", false);
            }

            return json;
        }

        private void closeConnection() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
                System.out.println("Client disconnected: " + clientSocket.getInetAddress());
            } catch (IOException e) {
                System.err.println("Error closing client connection: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        try {
            int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
            String databasePath = args.length > 1 ? args[1] : "./data/vectordb_server";

            VectorDBServer server = new VectorDBServer(port, databasePath);

            // Добавляем shutdown hook для graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down VectorDB Server...");
                server.stop();
            }));

            // Загрузка демо данных при первом запуске
            loadDemoData(server.vectorDB);

            server.start();

        } catch (Exception e) {
            System.err.println("Failed to start VectorDB Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Загрузка демонстрационных данных при первом запуске
     */
    private static void loadDemoData(BinaryVectorDatabase vectorDB) throws Exception {
        if (vectorDB.getVectorCount() == 0) {
            System.out.println("Loading demo data...");

            String[] demoTexts = {
                    "Машинное обучение - это область искусственного интеллекта, которая позволяет компьютерам обучаться на данных.",
                    "Глубокое обучение использует нейронные сети с множеством слоев для изучения сложных паттернов.",
                    "Векторные базы данных хранят данные в виде эмбеддингов и поддерживают семантический поиск.",
                    "SQL - это язык структурированных запросов для работы с реляционными базами данных.",
                    "Java - объектно-ориентированный язык программирования с сильной типизацией и кроссплатформенностью.",
                    "Python популярен в data science благодаря простоте синтаксиса и богатой экосистеме библиотек.",
                    "Бинарные деревья используются для эффективного хранения и поиска структурированных данных.",
                    "SemanticChunker разбивает текст на семантически связанные чанки для лучшего понимания контекста."
            };

            for (int i = 0; i < demoTexts.length; i++) {
                vectorDB.storeTextWithChunking(
                        demoTexts[i],
                        "demo_doc_" + (i + 1),
                        new Object[]{"demo", "category_" + (i % 3), "doc_" + (i + 1)}
                );
            }

            System.out.println("Demo data loaded: " + demoTexts.length + " documents");
        }
    }
}