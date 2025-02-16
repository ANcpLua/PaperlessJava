package at.fhtw.services;

public interface MessageBroker {
    void sendToResultQueue(String documentId, String ocrText);
}