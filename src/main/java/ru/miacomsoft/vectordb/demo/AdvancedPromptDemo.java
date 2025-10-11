package ru.miacomsoft.vectordb.demo;

import ru.miacomsoft.vectordb.core.BinaryVectorDatabase;
import ru.miacomsoft.vectordb.core.SemanticChunker;

import ru.miacomsoft.vectordb.knowledge.KnowledgeConfig;
import ru.miacomsoft.vectordb.knowledge.KnowledgeLoader;
import ru.miacomsoft.vectordb.knowledge.PromptGenerator;

import java.util.Arrays;

/**
 * Продвинутая демонстрация PromptGenerator с реальными сценариями использования бинарной БД
 */
public class AdvancedPromptDemo {
    public static void main(String[] args) {
        System.out.println("=== Advanced Binary PromptGenerator Demo ===");

        try {
            // Инициализация бинарной VectorDatabase
            SemanticChunker semanticChunker = new SemanticChunker(
                    "http://localhost:11434",
                    "all-minilm:22m",
                    0.8
            );
            BinaryVectorDatabase vectorDB = new BinaryVectorDatabase("./data/binary_vectordb", semanticChunker);

            // Создание конфигурации знаний
            KnowledgeConfig knowledgeConfig = new KnowledgeConfig(
                    "http://localhost:11434",
                    "deepseek-v3.1:671b-cloud",
                    0.8,
                    true,
                    true
            );

            PromptGenerator promptGenerator = new PromptGenerator(vectorDB, knowledgeConfig);
            loadRealWorldKnowledge(vectorDB, knowledgeConfig);

            // Реальные сценарии использования с бинарной БД
            demonstrateRealWorldScenarios(promptGenerator);
            demonstratePromptOptimization(promptGenerator);
            demonstrateMultiSourceKnowledge(promptGenerator);

        } catch (Exception e) {
            System.out.println("Advanced binary demo error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadRealWorldKnowledge(BinaryVectorDatabase vectorDB, KnowledgeConfig knowledgeConfig) throws Exception {
        KnowledgeLoader loader = new KnowledgeLoader(vectorDB, knowledgeConfig);

        // Документация по программированию
        String programmingDocs = """
            Java - объектно-ориентированный язык программирования, разработанный Sun Microsystems.
            Основные особенности: кроссплатформенность, автоматическое управление памятью, многопоточность.
            
            Spring Framework - популярный фреймворк для создания Java приложений.
            Spring Boot упрощает создание standalone приложений с минимальной конфигурацией.
            
            Базы данных MUMPS используются в healthcare системах и финансовых приложениях.
            Глобальное хранилище данных с древовидной структураой.
            
            SQL (Structured Query Language) - язык для работы с реляционными базами данных.
            Основные команды: SELECT, INSERT, UPDATE, DELETE, CREATE, DROP.
            
            REST API - архитектурный стиль для создания веб-сервисов.
            Использует HTTP методы: GET, POST, PUT, DELETE.
            """;

        // Документация по DevOps
        String devopsDocs = """
            Docker - платформа для контейнеризации приложений.
            Контейнеры обеспечивают изоляцию и переносимость приложений.
            
            Kubernetes - система оркестрации контейнеров.
            Автоматизирует развертывание, масштабирование и управление контейнеризированными приложениями.
            
            CI/CD (Continuous Integration/Continuous Deployment) - практика автоматизации сборки и развертывания.
            Jenkins, GitLab CI, GitHub Actions - популярные инструменты CI/CD.
            
            Мониторинг приложений включает сбор метрик, логов и трассировок.
            Prometheus для сбора метрик, Grafana для визуализации, ELK stack для логов.
            """;

        // Загружаем знания в бинарную векторную базу
        loader.loadText(programmingDocs, "ProgrammingDocs", new Object[]{"knowledge", "programming"}, 400, "Binary Programming Docs");
        loader.loadText(devopsDocs, "DevOpsDocs", new Object[]{"knowledge", "devops"}, 400, "Binary DevOps Docs");

        System.out.println("Real-world knowledge loaded successfully to binary database");
    }

    private static void demonstrateRealWorldScenarios(PromptGenerator promptGenerator) throws Exception {
        System.out.println("\n1. Real-world Usage Scenarios with Binary DB");
        System.out.println("=============================================");

        // Сценарий 1: Техническая поддержка с бинарной БД
        String supportQuery = "Как настроить Spring Boot приложение с базой данных?";
        String supportPrompt = promptGenerator.createContextPrompt(
                supportQuery,
                3,      // maxResultsPerChunk
                0.7     // similarityThreshold
        );
        System.out.println("Binary Support Scenario - Documents found: " +
                countDocuments(supportPrompt));

        // Сценарий 2: Обучение новых разработчиков с бинарной БД
        String trainingQuery = "Объясни основы Docker и Kubernetes для начинающих";
        String trainingPrompt = promptGenerator.createContextPrompt(
                trainingQuery,
                4,      // maxResultsPerChunk
                0.6     // similarityThreshold
        );
        System.out.println("Binary Training Scenario - Documents found: " +
                countDocuments(trainingPrompt));

        // Сценарий 3: Техническое интервью с бинарной БД
        String interviewQuery = "Какие вопросы задать на собеседовании Java разработчика?";
        String interviewPrompt = promptGenerator.createQuestionGenerationPrompt(
                "Java programming interview",
                10,     // numQuestions
                0.5     // similarityThreshold
        );
        System.out.println("Binary Interview Scenario - Prompt generated from binary DB");
    }

    private static void demonstratePromptOptimization(PromptGenerator promptGenerator) throws Exception {
        System.out.println("\n2. Binary Prompt Optimization");
        System.out.println("==============================");

        String query = "микросервисы vs монолит";

        // Тестируем разные настройки в бинарной БД
        int[] maxResultsOptions = {1, 3, 5};
        double[] thresholdOptions = {0.5, 0.7, 0.9};

        for (int maxResults : maxResultsOptions) {
            for (double threshold : thresholdOptions) {
                String prompt = promptGenerator.createContextPrompt(
                        query,
                        maxResults,  // maxResultsPerChunk
                        threshold    // similarityThreshold
                );

                int docs = countDocuments(prompt);
                System.out.printf("Binary DB - MaxResults: %d, Threshold: %.1f -> Documents: %d, Length: %d chars%n",
                        maxResults, threshold, docs, prompt.length());
            }
        }
    }

    private static void demonstrateMultiSourceKnowledge(PromptGenerator promptGenerator) throws Exception {
        System.out.println("\n3. Binary Multi-source Knowledge Integration");
        System.out.println("=============================================");

        String complexQuery = "Как построить cloud-native приложение с использованием Java, Docker и Kubernetes?";

        // Используем знания из всей бинарной базы данных
        String integratedPrompt = promptGenerator.createContextPrompt(
                complexQuery,
                5,  // больше результатов для комплексного запроса
                0.6  // более низкий порог для широкого охвата
        );

        System.out.println("Complex query processed with binary database");
        System.out.println("Total documents from all sources in binary DB: " + countDocuments(integratedPrompt));
        System.out.println("Integrated prompt length: " + integratedPrompt.length() + " characters");

        // Показываем статистику по всей бинарной базе знаний
        promptGenerator.printKnowledgeStats();
    }

    private static int countDocuments(String prompt) {
        return (int) Arrays.stream(prompt.split("\n"))
                .filter(line -> line.contains("[Документ"))
                .count();
    }
}