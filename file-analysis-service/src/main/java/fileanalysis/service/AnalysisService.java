package fileanalysis.service;

import fileanalysis.config.FileProperties;
import fileanalysis.domain.AnalysisReport;
import fileanalysis.dto.ReportResponse;
import fileanalysis.dto.WorkMetadata;
import fileanalysis.exception.FileAnalysisException;
import fileanalysis.exception.WorkNotFoundException;
import fileanalysis.repository.AnalysisReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisReportRepository reportRepository;
    private final RestTemplate restTemplate;
    private final FileProperties fileProperties;

    @Value("${app.file-storing-service.url}")
    private String fileStoringUrl;

    public ReportResponse getOrCreateReport(Long workId) {
        return reportRepository.findByWorkId(workId)
                .map(this::toResponse)
                .orElseGet(() -> {
                    log.info("Отчёт для workId={} не найден, запускаем анализ", workId);
                    AnalysisReport report = performAnalysis(workId);
                    reportRepository.save(report);
                    return toResponse(report);
                });
    }

    private AnalysisReport performAnalysis(Long workId) {
        WorkMetadata metadata = getWorkMetadata(workId);
        byte[] fileBytes = downloadFile(workId);

        String originalFileName = metadata.getOriginalFileName();
        String fileExtension = getExtension(originalFileName).toLowerCase();
        long fileSize = fileBytes.length;

        List<String> issues = new ArrayList<>();
        boolean accepted = true;

        if (!fileProperties.getAllowedFormats().contains(fileExtension)) {
            issues.add("Недопустимый формат файла: ." + fileExtension);
            accepted = false;
        }

        if (fileSize > fileProperties.getMaxSize()) {
            issues.add(String.format("Превышен размер файла: %d байт (максимум: %d байт)",
                    fileSize, fileProperties.getMaxSize()));
            accepted = false;
        }

        AnalysisReport report = new AnalysisReport();
        report.setWorkId(workId);
        report.setStatus(accepted ? "принято" : "требуется доработка");
        report.setOriginalFileName(originalFileName);
        report.setFileSize(fileSize);
        report.setFileFormat(fileExtension);
        report.setNotes(String.join("; ", issues));
        report.setCreatedAt(LocalDateTime.now());

        return report;
    }

    private WorkMetadata getWorkMetadata(Long workId) {
        String url = fileStoringUrl + "/api/works/" + workId;
        try {
            ResponseEntity<WorkMetadata> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, WorkMetadata.class);
            return Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> new WorkNotFoundException(workId));
        } catch (HttpClientErrorException e) {
            if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                throw new WorkNotFoundException(workId, e);
            }
            throw e;
        }
    }

    private byte[] downloadFile(Long workId) {
        String url = fileStoringUrl + "/api/works/" + workId + "/file";
        try {
            ResponseEntity<Resource> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, Resource.class);
            return Objects.requireNonNull(response.getBody()).getInputStream().readAllBytes();
        } catch (HttpClientErrorException e) {
            if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                throw new WorkNotFoundException(workId, e);
            }
            throw e;
        } catch (IOException e) {
            throw new FileAnalysisException("Ошибка при скачивании файла", e);
        }
    }

    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot == -1 ? "" : fileName.substring(lastDot + 1);
    }

    private ReportResponse toResponse(AnalysisReport report) {
        return new ReportResponse(
                report.getId(),
                report.getWorkId(),
                report.getStatus(),
                report.getOriginalFileName(),
                report.getFileSize(),
                report.getFileFormat(),
                report.getNotes(),
                report.getCreatedAt()
        );
    }
}