package ru.miacomsoft.vectordb.demo;

import ru.miacomsoft.vectordb.core.*;
import ru.miacomsoft.vectordb.knowledge.KnowledgeConfig;
import ru.miacomsoft.vectordb.knowledge.OllamaKnowledgeClient;
import org.json.JSONObject;
import org.json.JSONArray;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Сервер для OllamaChatDemo с поддержкой интерактивного чата
 * # Terminal 1 - RAGSQL Server
 * java ru.miacomsoft.vectordb.demo.RAGSQLServer 9090 ./data/ragsql_server
 *
 * # Terminal 2 - RAGWithEmbeddingSQL Server
 * java ru.miacomsoft.vectordb.demo.RAGWithEmbeddingSQLServer 9091 ./data/rag_embedding_server
 *
 * # Terminal 3 - OllamaChat Server
 * java ru.miacomsoft.vectordb.demo.OllamaChatServer 9092 ./data/ollama_chat_server
 *
 */
public class OllamaChatServer {
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private BinaryVectorDatabase vectorDB;
    private OllamaKnowledgeClient chatClient;
    private boolean isRunning;
    private int port;

    public OllamaChatServer(int port, String databasePath) throws Exception {
        this.port = port;

        // Инициализация конфигурации
        KnowledgeConfig knowledgeConfig = new KnowledgeConfig(
                "http://localhost:11434",
                "deepseek-v3.1:671b-cloud",
                0.7,
                true,
                true
        );

        // Инициализация базы данных
        SemanticChunker semanticChunker = new SemanticChunker(
                knowledgeConfig.getOllamaUrl(),
                "all-minilm:22m",
                knowledgeConfig.getSimilarityThreshold()
        );
        this.vectorDB = new BinaryVectorDatabase(databasePath, semanticChunker);

        // Инициализация чат-клиента
        this.chatClient = new OllamaKnowledgeClient(vectorDB, knowledgeConfig);

        // Загрузка начальных знаний
        loadInitialKnowledge();

        this.serverSocket = new ServerSocket(port);
        this.threadPool = Executors.newFixedThreadPool(10);
        this.isRunning = true;

        System.out.println("=== OllamaChat Server ===");
        System.out.println("Port: " + port);
        System.out.println("Database: " + databasePath);
        System.out.println("Ollama URL: " + knowledgeConfig.getOllamaUrl());
        System.out.println("Default Model: " + knowledgeConfig.getModel());
    }

    /**
     * Загрузка начальных знаний
     */
    private void loadInitialKnowledge() throws Exception {
        if (vectorDB.getVectorCount() == 0) {
            System.out.println("Loading initial knowledge for chat...");

            String initialFacts = """
                Искусственный интеллект - это область компьютерных наук, 
                которая занимается созданием машин, способных выполнять задачи, 
                требующие человеческого интеллекта.
                
                Машинное обучение является подразделом искусственного интеллекта 
                и фокусируется на разработке алгоритмов, которые могут обучаться на данных.
                
                Глубокое обучение использует нейронные сети с множеством слоев 
                для изучения сложных паттернов в данных.
                
                ИИ (искусственный интеллект) - это способность машин имитировать 
                человеческий интеллект и выполнять задачи, которые обычно требуют 
                человеческого мышления.
                
                Python - популярный язык программирования для data science и машинного обучения.
                Java - объектно-ориентированный язык с сильной типизацией, используется в enterprise.
                SQL - язык для работы с реляционными базами данных.
                Векторные базы данных хранят embedding векторы для семантического поиска.
                """;

            vectorDB.storeTextWithChunking(
                    initialFacts,
                    "ChatKnowledge",
                    new Object[]{"knowledge", "chat", "initial"}
            );

            System.out.println("Initial knowledge loaded: " + vectorDB.getVectorCount() + " vectors");
        }
    }

