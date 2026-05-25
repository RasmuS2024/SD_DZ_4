package fileanalysis.controller;

import fileanalysis.dto.ReportResponse;
import fileanalysis.service.AnalysisService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @GetMapping("/works/{workId}/report")
    public ResponseEntity<ReportResponse> getReport(@PathVariable @Positive Long workId) {
        ReportResponse report = analysisService.getOrCreateReport(workId);
        return ResponseEntity.ok(report);
    }
}
