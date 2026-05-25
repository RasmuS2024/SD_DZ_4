package fileanalysis.exception;

import fileanalysis.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WorkNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWorkNotFound(WorkNotFoundException e, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                404, "Not Found", e.getMessage(), LocalDateTime.now(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(FileAnalysisException.class)
    public ResponseEntity<ErrorResponse> handleFileAnalysis(FileAnalysisException e, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                500, "Internal Server Error", e.getMessage(), LocalDateTime.now(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                400, "Bad Request",
                "Некорректный ID: " + e.getValue(),
                LocalDateTime.now(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                400, "Bad Request",
                e.getMessage(),
                LocalDateTime.now(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                500, "Internal Server Error",
                "Внутренняя ошибка сервера",
                LocalDateTime.now(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
