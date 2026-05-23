package fileanalysis.exception;

public class fileAnalysisException extends RuntimeException {

    public fileAnalysisException(String message) {
        super(message);
    }

    public fileAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
