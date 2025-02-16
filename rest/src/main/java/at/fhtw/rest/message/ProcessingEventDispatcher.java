package at.fhtw.rest.message;

public interface ProcessingEventDispatcher {
    void sendProcessingRequest(
            String docId,
            String filename
    );
}