package at.fhtw.services.imp;

public interface IMessageBroker {
    void sendToResultQueue(String documentId, String ocrText);
}