package apigateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Order(-1)
@Configuration
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @NonNull
    public Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());

        if (ex instanceof NotFoundException) {
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            body.put("status", 404);
            body.put("error", "Not Found");
            body.put("message", "Запрашиваемый ресурс не найден");
        } else if (ex instanceof ConnectException) {
            exchange.getResponse().setStatusCode(HttpStatus.BAD_GATEWAY);
            body.put("status", 502);
            body.put("error", "Bad Gateway");
            body.put("message", "Сервис временно недоступен");
        } else if (ex instanceof TimeoutException) {
            exchange.getResponse().setStatusCode(HttpStatus.GATEWAY_TIMEOUT);
            body.put("status", 504);
            body.put("error", "Gateway Timeout");
            body.put("message", "Сервис не ответил вовремя");
        } else if (ex instanceof ResponseStatusException responseStatusEx) {
            HttpStatus status = HttpStatus.resolve(responseStatusEx.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            exchange.getResponse().setStatusCode(status);
            body.put("status", status.value());
            body.put("error", status.getReasonPhrase());
            body.put("message", responseStatusEx.getReason() != null
                    ? responseStatusEx.getReason()
                    : status.getReasonPhrase());
        } else {
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            body.put("status", 500);
            body.put("error", "Internal Server Error");
            body.put("message", "Внутренняя ошибка сервера");
        }

        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            return exchange.getResponse()
                    .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }
}