    public void start() {
        System.out.println("🚀 Server listening on port " + port + "...");

        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("📡 New chat client: " + clientSocket.getInetAddress());
                threadPool.execute(new ChatClientHandler(clientSocket, chatClient, vectorDB));
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("❌ Chat client connection error: " + e.getMessage());
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
            System.out.println("🛑 OllamaChat Server stopped");
        } catch (Exception e) {
            System.err.println("Error stopping chat server: " + e.getMessage());
        }
    }

    /**
     * Обработчик клиентских соединений для чата
     */
    private static class ChatClientHandler implements Runnable {
        private Socket clientSocket;
        private OllamaKnowledgeClient chatClient;
        private BinaryVectorDatabase vectorDB;
        private BufferedReader in;
        private PrintWriter out;

        public ChatClientHandler(Socket socket, OllamaKnowledgeClient chatClient, BinaryVectorDatabase vectorDB) {
            this.clientSocket = socket;
            this.chatClient = chatClient;
            this.vectorDB = vectorDB;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                String request;
                while ((request = in.readLine()) != null) {
                    System.out.println("📨 Received chat request");
                    processChatRequest(request);
                }
            } catch (IOException e) {
                System.err.println("❌ Chat handler error: " + e.getMessage());
            } finally {
                closeConnection();
            }
        }

        private void processChatRequest(String request) {
            try {
                JSONObject requestJson = new JSONObject(request);
                String command = requestJson.getString("command");
                JSONObject response = new JSONObject();

                switch (command.toLowerCase()) {
                    case "ping":
                        handlePing(response);
                        break;
                    case "chat_message":
                        handleChatMessage(requestJson, response);
                        break;
                    case "chat_with_knowledge":
                        handleChatWithKnowledge(requestJson, response);
                        break;
                    case "stream_chat":
                        handleStreamChat(requestJson, response);
                        break;
                    case "add_knowledge":
                        handleAddKnowledge(requestJson, response);
                        break;
                    case "search_knowledge":
                        handleSearchKnowledge(requestJson, response);
                        break;
                    case "get_chat_stats":
                        handleGetChatStats(response);
                        break;
                    case "test_connections":
                        handleTestConnections(response);
                        break;
                    case "list_models":
                        handleListModels(response);
                        break;
                    case "set_model":
                        handleSetModel(requestJson, response);
                        break;
                    default:
                        response.put("status", "error");
                        response.put("message", "Unknown chat command: " + command);
                }

                out.println(response.toString());
                System.out.println("📤 Sent chat response for: " + command);

            } catch (Exception e) {
                JSONObject errorResponse = new JSONObject();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Chat processing error: " + e.getMessage());
                out.println(errorResponse.toString());
                System.err.println("❌ Chat processing error: " + e.getMessage());
            }
        }

        private void handlePing(JSONObject response) {
            response.put("status", "success");
            response.put("message", "OllamaChat Server is running");
            response.put("timestamp", System.currentTimeMillis());
            response.put("server_type", "OllamaChat");
            response.put("vector_count", vectorDB.getVectorCount());
        }

        private void handleChatMessage(JSONObject request, JSONObject response) {
            String message = request.getString("message");

            try {
                String aiResponse = chatClient.generateResponse(message);

                response.put("status", "success");
                response.put("user_message", message);
                response.put("ai_response", aiResponse);
                response.put("response_type", "direct");

            } catch (Exception e) {
                response.put("status", "error");
                response.put("message", "Chat generation error: " + e.getMessage());
            }
        }

        private void handleChatWithKnowledge(JSONObject request, JSONObject response) {
            String message = request.getString("message");
            int maxContext = request.optInt("max_context", 3);
            double threshold = request.optDouble("threshold", 0.7);

            try {
                // Устанавливаем параметры для поиска знаний
                chatClient.setMaxContextResults(maxContext);
                chatClient.setSimilarityThreshold(threshold);

                // Генерация ответа с использованием знаний
                Map<String, Object> result = chatClient.generateResponseWithStats(message);

                response.put("status", "success");
                response.put("user_message", message);
                response.put("ai_response", result.get("response"));
                response.put("response_type", "knowledge_enhanced");
                response.put("context_facts", new JSONArray((List<String>) result.get("contextFacts")));
                response.put("similarities", new JSONArray((List<Double>) result.get("similarities")));
                response.put("total_facts", result.get("totalFacts"));

            } catch (Exception e) {
                response.put("status", "error");
                response.put("message", "Knowledge-enhanced chat error: " + e.getMessage());
            }
        }

        private void handleStreamChat(JSONObject request, JSONObject response) {
            String message = request.getString("message");
            boolean useKnowledge = request.optBoolean("use_knowledge", true);

            try {
                // Для потокового чата отправляем специальный ответ
                // В реальной реализации здесь будет потоковая передача
                String aiResponse;
                if (useKnowledge) {
                    aiResponse = chatClient.generateResponseWithKnowledge(message);
                } else {
                    aiResponse = chatClient.generateResponse(message);
                }

                response.put("status", "success");
                response.put("user_message", message);
                response.put("ai_response", aiResponse);
                response.put("response_type", "stream_simulated");
                response.put("use_knowledge", useKnowledge);

            } catch (Exception e) {
                response.put("status", "error");
                response.put("message", "Stream chat error: " + e.getMessage());
            }
        }

        private void handleAddKnowledge(JSONObject request, JSONObject response) {
            String text = request.getString("text");
            String documentId = request.optString("document_id", "user_added_" + System.currentTimeMillis());
            String category = request.optString("category", "user_knowledge");

            try {
                vectorDB.storeTextWithChunking(
                        text,
                        documentId,
                        new Object[]{"knowledge", category, "user_added"}
                );

                response.put("status", "success");
                response.put("message", "Knowledge added successfully");
                response.put("document_id", documentId);
                response.put("vector_count", vectorDB.getVectorCount());

            } catch (Exception e) {
                response.put("status", "error");
                response.put("message", "Knowledge addition error: " + e.getMessage());
            }
        }

        private void handleSearchKnowledge(JSONObject request, JSONObject response) {
            String query = request.getString("query");
            int maxResults = request.optInt("max_results", 5);

            try {
                List<Map<String, Object>> results = chatClient.findSimilarDocuments(query, maxResults);

                response.put("status", "success");
                response.put("query", query);
                response.put("results", new JSONArray(results));
                response.put("total_found", results.size());

            } catch (Exception e) {
                response.put("status", "error");
                response.put("message", "Knowledge search error: " + e.getMessage());
            }
        }

        private void handleGetChatStats(JSONObject response) {
            try {
                Map<String, Object> dbStats = chatClient.getDatabaseStats();
                Map<String, Boolean> connTests = chatClient.testConnections();

                response.put("status", "success");
                response.put("database_stats", new JSONObject(dbStats));
                response.put("connection_tests", new JSONObject(connTests));
                response.put("vector_count", vectorDB.getVectorCount());
                response.put("server_uptime", System.currentTimeMillis());

            } catch (Exception e) {
                response.put("status", "error");
                response.put("message", "Stats retrieval error: " + e.getMessage());
            }
        }

        private void handleTestConnections(JSONObject response) {
            try {
                Map<String, Boolean> tests = chatClient.testConnections();

                response.put("status", "success");
                response.put("connection_tests", new JSONObject(tests));

                // Проверка доступности моделей
                List<String> models = chatClient.getOllamaClient().getAvailableModels();
                response.put("available_models", new JSONArray(models));

            } catch (Exception e) {
                response.put("status", "error");
                response.put("message", "Connection test error: " + e.getMessage());
            }
        }

        private void handleListModels(JSONObject response) {
            try {
                List<String> models = chatClient.getOllamaClient().getAvailableModels();
                String currentModel = chatClient.getKnowledgeConfig().getModel();

                response.put("status", "success");
                response.put("available_models", new JSONArray(models));
                response.put("current_model", currentModel);
                response.put("models_count", models.size());

            } catch (Exception e) {
                response.put("status", "error");
                response.put("message", "Model listing error: " + e.getMessage());
            }
        }

        private void handleSetModel(JSONObject request, JSONObject response) {
            String model = request.getString("model");

            try {
                chatClient.setDefaultModel(model);

                response.put("status", "success");
                response.put("message", "Model set to: " + model);
                response.put("new_model", model);

            } catch (Exception e) {
                response.put("status", "error");
                response.put("message", "Model setting error: " + e.getMessage());
            }
        }

        private void closeConnection() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null) clientSocket.close();
                System.out.println("🔌 Chat client disconnected: " + clientSocket.getInetAddress());
            } catch (IOException e) {
                System.err.println("Error closing chat connection: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        try {
            int port = args.length > 0 ? Integer.parseInt(args[0]) : 9092;
            String databasePath = args.length > 1 ? args[1] : "./data/ollama_chat_server";

            OllamaChatServer server = new OllamaChatServer(port, databasePath);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n🛑 Shutting down OllamaChat Server...");
                server.stop();
            }));

            server.start();

        } catch (Exception e) {
            System.err.println("❌ Failed to start OllamaChat Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}