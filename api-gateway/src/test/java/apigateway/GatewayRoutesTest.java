package apigateway;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class GatewayRoutesTest {

    private static final MockWebServer fileStoringMock;
    private static final MockWebServer fileAnalysisMock;

    static {
        try {
            fileStoringMock = new MockWebServer();
            fileStoringMock.start();
            fileAnalysisMock = new MockWebServer();
            fileAnalysisMock.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start MockWebServer", e);
        }
    }

    @AfterAll
    static void tearDown() throws IOException {
        fileStoringMock.shutdown();
        fileAnalysisMock.shutdown();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.routes.file-storing.uri",
                () -> "http://localhost:" + fileStoringMock.getPort());
        registry.add("app.routes.file-analysis.uri",
                () -> "http://localhost:" + fileAnalysisMock.getPort());
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldRouteWorksRequestsToFileStoringService() {
        fileStoringMock.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"id\":1, \"originalFileName\":\"test.pdf\"}")
                .addHeader("Content-Type", "application/json"));

        webTestClient.get()
                .uri("/api/works/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.originalFileName").isEqualTo("test.pdf");
    }

    @Test
    void shouldRouteAnalysisRequestsToFileAnalysisService() {
        fileAnalysisMock.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"status\":\"принято\"}")
                .addHeader("Content-Type", "application/json"));

        webTestClient.get()
                .uri("/api/analysis/works/1/report")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("принято");
    }

    @Test
    void shouldReturn404ForUnknownRoutes() {
        webTestClient.get()
                .uri("/unknown")
                .exchange()
                .expectStatus().isNotFound();
    }
}
