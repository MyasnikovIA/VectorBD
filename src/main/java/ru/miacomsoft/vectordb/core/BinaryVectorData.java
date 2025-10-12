package ru.miacomsoft.vectordb.core;

import java.io.*;

public class BinaryVectorData {
    private String id;
    private float[] vector;
    private String text;
    private String metadata;
    private String nodePath;
    private String documentId;
    private long timestamp;
    private int chunkIndex; // Добавляем поле chunkIndex

    public BinaryVectorData() {
        this.timestamp = System.currentTimeMillis();
        this.chunkIndex = -1; // Значение по умолчанию
    }

    public BinaryVectorData(String id, float[] vector, String text, String metadata,
                            String nodePath, String documentId) {
        this();
        this.id = id;
        this.vector = vector;
        this.text = text;
        this.metadata = metadata;
        this.nodePath = nodePath;
        this.documentId = documentId;
    }

    public BinaryVectorData(String id, float[] vector, String text, String metadata,
                            String nodePath, String documentId, int chunkIndex) {
        this(id, vector, text, metadata, nodePath, documentId);
        this.chunkIndex = chunkIndex;
    }

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public float[] getVector() { return vector; }
    public void setVector(float[] vector) { this.vector = vector; }

    // Заменяем getOriginalData() на getMetadata()
    public Object getOriginalData() { return metadata; }
    public void setOriginalData(Object originalData) {
        if (originalData != null) {
            this.metadata = originalData.toString();
        }
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getNodePath() { return nodePath; }
    public void setNodePath(String nodePath) { this.nodePath = nodePath; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    // Добавляем методы для chunkIndex
    public int getChunkIndex() {
        // Если chunkIndex явно не установлен, пытаемся извлечь из ID
        if (chunkIndex == -1 && id != null && id.contains("_chunk_")) {
            try {
                String[] parts = id.split("_chunk_");
                if (parts.length > 1) {
                    return Integer.parseInt(parts[1]);
                }
            } catch (NumberFormatException e) {
                System.err.println("Error parsing chunk index from ID: " + id);
            }
        }
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    // Сериализация в бинарный формат
    public byte[] serialize() throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            // Записываем версию для совместимости
            dos.writeInt(2); // Увеличиваем версию для поддержки chunkIndex

            // Записываем основные поля
            writeString(dos, id);
            writeString(dos, text);
            writeString(dos, metadata);
            writeString(dos, nodePath);
            writeString(dos, documentId);
            dos.writeLong(timestamp);
            dos.writeInt(chunkIndex); // Добавляем chunkIndex

            // Записываем вектор
            if (vector != null) {
                dos.writeInt(vector.length);
                for (float value : vector) {
                    dos.writeFloat(value);
                }
            } else {
                dos.writeInt(0);
            }

            return baos.toByteArray();
        }
    }

    // Десериализация из бинарного формата
    public static BinaryVectorData deserialize(byte[] data) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             DataInputStream dis = new DataInputStream(bais)) {

            BinaryVectorData vectorData = new BinaryVectorData();

            int version = dis.readInt();
            if (version < 1 || version > 2) {
                throw new IOException("Unsupported BinaryVectorData version: " + version);
            }

            vectorData.id = readString(dis);
            vectorData.text = readString(dis);
            vectorData.metadata = readString(dis);
            vectorData.nodePath = readString(dis);
            vectorData.documentId = readString(dis);
            vectorData.timestamp = dis.readLong();

            // Читаем chunkIndex только для версии 2+
            if (version >= 2) {
                vectorData.chunkIndex = dis.readInt();
            }

            int vectorLength = dis.readInt();
            if (vectorLength > 0) {
                vectorData.vector = new float[vectorLength];
                for (int i = 0; i < vectorLength; i++) {
                    vectorData.vector[i] = dis.readFloat();
                }
            }

            return vectorData;
        }
    }

    private void writeString(DataOutputStream dos, String str) throws IOException {
        if (str == null) {
            dos.writeInt(-1);
        } else {
            byte[] bytes = str.getBytes("UTF-8");
            dos.writeInt(bytes.length);
            dos.write(bytes);
        }
    }

    private static String readString(DataInputStream dis) throws IOException {
        int length = dis.readInt();
        if (length == -1) return null;
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        return new String(bytes, "UTF-8");
    }

    @Override
    public String toString() {
        return String.format("BinaryVectorData{id='%s', text='%s', chunkIndex=%d, vector=%s}",
                id, text, getChunkIndex(), vector != null ? "[" + vector.length + " dimensions]" : "null");
    }
}