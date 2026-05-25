package fileanalysis.controller;

import fileanalysis.dto.ReportResponse;
import fileanalysis.exception.FileAnalysisException;
import fileanalysis.exception.WorkNotFoundException;
import fileanalysis.service.AnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalysisController.class)
@DisplayName("AnalysisController WebMvcTest")
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalysisService analysisService;

    @Test
    @DisplayName("GET /api/analysis/works/{id}/report — 200 с телом отчёта")
    void getReport_returns200_withReportBody() throws Exception {
        ReportResponse response = new ReportResponse(
                10L, 1L, "принято", "doc.pdf", 1024L, "pdf", "", LocalDateTime.now());

        when(analysisService.getOrCreateReport(1L)).thenReturn(response);

        mockMvc.perform(get("/api/analysis/works/1/report"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.reportId").value(10))
                .andExpect(jsonPath("$.workId").value(1))
                .andExpect(jsonPath("$.status").value("принято"));
    }

    @Test
    @DisplayName("GET /api/analysis/works/{id}/report — не число → 400")
    void getReport_whenWorkIdNotNumeric_returns400() throws Exception {
        mockMvc.perform(get("/api/analysis/works/abc/report"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/analysis/works/{id}/report — работа не найдена → 404")
    void getReport_whenWorkNotFound_returns404() throws Exception {
        when(analysisService.getOrCreateReport(99L))
                .thenThrow(new WorkNotFoundException(99L));

        mockMvc.perform(get("/api/analysis/works/99/report"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Работа с ID 99 не найдена"));
    }

    @Test
    @DisplayName("GET /api/analysis/works/{id}/report — внутренняя ошибка сервиса → 500")
    void getReport_whenFileAnalysisException_returns500() throws Exception {
        when(analysisService.getOrCreateReport(1L))
                .thenThrow(new FileAnalysisException("Ошибка скачивания файла"));

        mockMvc.perform(get("/api/analysis/works/1/report"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Ошибка скачивания файла"));
    }

    @Test
    @DisplayName("GET /api/analysis/works/{id}/report — неожиданная ошибка → 500 с общим сообщением")
    void getReport_whenUnexpectedException_returns500() throws Exception {
        when(analysisService.getOrCreateReport(1L))
                .thenThrow(new RuntimeException("Something went wrong"));

        mockMvc.perform(get("/api/analysis/works/1/report"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Внутренняя ошибка сервера"));
    }
}
