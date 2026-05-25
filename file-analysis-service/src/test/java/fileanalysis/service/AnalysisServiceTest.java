package fileanalysis.service;

import fileanalysis.config.FileProperties;
import fileanalysis.domain.AnalysisReport;
import fileanalysis.dto.ReportResponse;
import fileanalysis.dto.WorkMetadata;
import fileanalysis.exception.FileAnalysisException;
import fileanalysis.exception.WorkNotFoundException;
import fileanalysis.repository.AnalysisReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalysisService Unit Tests")
class AnalysisServiceTest {

    @Mock
    private AnalysisReportRepository reportRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private FileProperties fileProperties;

    @InjectMocks
    private AnalysisService analysisService;

    @Captor
    private ArgumentCaptor<AnalysisReport> reportCaptor;

    private static final Long WORK_ID = 1L;
    private static final String FILE_STORING_URL = "http://localhost:8081";
    private static final String ORIGINAL_FILE_NAME = "document.pdf";
    private static final byte[] FILE_CONTENT = "test file content".getBytes();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(analysisService, "fileStoringUrl", FILE_STORING_URL);
    }

    private WorkMetadata createMetadata() {
        WorkMetadata metadata = new WorkMetadata();
        metadata.setId(WORK_ID);
        metadata.setStudentName("Student");
        metadata.setOriginalFileName(ORIGINAL_FILE_NAME);
        metadata.setS3Key("s3/key");
        metadata.setCreatedAt(LocalDateTime.now());
        return metadata;
    }

    private AnalysisReport createReport() {
        AnalysisReport report = new AnalysisReport();
        report.setId(10L);
        report.setWorkId(WORK_ID);
        report.setStatus("принято");
        report.setOriginalFileName(ORIGINAL_FILE_NAME);
        report.setFileSize((long) FILE_CONTENT.length);
        report.setFileFormat("pdf");
        report.setNotes("");
        report.setCreatedAt(LocalDateTime.now());
        return report;
    }

    @Test
    @DisplayName("getOrCreateReport: когда отчёт уже есть в БД — возвращает кэшированный")
    void getOrCreateReport_whenReportExists_returnsCachedReport() {
        AnalysisReport existingReport = createReport();
        when(reportRepository.findByWorkId(WORK_ID)).thenReturn(Optional.of(existingReport));

        ReportResponse result = analysisService.getOrCreateReport(WORK_ID);

        assertThat(result.getReportId()).isEqualTo(10L);
        assertThat(result.getWorkId()).isEqualTo(WORK_ID);
        assertThat(result.getStatus()).isEqualTo("принято");
        verify(reportRepository).findByWorkId(WORK_ID);
        verify(reportRepository, never()).save(any());
        verifyNoInteractions(restTemplate, fileProperties);
    }

    @Test
    @DisplayName("getOrCreateReport: когда отчёта нет — выполняет анализ, сохраняет, возвращает результат")
    void getOrCreateReport_whenReportNotExists_performsAnalysisAndSaves() {
        when(reportRepository.findByWorkId(WORK_ID)).thenReturn(Optional.empty());

        WorkMetadata metadata = createMetadata();
        String metadataUrl = FILE_STORING_URL + "/api/works/" + WORK_ID;
        when(restTemplate.exchange(eq(metadataUrl), eq(HttpMethod.GET), eq(null), eq(WorkMetadata.class)))
                .thenReturn(ResponseEntity.ok(metadata));

        String fileUrl = FILE_STORING_URL + "/api/works/" + WORK_ID + "/file";
        when(restTemplate.exchange(eq(fileUrl), eq(HttpMethod.GET), eq(null), eq(org.springframework.core.io.Resource.class)))
                .thenReturn(ResponseEntity.ok(new ByteArrayResource(FILE_CONTENT)));

        when(fileProperties.getAllowedFormats()).thenReturn(List.of("pdf", "docx", "txt"));
        when(fileProperties.getMaxSize()).thenReturn(1_048_576L);

        ReportResponse result = analysisService.getOrCreateReport(WORK_ID);

        assertThat(result.getWorkId()).isEqualTo(WORK_ID);
        assertThat(result.getStatus()).isEqualTo("принято");
        assertThat(result.getOriginalFileName()).isEqualTo(ORIGINAL_FILE_NAME);
        assertThat(result.getFileSize()).isEqualTo(FILE_CONTENT.length);
        assertThat(result.getFileFormat()).isEqualTo("pdf");

        verify(reportRepository).save(reportCaptor.capture());
        AnalysisReport saved = reportCaptor.getValue();
        assertThat(saved.getWorkId()).isEqualTo(WORK_ID);
        assertThat(saved.getStatus()).isEqualTo("принято");
    }

    @Test
    @DisplayName("performAnalysis: недопустимый формат — статус 'требуется доработка'")
    void performAnalysis_whenInvalidFormat_statusRequiresRevision() {
        when(reportRepository.findByWorkId(WORK_ID)).thenReturn(Optional.empty());

        WorkMetadata metadata = createMetadata();
        String metadataUrl = FILE_STORING_URL + "/api/works/" + WORK_ID;
        when(restTemplate.exchange(eq(metadataUrl), eq(HttpMethod.GET), eq(null), eq(WorkMetadata.class)))
                .thenReturn(ResponseEntity.ok(metadata));

        String fileUrl = FILE_STORING_URL + "/api/works/" + WORK_ID + "/file";
        when(restTemplate.exchange(eq(fileUrl), eq(HttpMethod.GET), eq(null), eq(org.springframework.core.io.Resource.class)))
                .thenReturn(ResponseEntity.ok(new ByteArrayResource(FILE_CONTENT)));

        when(fileProperties.getAllowedFormats()).thenReturn(List.of("docx", "txt"));
        when(fileProperties.getMaxSize()).thenReturn(1_048_576L);

        ReportResponse result = analysisService.getOrCreateReport(WORK_ID);

        assertThat(result.getStatus()).isEqualTo("требуется доработка");
        assertThat(result.getNotes()).contains("Недопустимый формат файла");
    }

    @Test
    @DisplayName("performAnalysis: превышен размер — статус 'требуется доработка'")
    void performAnalysis_whenSizeTooLarge_statusRequiresRevision() {
        when(reportRepository.findByWorkId(WORK_ID)).thenReturn(Optional.empty());

        WorkMetadata metadata = createMetadata();
        String metadataUrl = FILE_STORING_URL + "/api/works/" + WORK_ID;
        when(restTemplate.exchange(eq(metadataUrl), eq(HttpMethod.GET), eq(null), eq(WorkMetadata.class)))
                .thenReturn(ResponseEntity.ok(metadata));

        String fileUrl = FILE_STORING_URL + "/api/works/" + WORK_ID + "/file";
        when(restTemplate.exchange(eq(fileUrl), eq(HttpMethod.GET), eq(null), eq(org.springframework.core.io.Resource.class)))
                .thenReturn(ResponseEntity.ok(new ByteArrayResource(FILE_CONTENT)));

        when(fileProperties.getAllowedFormats()).thenReturn(List.of("pdf", "docx", "txt"));
        when(fileProperties.getMaxSize()).thenReturn(1L);

        ReportResponse result = analysisService.getOrCreateReport(WORK_ID);

        assertThat(result.getStatus()).isEqualTo("требуется доработка");
        assertThat(result.getNotes()).contains("Превышен размер файла");
    }

    @Test
    @DisplayName("performAnalysis: невалидный формат И превышен размер — обе ошибки в notes")
    void performAnalysis_whenBothInvalid_bothIssuesInNotes() {
        when(reportRepository.findByWorkId(WORK_ID)).thenReturn(Optional.empty());

        WorkMetadata metadata = createMetadata();
        String metadataUrl = FILE_STORING_URL + "/api/works/" + WORK_ID;
        when(restTemplate.exchange(eq(metadataUrl), eq(HttpMethod.GET), eq(null), eq(WorkMetadata.class)))
                .thenReturn(ResponseEntity.ok(metadata));

        String fileUrl = FILE_STORING_URL + "/api/works/" + WORK_ID + "/file";
        when(restTemplate.exchange(eq(fileUrl), eq(HttpMethod.GET), eq(null), eq(org.springframework.core.io.Resource.class)))
                .thenReturn(ResponseEntity.ok(new ByteArrayResource(FILE_CONTENT)));

        when(fileProperties.getAllowedFormats()).thenReturn(List.of("docx"));
        when(fileProperties.getMaxSize()).thenReturn(1L);

        ReportResponse result = analysisService.getOrCreateReport(WORK_ID);

        assertThat(result.getStatus()).isEqualTo("требуется доработка");
        assertThat(result.getNotes())
                .contains("Недопустимый формат файла")
                .contains("Превышен размер файла");
    }

    @Test
    @DisplayName("getWorkMetadata: file-storing вернул null body — WorkNotFoundException")
    void getWorkMetadata_whenBodyNull_throwsWorkNotFoundException() {
        when(reportRepository.findByWorkId(WORK_ID)).thenReturn(Optional.empty());

        String metadataUrl = FILE_STORING_URL + "/api/works/" + WORK_ID;
        when(restTemplate.exchange(eq(metadataUrl), eq(HttpMethod.GET), eq(null), eq(WorkMetadata.class)))
                .thenReturn(ResponseEntity.ok(null));

        assertThatThrownBy(() -> analysisService.getOrCreateReport(WORK_ID))
                .isInstanceOf(WorkNotFoundException.class)
                .hasMessageContaining("Работа с ID " + WORK_ID);
    }

    @Test
    @DisplayName("getWorkMetadata: file-storing вернул 404 — WorkNotFoundException")
    void getWorkMetadata_when404_throwsWorkNotFoundException() {
        when(reportRepository.findByWorkId(WORK_ID)).thenReturn(Optional.empty());

        String metadataUrl = FILE_STORING_URL + "/api/works/" + WORK_ID;
        when(restTemplate.exchange(eq(metadataUrl), eq(HttpMethod.GET), eq(null), eq(WorkMetadata.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> analysisService.getOrCreateReport(WORK_ID))
                .isInstanceOf(WorkNotFoundException.class)
                .hasMessageContaining("Работа с ID " + WORK_ID);
    }

    @Test
    @DisplayName("downloadFile: file-storing вернул 404 — WorkNotFoundException")
    void downloadFile_when404_throwsWorkNotFoundException() {
        when(reportRepository.findByWorkId(WORK_ID)).thenReturn(Optional.empty());

        WorkMetadata metadata = createMetadata();
        String metadataUrl = FILE_STORING_URL + "/api/works/" + WORK_ID;
        when(restTemplate.exchange(eq(metadataUrl), eq(HttpMethod.GET), eq(null), eq(WorkMetadata.class)))
                .thenReturn(ResponseEntity.ok(metadata));

        String fileUrl = FILE_STORING_URL + "/api/works/" + WORK_ID + "/file";
        when(restTemplate.exchange(eq(fileUrl), eq(HttpMethod.GET), eq(null), eq(org.springframework.core.io.Resource.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> analysisService.getOrCreateReport(WORK_ID))
                .isInstanceOf(WorkNotFoundException.class)
                .hasMessageContaining("Работа с ID " + WORK_ID);
    }

    @Test
    @DisplayName("downloadFile: IOException при скачивании — FileAnalysisException")
    void downloadFile_whenIOException_throwsFileAnalysisException() throws Exception {
        when(reportRepository.findByWorkId(WORK_ID)).thenReturn(Optional.empty());

        WorkMetadata metadata = createMetadata();
        String metadataUrl = FILE_STORING_URL + "/api/works/" + WORK_ID;
        when(restTemplate.exchange(eq(metadataUrl), eq(HttpMethod.GET), eq(null), eq(WorkMetadata.class)))
                .thenReturn(ResponseEntity.ok(metadata));

        InputStream brokenStream = mock(InputStream.class);
        when(brokenStream.readAllBytes()).thenThrow(new IOException("Connection reset"));

        org.springframework.core.io.Resource brokenResource = mock(org.springframework.core.io.Resource.class);
        when(brokenResource.getInputStream()).thenReturn(brokenStream);

        String fileUrl = FILE_STORING_URL + "/api/works/" + WORK_ID + "/file";
        when(restTemplate.exchange(eq(fileUrl), eq(HttpMethod.GET), eq(null), eq(org.springframework.core.io.Resource.class)))
                .thenReturn(ResponseEntity.ok(brokenResource));

        assertThatThrownBy(() -> analysisService.getOrCreateReport(WORK_ID))
                .isInstanceOf(FileAnalysisException.class)
                .hasMessageContaining("Ошибка при скачивании файла");
    }

    @ParameterizedTest
    @CsvSource(value = {
            "document.pdf, pdf",
            "archive.tar.gz, gz",
            "noextension, ''",
            "file, ''",
            "., ''"
    })
    @DisplayName("getExtension: извлечение расширения из разных имён файлов")
    void getExtension_variousFilenames(String fileName, String expectedExtension) {
        when(reportRepository.findByWorkId(WORK_ID)).thenReturn(Optional.empty());

        WorkMetadata metadata = createMetadata();
        metadata.setOriginalFileName(fileName);
        String metadataUrl = FILE_STORING_URL + "/api/works/" + WORK_ID;
        when(restTemplate.exchange(eq(metadataUrl), eq(HttpMethod.GET), eq(null), eq(WorkMetadata.class)))
                .thenReturn(ResponseEntity.ok(metadata));

        String fileUrl = FILE_STORING_URL + "/api/works/" + WORK_ID + "/file";
        when(restTemplate.exchange(eq(fileUrl), eq(HttpMethod.GET), eq(null), eq(org.springframework.core.io.Resource.class)))
                .thenReturn(ResponseEntity.ok(new ByteArrayResource(FILE_CONTENT)));

        when(fileProperties.getAllowedFormats()).thenReturn(List.of("pdf", "gz"));
        when(fileProperties.getMaxSize()).thenReturn(1_048_576L);

        ReportResponse result = analysisService.getOrCreateReport(WORK_ID);

        assertThat(result.getFileFormat()).isEqualTo(expectedExtension);
    }
}
