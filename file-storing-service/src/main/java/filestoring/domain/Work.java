package filestoring.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "works")
@Getter
@Setter
@NoArgsConstructor
public class Work {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_name", nullable = false)
    private String studentName;

    @Column(name = "file_name", nullable = false)
    private String originalFileName;

    @Column(name = "s3_key", unique = true, nullable = false)
    private String s3Key;

    @CreationTimestamp
    @Column(name = "uploaded_dt", updatable = false)
    private LocalDateTime uploadedDateTime;

}
