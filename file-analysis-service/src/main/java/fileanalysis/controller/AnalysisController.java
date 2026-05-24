package fileanalysis.controller;

import fileanalysis.dto.ReportResponse;
import fileanalysis.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @GetMapping("/works/{workId}/report")
    public ResponseEntity<ReportResponse> getReport(@PathVariable Long workId) {
        ReportResponse report = analysisService.getOrCreateReport(workId);
        return ResponseEntity.ok(report);
    }
}
