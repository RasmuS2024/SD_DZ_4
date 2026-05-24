package apigateway;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayRoutesTest {

    private static final MockWebServer fileStoringMock = new MockWebServer();
    private static final MockWebServer fileAnalysisMock = new MockWebServer();

    @BeforeAll
    static void setUp() throws IOException {
        fileStoringMock.start(18081);
        fileAnalysisMock.start(18082);
    }

    @AfterAll
    static void tearDown() throws IOException {
        fileStoringMock.shutdown();
        fileAnalysisMock.shutdown();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.routes.file-storing.uri", () -> "http://localhost:18081");
        registry.add("app.routes.file-analysis.uri", () -> "http://localhost:18082");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldRouteWorksRequestsToFileStoringService() {
        // given
        fileStoringMock.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"id\":1, \"originalFileName\":\"test.pdf\"}")
                .addHeader("Content-Type", "application/json"));

        // when
        ResponseEntity<String> response = restTemplate.getForEntity("/api/works/1", String.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("test.pdf");
    }

    @Test
    void shouldRouteAnalysisRequestsToFileAnalysisService() {
        // given
        fileAnalysisMock.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"status\":\"принято\"}")
                .addHeader("Content-Type", "application/json"));

        // when
        ResponseEntity<String> response = restTemplate.getForEntity("/api/analysis/works/1/report", String.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("принято");
    }

    @Test
    void shouldReturn404ForUnknownRoutes() {
        ResponseEntity<String> response = restTemplate.getForEntity("/unknown", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}