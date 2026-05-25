package fileanalysis.exception;

public class WorkNotFoundException extends FileAnalysisException {

    public WorkNotFoundException(Long workId) {
        super("Работа с ID " + workId + " не найдена");
    }

    public WorkNotFoundException(Long workId, Throwable cause) {
        super("Работа с ID " + workId + " не найдена", cause);
    }
}
