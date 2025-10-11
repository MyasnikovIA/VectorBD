package ru.miacomsoft.vectordb.core;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class BinaryTreeNode {
    private String content;
    private Map<String, Object> metadata;
    private BinaryTreeNode left;
    private BinaryTreeNode right;

    public BinaryTreeNode() {
        this.metadata = new HashMap<>();
    }

    public BinaryTreeNode(String content) {
        this();
        this.content = content;
    }

    // Геттеры и сеттеры
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    public BinaryTreeNode getLeft() {
        return left;
    }

    public void setLeft(BinaryTreeNode left) {
        this.left = left;
    }

    public BinaryTreeNode getRight() {
        return right;
    }

    public void setRight(BinaryTreeNode right) {
        this.right = right;
    }

    // Сериализация в бинарный формат
    public byte[] serialize() throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {

            oos.writeObject(content);
            oos.writeObject(metadata);
            oos.writeBoolean(left != null);
            if (left != null) {
                byte[] leftData = left.serialize();
                oos.writeInt(leftData.length);
                oos.write(leftData);
            }
            oos.writeBoolean(right != null);
            if (right != null) {
                byte[] rightData = right.serialize();
                oos.writeInt(rightData.length);
                oos.write(rightData);
            }

            return baos.toByteArray();
        }
    }

    // Десериализация из бинарного формата
    public static BinaryTreeNode deserialize(byte[] data) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {

            BinaryTreeNode node = new BinaryTreeNode();
            node.content = (String) ois.readObject();
            node.metadata = (Map<String, Object>) ois.readObject();

            if (ois.readBoolean()) {
                int leftLength = ois.readInt();
                byte[] leftData = new byte[leftLength];
                ois.readFully(leftData);
                node.left = deserialize(leftData);
            }

            if (ois.readBoolean()) {
                int rightLength = ois.readInt();
                byte[] rightData = new byte[rightLength];
                ois.readFully(rightData);
                node.right = deserialize(rightData);
            }

            return node;
        } catch (ClassNotFoundException e) {
            throw new IOException("Error deserializing BinaryTreeNode", e);
        }
    }

    @Override
    public String toString() {
        return "BinaryTreeNode{content='" + content + "', metadata=" + metadata + "}";
    }
}