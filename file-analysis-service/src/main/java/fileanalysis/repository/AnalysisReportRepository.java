package fileanalysis.repository;

import fileanalysis.domain.AnalysisReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, Long> {
    Optional<AnalysisReport> findByWorkId(Long workId);
}
