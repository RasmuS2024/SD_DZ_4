package filestoring.exception;

public class fileStoringException extends RuntimeException {

    public fileStoringException(String message) {
        super(message);
    }

    public fileStoringException(String message, Throwable cause) {
        super(message, cause);
    }
}
