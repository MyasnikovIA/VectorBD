package ru.miacomsoft.vectordb.core;

import org.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class TreeNode implements Serializable {
    private static final long serialVersionUID = 3L;

    private Object data;
    private final Map<Object, TreeNode> children;
    private transient Map<String, Object> pathCache;
    private Map<String, Object> metadata;

    public TreeNode() {
        this.children = new HashMap<>();
        this.metadata = new HashMap<>();
    }

    public TreeNode(Object data) {
        this();
        this.data = data;
    }

    public void setNode(Object[] path, Object value) {
        setNode(path, 0, value);
        clearPathCache();
    }

    private void setNode(Object[] path, int index, Object value) {
        if (index == path.length) {
            this.data = value;
            return;
        }

        Object key = path[index];
        TreeNode child = children.computeIfAbsent(key, k -> new TreeNode());
        child.setNode(path, index + 1, value);
    }

    public Object getNode(Object[] path) {
        if (pathCache == null) {
            pathCache = new HashMap<>();
        }

        String cacheKey = buildCacheKey(path);
        Object cached = pathCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Object result = getNode(path, 0);
        if (result != null) {
            pathCache.put(cacheKey, result);
        }
        return result;
    }

    private Object getNode(Object[] path, int index) {
        if (index == path.length) {
            return data;
        }

        Object key = path[index];
        TreeNode child = children.get(key);
        return child != null ? child.getNode(path, index + 1) : null;
    }

    public void removeNode(Object[] path) {
        removeNode(path, 0);
        clearPathCache();
    }

    private boolean removeNode(Object[] path, int index) {
        if (index == path.length) {
            data = null;
            return children.isEmpty();
        }

        Object key = path[index];
        TreeNode child = children.get(key);
        if (child == null) {
            return false;
        }

        boolean shouldRemoveChild = child.removeNode(path, index + 1);
        if (shouldRemoveChild) {
            children.remove(key);
        }

        return data == null && children.isEmpty();
    }

    public List<QueryResult> query(Object[] path, int depth) {
        List<QueryResult> results = new ArrayList<>();
        query(path, 0, new ArrayList<>(), depth, results);
        return results;
    }

    private void query(Object[] path, int index, List<Object> currentPath, int depth, List<QueryResult> results) {
        if (index >= path.length) {
            if (data != null) {
                results.add(new QueryResult(currentPath.toArray(), data));
            }
            if (depth > 0) {
                for (Map.Entry<Object, TreeNode> entry : children.entrySet()) {
                    List<Object> newPath = new ArrayList<>(currentPath);
                    newPath.add(entry.getKey());
                    entry.getValue().query(new Object[0], 0, newPath, depth - 1, results);
                }
            }
            return;
        }
        Object key = path[index];
        TreeNode child = children.get(key);
        if (child != null) {
            List<Object> newPath = new ArrayList<>(currentPath);
            newPath.add(key);
            child.query(path, index + 1, newPath, depth, results);
        }
    }

    public int countNodes() {
        int count = data != null ? 1 : 0;
        for (TreeNode child : children.values()) {
            count += child.countNodes();
        }
        return count;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
        clearPathCache();
    }

    public Map<Object, TreeNode> getChildren() {
        return Collections.unmodifiableMap(children);
    }

    public Set<Object> getChildKeys() {
        return children.keySet();
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public boolean isEmpty() {
        return data == null && children.isEmpty();
    }

    public Map<List<Object>, Object> getAllPaths() {
        Map<List<Object>, Object> paths = new HashMap<>();
        getAllPaths(new ArrayList<>(), paths);
        return paths;
    }

    private void getAllPaths(List<Object> currentPath, Map<List<Object>, Object> paths) {
        if (data != null) {
            paths.put(new ArrayList<>(currentPath), data);
        }
        for (Map.Entry<Object, TreeNode> entry : children.entrySet()) {
            List<Object> newPath = new ArrayList<>(currentPath);
            newPath.add(entry.getKey());
            entry.getValue().getAllPaths(newPath, paths);
        }
    }

    public List<Map.Entry<List<Object>, Object>> findValues(Object targetValue) {
        List<Map.Entry<List<Object>, Object>> results = new ArrayList<>();
        findValues(new ArrayList<>(), targetValue, results);
        return results;
    }

    private void findValues(List<Object> currentPath, Object targetValue,
                            List<Map.Entry<List<Object>, Object>> results) {
        if (data != null && data.equals(targetValue)) {
            results.add(new AbstractMap.SimpleEntry<>(new ArrayList<>(currentPath), data));
        }
        for (Map.Entry<Object, TreeNode> entry : children.entrySet()) {
            List<Object> newPath = new ArrayList<>(currentPath);
            newPath.add(entry.getKey());
            entry.getValue().findValues(newPath, targetValue, results);
        }
    }

    public List<Map.Entry<List<Object>, Object>> findValuesByPattern(String pattern) {
        List<Map.Entry<List<Object>, Object>> results = new ArrayList<>();
        findValuesByPattern(new ArrayList<>(), pattern, results);
        return results;
    }

    private void findValuesByPattern(List<Object> currentPath, String pattern,
                                     List<Map.Entry<List<Object>, Object>> results) {
        if (data != null && data.toString().toLowerCase().contains(pattern.toLowerCase())) {
            results.add(new AbstractMap.SimpleEntry<>(new ArrayList<>(currentPath), data));
        }
        for (Map.Entry<Object, TreeNode> entry : children.entrySet()) {
            List<Object> newPath = new ArrayList<>(currentPath);
            newPath.add(entry.getKey());
            entry.getValue().findValuesByPattern(newPath, pattern, results);
        }
    }

    public void clear() {
        data = null;
        children.clear();
        metadata.clear();
        clearPathCache();
    }

    private void clearPathCache() {
        if (pathCache != null) {
            pathCache.clear();
        }
    }

    private String buildCacheKey(Object[] path) {
        StringBuilder key = new StringBuilder();
        for (Object p : path) {
            key.append(p).append(":");
        }
        return key.toString();
    }

    // Методы для работы с метаданными
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }

    public void removeMetadata(String key) {
        metadata.remove(key);
    }

    public void clearMetadata() {
        metadata.clear();
    }

    // Методы для работы с JSON
    public JSONObject toJson() {
        JSONObject json = new JSONObject();

        if (data != null) {
            json.put("data", data.toString());
        }

        // Сохранение детей
        JSONObject childrenJson = new JSONObject();
        for (Map.Entry<Object, TreeNode> entry : children.entrySet()) {
            childrenJson.put(entry.getKey().toString(), entry.getValue().toJson());
        }
        json.put("children", childrenJson);

        // Сохранение метаданных
        if (!metadata.isEmpty()) {
            JSONObject metadataJson = new JSONObject();
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                metadataJson.put(entry.getKey(), entry.getValue().toString());
            }
            json.put("metadata", metadataJson);
        }

        return json;
    }

    public static TreeNode fromJson(JSONObject json) {
        TreeNode node = new TreeNode();

        if (json.has("data")) {
            node.data = json.getString("data");
        }

        if (json.has("children")) {
            JSONObject childrenJson = json.getJSONObject("children");
            for (String key : childrenJson.keySet()) {
                TreeNode childNode = fromJson(childrenJson.getJSONObject(key));
                node.children.put(key, childNode);
            }
        }

        if (json.has("metadata")) {
            JSONObject metadataJson = json.getJSONObject("metadata");
            for (String key : metadataJson.keySet()) {
                node.metadata.put(key, metadataJson.getString(key));
            }
        }

        return node;
    }

    public String toJsonString() {
        return toJson().toString(2);
    }

    public static TreeNode fromJsonString(String jsonString) {
        JSONObject json = new JSONObject(jsonString);
        return fromJson(json);
    }

    // Дополнительные полезные методы
    public List<Object> getPathToNode(Object targetData) {
        List<Object> path = new ArrayList<>();
        if (findPathToNode(targetData, path)) {
            return path;
        }
        return Collections.emptyList();
    }

    private boolean findPathToNode(Object targetData, List<Object> path) {
        if (Objects.equals(data, targetData)) {
            return true;
        }

        for (Map.Entry<Object, TreeNode> entry : children.entrySet()) {
            path.add(entry.getKey());
            if (entry.getValue().findPathToNode(targetData, path)) {
                return true;
            }
            path.remove(path.size() - 1);
        }

        return false;
    }

    public boolean containsValue(Object value) {
        if (Objects.equals(data, value)) {
            return true;
        }
        for (TreeNode child : children.values()) {
            if (child.containsValue(value)) {
                return true;
            }
        }
        return false;
    }

    public int getDepth() {
        if (children.isEmpty()) {
            return 1;
        }
        int maxChildDepth = 0;
        for (TreeNode child : children.values()) {
            maxChildDepth = Math.max(maxChildDepth, child.getDepth());
        }
        return maxChildDepth + 1;
    }

    public int getWidth() {
        if (children.isEmpty()) {
            return 1;
        }
        int width = 0;
        for (TreeNode child : children.values()) {
            width += child.getWidth();
        }
        return width;
    }

    public List<Object> getLeafValues() {
        List<Object> leafValues = new ArrayList<>();
        collectLeafValues(leafValues);
        return leafValues;
    }

    private void collectLeafValues(List<Object> leafValues) {
        if (children.isEmpty() && data != null) {
            leafValues.add(data);
        }
        for (TreeNode child : children.values()) {
            child.collectLeafValues(leafValues);
        }
    }

    public TreeNode findSubtree(Object[] path) {
        return findSubtree(path, 0);
    }

    private TreeNode findSubtree(Object[] path, int index) {
        if (index == path.length) {
            return this;
        }

        Object key = path[index];
        TreeNode child = children.get(key);
        if (child == null) {
            return null;
        }

        return child.findSubtree(path, index + 1);
    }

    public void mergeWith(TreeNode other) {
        if (other.data != null) {
            this.data = other.data;
        }

        for (Map.Entry<Object, TreeNode> entry : other.children.entrySet()) {
            Object key = entry.getKey();
            TreeNode otherChild = entry.getValue();

            if (this.children.containsKey(key)) {
                this.children.get(key).mergeWith(otherChild);
            } else {
                this.children.put(key, otherChild.deepCopy());
            }
        }

        // Merge metadata
        for (Map.Entry<String, Object> entry : other.metadata.entrySet()) {
            this.metadata.putIfAbsent(entry.getKey(), entry.getValue());
        }

        clearPathCache();
    }

    @Override
    public String toString() {
        return "TreeNode{" +
                "data=" + data +
                ", children=" + children.keySet() +
                ", metadata=" + metadata.keySet() +
                '}';
    }

    public String toTreeString() {
        StringBuilder sb = new StringBuilder();
        buildTreeString(sb, "", true);
        return sb.toString();
    }

    private void buildTreeString(StringBuilder sb, String prefix, boolean isTail) {
        sb.append(prefix).append(isTail ? "└── " : "├── ").append(data).append("\n");

        List<Map.Entry<Object, TreeNode>> entries = new ArrayList<>(children.entrySet());
        for (int i = 0; i < entries.size() - 1; i++) {
            entries.get(i).getValue().buildTreeString(sb, prefix + (isTail ? "    " : "│   "), false);
        }
        if (!entries.isEmpty()) {
            entries.get(entries.size() - 1).getValue().buildTreeString(sb, prefix + (isTail ? "    " : "│   "), true);
        }
    }

    public TreeNode deepCopy() {
        TreeNode copy = new TreeNode(data);
        for (Map.Entry<Object, TreeNode> entry : children.entrySet()) {
            copy.children.put(entry.getKey(), entry.getValue().deepCopy());
        }
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            copy.metadata.put(entry.getKey(), entry.getValue());
        }
        return copy;
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.pathCache = new HashMap<>();
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
    }

    // Класс QueryResult для результатов запроса
    public static class QueryResult {
        private final Object[] path;
        private final Object value;

        public QueryResult(Object[] path, Object value) {
            this.path = path;
            this.value = value;
        }

        public Object[] getPath() {
            return path;
        }

        public Object getValue() {
            return value;
        }

        public String getPathString() {
            return Arrays.stream(path)
                    .map(Object::toString)
                    .collect(Collectors.joining("."));
        }

        @Override
        public String toString() {
            return "QueryResult{" +
                    "path=" + getPathString() +
                    ", value=" + value +
                    '}';
        }
    }

    // Статические методы утилиты
    public static TreeNode createFromMap(Map<String, Object> map) {
        TreeNode root = new TreeNode();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            root.setNode(new Object[]{entry.getKey()}, entry.getValue());
        }
        return root;
    }

    public static TreeNode createFromPaths(Map<List<Object>, Object> paths) {
        TreeNode root = new TreeNode();
        for (Map.Entry<List<Object>, Object> entry : paths.entrySet()) {
            root.setNode(entry.getKey().toArray(), entry.getValue());
        }
        return root;
    }

    public Map<String, Object> toFlatMap() {
        Map<String, Object> flatMap = new HashMap<>();
        toFlatMap(new ArrayList<>(), flatMap);
        return flatMap;
    }

    private void toFlatMap(List<Object> currentPath, Map<String, Object> flatMap) {
        if (data != null) {
            String key = currentPath.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining("."));
            flatMap.put(key, data);
        }
        for (Map.Entry<Object, TreeNode> entry : children.entrySet()) {
            List<Object> newPath = new ArrayList<>(currentPath);
            newPath.add(entry.getKey());
            entry.getValue().toFlatMap(newPath, flatMap);
        }
    }
}