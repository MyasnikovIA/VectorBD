На основе анализа проекта я создал comprehensive README.md файл:

```markdown
# VectorDB - Векторная база данных с семантическим поиском и RAG

Java-библиотека для работы с векторными базами данных, поддерживающая семантический поиск, чанкинг текста и интеграцию с Ollama для RAG (Retrieval-Augmented Generation) приложений.

# Документация с примерами использования
- [DOCUMENT.md](DOCUMENT.md)

## 🚀 Основные возможности

- **Векторное хранение данных** - эффективное хранение и поиск по векторным эмбеддингам
- **Семантический чанкинг** - интеллектуальное разбиение текста на смысловые фрагменты
- **Древовидная структура данных** - гибкое хранение иерархических данных
- **Интеграция с Ollama** - работа с локальными LLM моделями
- **RAG-функциональность** - генерация ответов с использованием контекста из базы знаний
- **Поиск по схожести** - косинусная схожесть для семантического поиска

## 📁 Структура проекта

### Основные модули

#### Core (`ru.miacomsoft.vectordb.core`)
- `VectorDatabase` - основная база данных векторов
- `SemanticChunker` - семантическое разбиение текста на чанки
- `TreeNode` - древовидная структура для хранения данных
- `VectorData` - модель данных для векторных записей
- `VectorIndex` - индекс для быстрого векторного поиска

#### Knowledge (`ru.miacomsoft.vectordb.knowledge`)
- `KnowledgeLoader` - загрузчик текстовых знаний в базу
- `OllamaKnowledgeClient` - клиент для работы с Ollama
- `PromptGenerator` - генератор промптов для AI
- `KnowledgeConfig` - конфигурация системы знаний

#### Demo (`ru.miacomsoft.vectordb.demo`)
- Примеры использования всех компонентов системы

## 🛠️ Быстрый старт

### Предварительные требования

1. **Java 11+**
2. **Ollama** (для работы с LLM)
3. **Maven/Gradle** для сборки

### Установка Ollama

```bash
# Установка Ollama (Linux/Mac)
curl -fsSL https://ollama.ai/install.sh | sh

# Загрузка модели
ollama pull llama3.2
ollama pull all-minilm:22m
```

### Базовая настройка

```java
// Инициализация SemanticChunker
SemanticChunker semanticChunker = new SemanticChunker(
    "http://localhost:11434",
    "all-minilm:22m",
    0.8  // порог схожести
);

// Создание векторной базы данных
VectorDatabase vectorDB = new VectorDatabase("./data/vectordb", semanticChunker);

// Загрузка текста с семантическим чанкингом
vectorDB.storeTextWithChunking(
    "Ваш текст для индексации...",
    "doc_001", 
    new Object[]{"documents", "category"}
);

// Семантический поиск
List<VectorSearchResult> results = vectorDB.similaritySearch(
    "поисковый запрос", 5
);
```

## 📚 Примеры использования

### 1. Семантический поиск

```java
// Поиск похожих документов
List<VectorSearchResult> results = vectorDB.similaritySearch(
    "машинное обучение", 3
);

for (VectorSearchResult result : results) {
    System.out.printf("Схожесть: %.4f - %s%n", 
        result.getSimilarity(), 
        result.getVectorData().getText()
    );
}
```

### 2. Загрузка знаний

```java
KnowledgeConfig config = new KnowledgeConfig(
    "http://localhost:11434",
    "llama3.2",
    0.8,
    true,
    true
);

KnowledgeLoader loader = new KnowledgeLoader(vectorDB, config);

// Загрузка текста
int chunks = loader.loadText(
    "Текст с знаниями...",
    "knowledge_base",
    new Object[]{"knowledge", "ai"},
    500,
    "source_name"
);
```

### 3. Генерация ответов с RAG

```java
OllamaKnowledgeClient client = new OllamaKnowledgeClient(vectorDB, config);

// Генерация ответа с использованием знаний
String response = client.generateResponseWithKnowledge(
    "Что такое машинное обучение?"
);

// Интерактивный чат
client.startInteractiveChat();
```

### 4. Работа с древовидными структурами

```java
TreeNode root = new TreeNode();

