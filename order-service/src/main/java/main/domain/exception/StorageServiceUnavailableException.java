package main.domain.exception;

public class StorageServiceUnavailableException extends RuntimeException {
    public StorageServiceUnavailableException(String message) {
        super(message);
    }
}
