package at.fhtw.rest.message.imp;

public interface IProcessingEventDispatcher {
    void sendProcessingRequest(
            String docId,
            String filename
    );
}