// Установка значений по пути
root.setNode(new Object[]{"users", "user1", "name"}, "Иван Иванов");
root.setNode(new Object[]{"users", "user1", "age"}, 30);

// Получение значений
String name = (String) root.getNode(new Object[]{"users", "user1", "name"});

// Поиск по дереву
List<TreeNode.QueryResult> results = root.query(
    new Object[]{"users"}, 
    2  // глубина поиска
);
```

## 🔧 Конфигурация

### Настройка SemanticChunker

```java
SemanticChunker chunker = new SemanticChunker(
    "http://localhost:11434",  // URL Ollama
    "all-minilm:22m",          // модель для эмбеддингов
    0.8                        // порог схожести (0.0-1.0)
);

// Настройка порога
chunker.setSimilarityThreshold(0.7);
```

### Конфигурация Knowledge системы

```java
KnowledgeConfig config = new KnowledgeConfig(
    "http://localhost:11434",  // Ollama URL
    "llama3.2",               // модель для генерации
    0.8,                      // порог схожести
    true,                     // сохранять историю
    true                      // включить функциональность
);
```

## 🎯 Демонстрационные примеры

Проект включает comprehensive демо-классы:

- `VectorDBExample` - базовое использование векторной БД
- `EmbeddingDemo` - работа с эмбеддингами
- `KnowledgeDemo` - загрузка и использование знаний
- `OllamaChatDemo` - интерактивный чат с RAG
- `PromptGeneratorDemo` - генерация промптов
- `AdvancedPromptDemo` - продвинутые сценарии

Запуск демо:
```bash
java -cp vectordb.jar ru.miacomsoft.vectordb.demo.VectorDBExample
```

## 📊 Метрики и производительность

- **Косинусная схожесть** для семантического поиска
- **Автоматическое чанкинг** с настраиваемым порогом
- **Эффективное индексирование** для быстрого поиска
- **Поддержка больших объемов** данных через древовидные структуры

## 🔮 Расширенные возможности

### Кастомные стратегии чанкинга

```java
// Настройка семантического чанкинга
chunker.setSimilarityThreshold(0.6);  // более агрессивное разбиение
List<Chunk> chunks = chunker.semanticChunking(text, 1000);
```

### Многомодельная работа

```java
// Разные модели для эмбеддингов и генерации
SemanticChunker chunker = new SemanticChunker(
    "http://localhost:11434", 
    "all-minilm:22m",  // для эмбеддингов
    0.8
);

KnowledgeConfig config = new KnowledgeConfig(
    "http://localhost:11434",
    "llama3.2",        // для генерации
    0.8, true, true
);
```

## 🐛 Устранение неполадок

### Общие проблемы

1. **Ollama недоступен**
   ```bash
   # Проверка статуса
   curl http://localhost:11434/api/tags
   ```

2. **Модели не загружены**
   ```bash
   # Загрузка необходимых моделей
   ollama pull llama3.2
   ollama pull all-minilm:22m
   ```

3. **Проблемы с памятью**
    - Увеличьте heap size: `-Xmx4G`
    - Настройте размер чанков

### Логирование и отладка

```java
// Включение debug информации
System.out.println("Database stats: " + vectorDB.getVectorCount());
System.out.println("Chunker config: " + chunker.getConfigInfo());
```

## 📄 Лицензия

[Указать лицензию проекта]

## 🤝 Вклад в проект

Приветствуются issues и pull requests!

## 📞 Поддержка

Для вопросов и предложений:
- Создайте issue в репозитории
- Опишите проблему с примерами кода и логами

---

**Примечание**: Для полной функциональности требуется запущенный Ollama сервер с установленными моделями.
```

Этот README.md предоставляет:

1. **Полное описание** возможностей системы
2. **Быстрый старт** для новых пользователей
3. **Подробные примеры** использования всех компонентов
4. **Конфигурационные руководства**
5. **Демонстрационные сценарии**
6. **Решение проблем** и отладку

Проект представляет собой полнофункциональную систему для работы с векторными базами данных и RAG-приложениями на Java.