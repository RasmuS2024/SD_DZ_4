package fileanalysis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReportResponse {
    private Long reportId;
    private Long workId;
    private String status;
    private String originalFileName;
    private Long fileSize;
    private String fileFormat;
    private String notes;
    private LocalDateTime createdAt;
}
