package apigateway.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.mock;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalErrorHandler Unit Tests")
class GlobalErrorHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GlobalErrorHandler handler;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private DataBufferFactory bufferFactory;

    @Captor
    private ArgumentCaptor<byte[]> bytesCaptor;

    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        handler = new GlobalErrorHandler(MAPPER);
        headers = new HttpHeaders();
        when(exchange.getResponse()).thenReturn(response);
        when(response.getHeaders()).thenReturn(headers);
        when(response.bufferFactory()).thenReturn(bufferFactory);
        when(response.writeWith(any(Publisher.class))).thenReturn(Mono.empty());
        when(bufferFactory.wrap(bytesCaptor.capture())).thenReturn(mock(DataBuffer.class));
    }

    @Test
    @DisplayName("NotFoundException → 404 Not Found")
    void handle_shouldReturn404_whenNotFoundException() throws Exception {
        handler.handle(exchange, new NotFoundException("test")).block();

        assertAll(
                () -> verify(response).setStatusCode(HttpStatus.NOT_FOUND),
                () -> assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON)
        );

        JsonNode root = MAPPER.readTree(bytesCaptor.getValue());
        assertAll(
                () -> assertThat(root.get("status").asInt()).isEqualTo(404),
                () -> assertThat(root.get("error").asText()).isEqualTo("Not Found"),
                () -> assertThat(root.get("message").asText())
                        .isEqualTo("Запрашиваемый ресурс не найден")
        );
    }

    @Test
    @DisplayName("ConnectException → 502 Bad Gateway")
    void handle_shouldReturn502_whenConnectException() throws Exception {
        handler.handle(exchange, new ConnectException("refused")).block();

        assertAll(
                () -> verify(response).setStatusCode(HttpStatus.BAD_GATEWAY),
                () -> assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON)
        );

        JsonNode root = MAPPER.readTree(bytesCaptor.getValue());
        assertAll(
                () -> assertThat(root.get("status").asInt()).isEqualTo(502),
                () -> assertThat(root.get("error").asText()).isEqualTo("Bad Gateway"),
                () -> assertThat(root.get("message").asText())
                        .isEqualTo("Сервис временно недоступен")
        );
    }

    @Test
    @DisplayName("TimeoutException → 504 Gateway Timeout")
    void handle_shouldReturn504_whenTimeoutException() throws Exception {
        handler.handle(exchange, new TimeoutException("timeout")).block();

        assertAll(
                () -> verify(response).setStatusCode(HttpStatus.GATEWAY_TIMEOUT),
                () -> assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON)
        );

        JsonNode root = MAPPER.readTree(bytesCaptor.getValue());
        assertAll(
                () -> assertThat(root.get("status").asInt()).isEqualTo(504),
                () -> assertThat(root.get("error").asText()).isEqualTo("Gateway Timeout"),
                () -> assertThat(root.get("message").asText())
                        .isEqualTo("Сервис не ответил вовремя")
        );
    }

    @Test
    @DisplayName("RuntimeException → 500 Internal Server Error")
    void handle_shouldReturn500_whenGenericException() throws Exception {
        handler.handle(exchange, new RuntimeException("unexpected")).block();

        assertAll(
                () -> verify(response).setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR),
                () -> assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON)
        );

        JsonNode root = MAPPER.readTree(bytesCaptor.getValue());
        assertAll(
                () -> assertThat(root.get("status").asInt()).isEqualTo(500),
                () -> assertThat(root.get("error").asText()).isEqualTo("Internal Server Error"),
                () -> assertThat(root.get("message").asText())
                        .isEqualTo("Внутренняя ошибка сервера")
        );
    }
}
