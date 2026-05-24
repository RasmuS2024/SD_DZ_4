package fileanalysis.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkMetadata {
    private Long id;
    private String studentName;
    private String originalFileName;
    private String s3Key;
    private LocalDateTime createdAt;
}
