package filestoring.exception;

public class FileStoringException extends RuntimeException {

    public FileStoringException(String message) {
        super(message);
    }

    public FileStoringException(String message, Throwable cause) {
        super(message, cause);
    }
}
